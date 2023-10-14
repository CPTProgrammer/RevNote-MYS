package com.revnote.hoyolab

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.Color as GraphicsColor
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.util.Patterns
import android.util.TypedValue
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.doOnTextChanged
import com.amrdeveloper.codeview.Code
import com.amrdeveloper.codeview.CodeView
import com.amrdeveloper.codeview.CodeViewAdapter
import java.util.regex.Pattern

val KotlinLanguageHighlight = mapOf<Long, List<String>>(
    0xFFdcdcaa to listOf(
        "(?<=\\b(fun)\\b)\\s*[a-zA-Z_]\\w*"
    ),
    0xFF569CD6 to listOf<String>(
        "\\b(import|package)\\b",
        "(\\!in|\\!is|as\\?)",
        "\\b(in|is|as|assert)\\b",
        "\\b(const)\\b",
        "\\b(val|var)\\b",
        "\\b(data|inline|tailrec|operator|infix|typealias|reified)\\b",
        "\\b(external|public|private|protected|internal|abstract|final|sealed|enum|open|annotation|override|vararg|typealias|expect|actual|suspend|yield|out|in)\\b",
        "\\b(try|catch|finally|throw)\\b",
        "\\b(if|else|when)\\b",
        "\\b(while|for|do|return|break|continue)\\b",
        "\\b(constructor|init)\\b",
        "\\b(companion|object)\\b",
        "\\b(true|false|null|class)\\b",
        "\\b(this|super)\\b",
        "\\b(fun)\\b",
        "(@[a-zA-Z]\\w*)\\b"
    ),
    0xFFffd700 to listOf(
        "\\(|\\[|\\{|\\)|\\]|\\}"
    ),
    0xFFCE9178 to listOf(
        "\".*\"",
        "\"\"\"(?:.|\\n)*\"\"\"",
        "'.?'",
    ),
    0xFFB5CEA8 to listOf(
        "\\b(0(x|X)[0-9A-Fa-f_]*)[L]?\\b",
        "\\b(0(b|B)[0-1_]*)[L]?\\b",
        "\\b([0-9][0-9_]*\\.[0-9][0-9_]*[fFL]?)\\b",
        "\\b([0-9][0-9_]*[fFL]?)\\b"
    ),
    0xFF6A9955 to listOf(
        "/\\*(?:.|\\n)*?\\*/",
        "//.*"
    ),
)

@SuppressLint("SetTextI18n")
@Composable
fun CodeEditor(codeText: MutableState<String>) {
    var codeView: CodeView? = null
    LaunchedEffect(key1 = codeText.value, block = {
        codeView?.setTextHighlighted(codeText.value)
    })
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            // Toast.makeText(context, "Test", Toast.LENGTH_SHORT).show()

            CodeView(context).apply {
                background = ColorDrawable(Color(0xFF1E1E1E).toArgb())
                typeface = Typeface.MONOSPACE
                textSize = 16f
                setTextColor(Color(0xFFD4D4D4).toArgb())

                // setText("""""".trimIndent())
                doOnTextChanged { text, start, before, count ->
                    codeText.value = text.toString()
                }

                reHighlightSyntax()

                setEnableLineNumber(true)
                setEnableHighlightCurrentLine(true)
                setLineNumberTextColor(GraphicsColor.GRAY)
                setHighlightCurrentLineColor(Color(0xFF383B3D).toArgb())
                setLineNumberTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16f, context.resources.displayMetrics))
                setTextIsSelectable(true)

                setEnableAutoIndentation(true)
                setTabLength(2)

                val highlight = mutableMapOf<Pattern, Int>()
                for ((key, value) in KotlinLanguageHighlight){
                    value.forEach {
                        highlight[Pattern.compile(it)] = Color(key).toArgb()
                    }
                }
                setSyntaxPatternsMap(highlight.toMap())

//                val codeAdapter = CodeViewAdapter(context, 0, 0, codes)
//                setAdapter(codeAdapter)
                codeView = this
            }
        }
    )
}