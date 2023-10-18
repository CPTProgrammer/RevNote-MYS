package com.revnote.hoyolab

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.widget.HorizontalScrollView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
            arrayOf("Tab", "{", "}", "(", ")", ",", ".", ";", "\"", "=", "+", "-", "*", "/", "?"),
            arrayOf("\t", "{}", "}", "(", ")", ",", ".", ";", "\"", "=", "+", "-", "*", "/", "?")
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



@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CodeEditorPage(context: Context, navController: NavController){
    var openCloseDialog by remember { mutableStateOf(false) }
    var openSaveDialog by remember { mutableStateOf(false) }

    val store = TrackingData(LocalContext.current)
    // val captcha_code = store.getData("captcha_code").collectAsState("")
    val captcha_code = if (store.isDataExistSync("captcha_code")) {
        store.getDataSync("captcha_code")
    } else {
        """
            /*
            * 获取 API 的信息
            * 
            * 参数 captchaData 内容为类似下面 JSON 的 stringify
            * 当然，如果官方的验证码 API 有变动，那么可能不是这样
            * { 
            *     "challenge": "0574339c10888fd42aaa2d3aec0e10fc",
            *     "gt": "846fea0fd833b173bec7a25920851739", 
            *     "new_captcha": 1, 
            *     "success": 1
            * }
            * new_captcha 的值一般不变（我也不知道啥作用）
            * 
            * 返回值为类似以下 JSON 的 stringify
            * {
            *     method: "POST",  <--可填"POST"或"GET"
            *     url: "http://api.example.com/api/recognize",  <--处理后的请求链接，如果有参数也写在后面，例如".../recognize?key=value"
            *     body: "{\"key\":\"value\"}"  <--如果method为POST则需要填写这个，否则写空字符串""
            * }
            * */
            function getApi(captchaData) {
                // 解析 captchaData
                const { challenge, gt, newCaptcha, success } = JSON.parse(captchaData)
                var request = RequestBuilder.url("http://www.baidu.com/").build()
                // return UserScriptInterface.test()
                const promise = new Promise((resolve, reject) => {
                    const client = OkHttpClientBuilder.connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS).build()
                    client.newCall(request).enqueue({
                        onFailure: (call, e) => {
                            reject(String(e.message))
                        },
                        onResponse: (call, response) => {
                            resolve(response.body ? String(response.body().string()) : "")
                        }
                    })
                })
                
                promise.then((result) => {
                    callback(result)
                }, (error) => {
                    callback(error)
                })
            }
            
            /*
            解析 API 的返回值
            
            参数 responseBody 类型为String，内容为API的返回值
            
            
            * */
            function resolveApiResponse(responseBody) {
                
            }
        """.trimIndent()
    }

    val codeText = remember { mutableStateOf(captcha_code) }
//    val updateState = remember { mutableStateOf(false) }

    val codeEditor = CustomCodeEditor(LocalContext.current, "javascript")
    //Log.d("Debug", "OUT $codeEditor")
    // Log.d("Debug", "CODE $captcha_code")

//    LaunchedEffect(key1 = captcha_code, block = {
//        codeEditor.setText(captcha_code)
//    })

    // 禁用直接回退
    BackHandler(true) {
        openCloseDialog = true
    }

    Scaffold (
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF000000),
                    titleContentColor = Color(0xFFFFFFFF),
                    navigationIconContentColor = Color(0xFFFFFFFF),
                    actionIconContentColor = Color(0xFFFFFFFF)
                ),
                title = { Text("自定义打码平台接口") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            openCloseDialog = true
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Close, null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // updateState.value = !updateState.value
                            openSaveDialog = true
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Done, null)
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier.padding(padding).imePadding()
        ) {
            CodeEditor(LocalContext.current, codeEditor, codeText/*, updateState*/)
        }
    }

    when {
        openCloseDialog -> {
            AlertDialog(
                icon = { Icon(imageVector = Icons.Filled.Info, null) },
                text = { Text("当前编辑未保存，是否确认关闭？") },
                onDismissRequest = {},
                confirmButton = {
                    TextButton(
                        onClick = {
                            openCloseDialog = false
                            navController.navigate("home")
                        }
                    ) {
                        Text("确认")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            openCloseDialog = false
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
        openSaveDialog -> {
            AlertDialog(
                icon = { Icon(imageVector = Icons.Filled.Info, null) },
                text = { Text("是否确认保存脚本并关闭") },
                onDismissRequest = {},
                confirmButton = {
                    TextButton(
                        onClick = {
                            openSaveDialog = false
                            CoroutineScope(Dispatchers.IO).launch {
                                // Log.d("Debug", "D1 " + codeText.value)
                                store.saveData("captcha_code", codeText.value)
                                // Log.d("Debug", "D2 " + codeEditor.text.toString())
                            }
                            navController.navigate("home")
                        }
                    ) {
                        Text("确认")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            openSaveDialog = false
                        }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}