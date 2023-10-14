package com.revnote.hoyolab

import android.content.Context
import android.os.Build
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebView
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.json.JSONObject
import kotlin.reflect.KFunction

class MiHoYoJSInterface(val context: Context, val webView: WebView, val cookieMap: Map<String, String>, val uid: String, val serverName: String, val headers: Map<String, String>, val loadUrl: (String) -> Unit) {

    @Serializable
    data class Message(
        val method: String?,
        val callback: String?,
        @Serializable(with = JsonAsStringSerializer::class)
        val payload: String?
    )

    private val JSFunctions: Map<String, (String) -> String> = mapOf(
        "getStatusBarHeight" to ::getStatusBarHeight,
        "getDS" to ::getDS,
        "getDS2" to ::getDS2,
        "getHTTPRequestHeaders" to ::getHTTPRequestHeaders,
        "getCookieInfo" to ::getCookieInfo,
        "pushPage" to ::pushPage
    )

    @JavascriptInterface
    fun postMessage(str: String) {
        // {"method":"getStatusBarHeight","payload":null,"callback":"bbs_callback_0"}
        Log.i("webview", str)
        try {
            val message = Json { ignoreUnknownKeys = true }.decodeFromString<Message>(str)
            if (JSFunctions.containsKey(message.method)) {
                val returnData = JSFunctions[message.method]!!.invoke(message.payload ?: "")
                // Log.i("webviewEval", "javascript:window.mhyWebBridge(\"${message.callback}\", ${returnData})")
                webView.post {
                    webView.evaluateJavascript("javascript:window.mhyWebBridge(\"${message.callback}\", ${returnData})") { value ->
                        value?.let { Log.i("webviewCallback", it) }
                    }
                }
            }
        } catch (e: Error) {
            throw e
        }
    }

    private fun getStatusBarHeight(payload: String): String {
        // {"0":"bbs_callback_0","1":{"message":"","data":{"statusBarHeight":"24.081633"},"retcode":0}}
        return "{\"message\":\"\",\"data\":{\"statusBarHeight\":\"24.081633\"},\"retcode\":0}"
    }
    private fun getDS(payload: String): String {
        // {"message":"","data":{"DS":"1693586243,cb0XXX,fcc405d2643a65ab6e134b7e37XXXXXX"},"retcode":0}
        return "{\"message\":\"\",\"data\":{\"DS\":\"${com.revnote.hoyolab.getDS()}\"},\"retcode\":0}"
    }
    private fun getDS2(payload: String): String {
        // {"message":"","data":{"DS":"1693586363,175XXX,c803720db07391f50611bf26bcXXXXXX"},"retcode":0}
        return "{\"message\":\"\",\"data\":{\"DS\":\"${com.revnote.hoyolab.getDS2(payload)}\"},\"retcode\":0}"
    }
    private fun getHTTPRequestHeaders(payload: String): String {
        // {"message":"","data":{"x-rpc-device_name":"OPPO PDEM10","Referer":"https://app.mihoyo.com","x-rpc-app_version":"2.57.1","x-rpc-client_type":"2","x-rpc-device_fp":"38d7f0079a728","x-rpc-device_id":"9b3e5efe-98ea-3716-9380-324d363bXXXX","x-rpc-channel":"oppo","x-rpc-device_model":"PDEM10","x-rpc-sys_version":"7.1.2"},"retcode":0}
        val deviceId = getDeviceId(context)
        val deviceFp: FPData
        runBlocking {
            deviceFp = TrackingNetwork.getFp(deviceId, context)
        }
        return "{\"message\":\"\",\"data\":{\"x-rpc-device_name\":\"${Build.BRAND} ${Build.MODEL}\",\"Referer\":\"https://app.mihoyo.com\",\"x-rpc-app_version\":\"2.57.1\",\"x-rpc-client_type\":\"2\",\"x-rpc-device_fp\":\"${deviceFp.deviceFp}\",\"x-rpc-device_id\":\"${deviceId}\",\"x-rpc-channel\":\"${Build.BRAND}\",\"x-rpc-device_model\":\"${Build.MODEL}\",\"x-rpc-sys_version\":\"${Build.VERSION.RELEASE}\"},\"retcode\":0}"
    }
    private fun getCookieInfo(payload: String): String {
        // {"message":"","data":{"ltoken":"dE6XXXXXXXXXXXXXXXXXXXXXXNnFJ","login_ticket":"SIyE4XXXXXXXXXXXXXXXXXXXXXXIqLbS","ltuid":"3*******3"},"retcode":0}
        return "{\"message\":\"\",\"data\":{\"ltoken\":\"${cookieMap["ltoken"]}\",\"login_ticket\":\"${cookieMap["login_ticket"]}\",\"ltuid\":\"${cookieMap["ltuid"]}\"},\"retcode\":0}"
    }
    private fun pushPage(payload: String): String {
        Log.i("webview_pushPage", JSONObject(payload).getString("page"))
        loadUrl(JSONObject(payload).getString("page"))
        return "{\"message\":\"\",\"data\":{},\"retcode\":0}"
    }
}