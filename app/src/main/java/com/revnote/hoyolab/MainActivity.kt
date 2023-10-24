package com.revnote.hoyolab

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.revnote.hoyolab.ui.theme.HoYoLABTheme
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.*

import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import org.mozilla.javascript.Function
import org.mozilla.javascript.FunctionObject
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.lang.reflect.Member
import javax.script.Invocable
import javax.script.ScriptEngineManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {

    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        WebView.setWebContentsDebuggingEnabled(true)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val navController = rememberNavController()

            HoYoLABTheme {
                // A surface container using the 'background' color from the theme
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        window.statusBarColor = MaterialTheme.colorScheme.primary.toArgb()
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            Home(navController, {
                                createNotification("test", "原神战绩工具")
                            })
                        }
                    }
                    composable("code_editor") {
                        window.statusBarColor = Color(0xFF000000).toArgb()
                        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                            CodeEditorPage(LocalContext.current, navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavigation(modifier: Modifier = Modifier, navController: NavController) {
    val openTrackingInfoForm = remember { mutableStateOf(false) }
    val expanded = remember { mutableStateOf(false) }
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        title = {
            Text(
                text = "通知栏便笺"
            )
        },
        actions = {
            IconButton(
                onClick = {
                    expanded.value = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        openTrackingInfoForm.value = true
                        expanded.value = false
                    },
                    text = {
                        Text("添加便笺追踪")
                    }
                )
                DropdownMenuItem(
                    onClick = {
                        navController.navigate("code_editor")
                        expanded.value = false
                    },
                    text = {
                        Text("自定义打码平台接口")
                    }
                )
                DropdownMenuItem(
                    onClick = {
                        expanded.value = false
                    },
                    text = {
                        Text("刷新数据")
                    }
                )
            }
        },
        modifier = modifier
    )
    PopupTrackingInfoForm(openTrackingInfoForm)
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier, navController: NavController, snackbarHostState: SnackbarHostState, clickCallBack: () -> Unit) {

    val context = LocalContext.current
    val store = TrackingData(context)

    val testData = store.getData("test").collectAsState("")

    val testResponse = remember { mutableStateOf("") }
    var testVerificationTrace by remember { mutableStateOf("") }
    var testVerificationInfo by remember { mutableStateOf("") }

    Column (
        modifier.verticalScroll(rememberScrollState()).padding()
    ) {
        TrackingCard(
            modifier = Modifier,
            context = context,
            data = YsTrackingInfo(
                index = 0,
                uid = "236210923",
                cookie = store.getDataSync("__temp_cookie"),
                gameId = 0,
                lastRefreshTime = System.currentTimeMillis(),
                data = Json.decodeFromString<MiHoYoApi<YsNoteData>>(
                    "{\"retcode\":0,\"message\":\"OK\",\"data\":{\"current_resin\":119,\"max_resin\":160,\"resin_recovery_time\":\"19209\",\"finished_task_num\":0,\"total_task_num\":4,\"is_extra_task_reward_received\":false,\"remain_resin_discount_num\":3,\"resin_discount_num_limit\":3,\"current_expedition_num\":5,\"max_expedition_num\":5,\"expeditions\":[{\"avatar_side_icon\":\"https://act-webstatic.mihoyo.com/hk4e/e20200928calculate/item_avatar_side_icon_u1490e/bd4d97a04749da6208851b5b88f4f6dd.png\",\"status\":\"Finished\",\"remained_time\":\"0\"},{\"avatar_side_icon\":\"https://act-webstatic.mihoyo.com/hk4e/e20200928calculate/item_avatar_side_icon_u1490e/aa79fdfe1d3b8bf1552a0167634cc5aa.png\",\"status\":\"Ongoing\",\"remained_time\":\"14636\"},{\"avatar_side_icon\":\"https://act-webstatic.mihoyo.com/hk4e/e20200928calculate/item_avatar_side_icon_u1490e/a6d9337223cae80e51a5d82f89b99d54.png\",\"status\":\"Finished\",\"remained_time\":\"0\"},{\"avatar_side_icon\":\"https://act-webstatic.mihoyo.com/hk4e/e20200928calculate/item_avatar_side_icon_u1490e/8b1f1a23ade6093374ce2fdaedecf9ec.png\",\"status\":\"Finished\",\"remained_time\":\"0\"},{\"avatar_side_icon\":\"https://act-webstatic.mihoyo.com/hk4e/e20200928calculate/item_avatar_side_icon_u1490e/41bf4a8d22074fddb2b40a41715ce059.png\",\"status\":\"Ongoing\",\"remained_time\":\"14636\"}],\"current_home_coin\":2400,\"max_home_coin\":2400,\"home_coin_recovery_time\":\"0\",\"calendar_url\":\"\",\"transformer\":{\"obtained\":true,\"recovery_time\":{\"Day\":0,\"Hour\":0,\"Minute\":0,\"Second\":0,\"reached\":true},\"wiki\":\"https://bbs.mihoyo.com/ys/obc/content/1562/detail?bbs_presentation_style=no_header\",\"noticed\":false,\"latest_job_id\":\"0\"}}}"
                ).data!!
            )
        )
        Text("Text")

        Button(
            onClick = {
                clickCallBack();
            }
        ) {
            Text("创建常驻通知")
        }

        Button(
            onClick = {}
        ) {
            Text("取消常驻通知")
        }

        Button(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    store.saveData("test", "testData")
                }
            }
        ) {
            Text("添加DataStore")
        }

        Text("data: ${testData.value}")

        Button(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    store.removeData("test")
                }
            }
        ) {
            Text("删除DataStore")
        }


        Divider()
        Text(buildAnnotatedString {
            append("Keys: \n")
            append(store.getAllKeys().collectAsState(null).value?.joinToString("\n") { " [-] $it" })
        })
        Text("Codes: ")
        val captcha_code = store.getData("captcha_code").collectAsState("")
        Text(captcha_code.value)
        var returnValue by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        Button(
            onClick = {
                try {
                    val scriptEngine = UserScript(context)
                    class Callback(name: String, methodOrConstructor: Member, scope: Scriptable): FunctionObject(name, methodOrConstructor, scope) {
                        override fun call(
                            cx: org.mozilla.javascript.Context?,
                            scope: Scriptable?,
                            thisObj: Scriptable?,
                            args: Array<out Any>?
                        ): Any {
                            // returnValue = ((args ?: arrayOf("")) as Array<String>)[0]
                            return super.call(cx, scope, thisObj, args)
                        }
                    }
                    abstract class MyScriptable: ScriptableObject() {
                        fun callbackFunc(str: String){
                            returnValue = str
                        }
                    }
                    val callbackFunction = CallbackFunction("callback") { args ->
                        args?.let {
                            returnValue = args[0] as String
                        }
                    }
                    // val callback = Callback("callback", MyScriptable::class.java.getMethod("callbackFunc", String::class.java), scriptEngine.scriptEngine.get("global") as Scriptable)
                    scriptEngine.scriptEngine.put("callback", callbackFunction)
                    //returnValue =  as String
                    // scriptEngine.scriptEngine.eval("callback(String(1234))")
                    scriptEngine.invoke("getApi", "{\"challenge\":\"0574339c10888fd42aaa2d3aec0e10fc\",\"gt\":\"846fea0fd833b173bec7a25920851739\",\"new_captcha\":1,\"success\":1}")
                } catch (e: Exception) {
                    Log.e("ScriptRuntime", e.message ?: "")
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = e.message ?: ""
                        )
                    }
                    e.printStackTrace()
                } catch (e: Error) {
                    Log.e("ScriptRuntime", e.message ?: "")
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = e.message ?: ""
                        )
                    }
                    e.printStackTrace()
                }
            }
        ) { Text("获取脚本返回值") }
        Button(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    store.removeData("captcha_code")
                }
            }
        ) { Text("删除脚本") }
        Text(buildAnnotatedString { append(returnValue) })


        Divider()
        SelectionContainer {
            Text("testResponse: ${testResponse.value}")
        }
        Button(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    val response = TrackingNetwork.getTrackingData(
                        PlayerData(
                            uid = "236210923",
                            cookie = store.getDataAsync("__temp_cookie"),
                            game_id = 0
                        ),
                        context
                    )
                    //val response = TrackingNetwork.getFp("89asf89sdf7ynf78as")
                    Log.i("response", response)
                    if (JSONObject(response).get("retcode") == 1034){
                        TODO()
                    }
                    testResponse.value = "${System.currentTimeMillis()}: ${response}"
                }
            }
        ) {
            Text("获取数值")
        }

        Divider()
        Text("challenge_trace: ")
        SelectionContainer {
            Text(testVerificationTrace)
        }
        Text("testVerificationInfo: ")
        SelectionContainer {
            Text(testVerificationInfo)
        }
        Button(
            onClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    testVerificationTrace = generateRandomMd5String(16)
                    val response = TrackingNetwork.getVerification(
                        PlayerData(
                            uid = "236210923",
                            cookie = store.getDataAsync("__temp_cookie"),
                            game_id = 0
                        ),
                        testVerificationTrace,
                        context
                    )
                    //val response = TrackingNetwork.getFp("89asf89sdf7ynf78as")
                    Log.i("response", response)
                    testVerificationInfo = "${System.currentTimeMillis()}: ${response}"
                }
            }
        ) {
            Text("获取数值")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(navController: NavController, clickCallBack: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold (
        topBar = { TopNavigation(Modifier, navController) },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    contentColor = Color(0xFFFFFFFF),
                    containerColor = Color(0xFFC5002A),
                    snackbarData = data
                )
            }
        }
    ) { padding ->
        HomeScreen(Modifier.padding(padding), navController, snackbarHostState, clickCallBack)
    }
}

