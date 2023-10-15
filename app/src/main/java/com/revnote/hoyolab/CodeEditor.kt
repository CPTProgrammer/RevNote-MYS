package com.revnote.hoyolab

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.widget.HorizontalScrollView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.view.WindowCompat
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentListener
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolInputView
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.HIGHLIGHTED_DELIMITERS_BACKGROUND
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND
import io.github.rosemoe.sora.widget.schemes.SchemeVS2019
import io.github.rosemoe.sora.widget.subscribeEvent
import org.eclipse.tm4e.core.registry.IGrammarSource
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

fun CustomCodeEditor(context: Context, languageName: String): CodeEditor {
    return CodeEditor(context).apply {
        val grammarSource = IGrammarSource.fromInputStream(context.assets.open("languages/$languageName/tmLanguage.json"), "tmLanguage.json", StandardCharsets.UTF_8)
        val languageConfiguration = InputStreamReader(context.assets.open("languages/$languageName/language-configuration.json"))
        val themeSource = IThemeSource.fromInputStream(context.assets.open("themes/monokai_dark.tmTheme"), "monokai_dark.tmTheme", StandardCharsets.UTF_8)

        val customColorScheme = TextMateColorScheme(
            ThemeRegistry.getInstance(),
            ThemeModel(themeSource)
        )

        colorScheme = customColorScheme
        isHighlightBracketPair = false

        typefaceText = Typeface.MONOSPACE

        setEditorLanguage(TextMateLanguage.create(grammarSource, languageConfiguration, themeSource))
    }
}

@SuppressLint("SetTextI18n")
@Composable
fun CodeEditor(context: Context, codeEditor: CodeEditor, codeText: MutableState<String>/*, state: MutableState<Boolean>*/) {

    val symbolInputView = SymbolInputView(context).apply {
        bindEditor(codeEditor)
        addSymbols(
            arrayOf("Tab", "{", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/"),
            arrayOf("\t", "{}", "}", "(", ")", ",", ".", ";", "\"", "?", "+", "-", "*", "/")
        )
    }

    Box (
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 40.dp).fillMaxSize(),
            factory = {
                codeEditor.apply {
                    // Log.d("Debug", "111" + codeText.value)
                    setText(codeText.value)
                }
            },
            update = {
                //Log.d("Debug", "IN $codeEditor")
                codeEditor
//                Log.d("Debug", "111Update")
//                Log.d("Debug", it.text.toString())
                codeText.value = it.text.toString()
            }
        )
        Column(
            modifier = Modifier.fillMaxWidth().height(40.dp).align(Alignment.BottomCenter).horizontalScroll(rememberScrollState())
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { symbolInputView }
            )
        }
    }
}