// 弹窗
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopupTrackingInfoForm(openForm: MutableState<Boolean>){
    val data = remember { mutableStateMapOf(
        "uid" to "",
        "cookie" to "",
        "game_id" to 0, // 0=GenshinImpact 1=HonkaiStarRail
    ) }
    val gameName = listOf("原神", "崩坏：星穹铁道");
    val _expanded = remember { mutableStateOf(false) }
    if (openForm.value) {
        AlertDialog(
            confirmButton = {

                Button(
                    onClick = {
                        openForm.value = false
                    }
                ) { Text("取消") }

                Button(
                    onClick = {
                        openForm.value = false
                    }
                ) { Text("确定") }

            },
            onDismissRequest = {},
            title = { Text("添加便笺追踪") },
            text = {
                Surface (
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column {
                        ExposedDropdownMenuBox(
                            expanded = _expanded.value,
                            onExpandedChange = { _expanded.value = !_expanded.value }
                        ){
                            TextField(
                                value = gameName[data.getValue("game_id") as Int],
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                readOnly = true,
                                onValueChange = { },
                                label = { Text("游戏") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(_expanded.value)
                                },
                                colors = ExposedDropdownMenuDefaults.textFieldColors()
                            )

                            ExposedDropdownMenu(
                                expanded = _expanded.value,
                                onDismissRequest = {
                                    _expanded.value = false
                                }
                            ) {
                                gameName.forEach{selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            data["game_id"] = gameName.indexOf(selectionOption)
                                            _expanded.value = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextField(
                            value = data.getValue("uid") as String,
                            onValueChange = {
                                data["uid"] = it
                                // Log.i("valueChange: ", it)
                            },
                            label = { Text("游戏UID") },
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.NumberPassword
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        TextField(
                            value = data.getValue("cookie") as String,
                            onValueChange = {
                                data["cookie"] = it
                                // Log.i("valueChange: ", it)
                            },
                            label = { Text("米游社Cookie") },
                            singleLine = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize().padding(0.dp, 10.dp)

        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    HoYoLABTheme {
        // Home({})
        // TopNavigation()
        // PopupForm(remember {mutableStateOf(true)})
    }
}