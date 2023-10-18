package com.revnote.hoyolab

import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

// 使用时需要 try-catch
class UserScript(context: Context) {
    companion object {
        const val GET_API = "getApi"
        const val RESOLVE_API_RESPONSE = "resolveApiResponse"
    }

    val scriptEngine: ScriptEngine

    init {
        scriptEngine = ScriptEngineManager().getEngineByExtension("js")

        val store = TrackingData(context)
        val captchaCode = store.getDataSync("captcha_code")

        scriptEngine.put("RequestBuilder", Request.Builder())
        scriptEngine.put("OkHttpClientBuilder", OkHttpClient.Builder())
        scriptEngine.put("UserScriptInterface", UserScriptInterface)

        scriptEngine.eval("""
            function Promise(executor) {
                this.state = 'pending';
                this.value = undefined;
                this.onFulfilledCallbacks = [];
                this.onRejectedCallbacks = [];

                const resolve = (value) => {
                    if (this.state === 'pending') {
                        this.state = 'fulfilled';
                        this.value = value;
                        this.onFulfilledCallbacks.forEach(callback => callback(value));
                    }
                };

                const reject = (reason) => {
                    if (this.state === 'pending') {
                        this.state = 'rejected';
                        this.value = reason;
                        this.onRejectedCallbacks.forEach(callback => callback(reason));
                    }
                };

                try {
                    executor(resolve, reject);
                } catch (error) {
                    reject(error);
                }
            }
            Promise.prototype.then = function (onFulfilled, onRejected) {
                if (this.state === 'fulfilled') {
                    onFulfilled(this.value);
                } else if (this.state === 'rejected') {
                    onRejected(this.value);
                } else if (this.state === 'pending') {
                    this.onFulfilledCallbacks.push(onFulfilled);
                    this.onRejectedCallbacks.push(onRejected);
                }
            };
            
            function objectToKeyValueString(obj) {
                const keyValuePairs = [];
                for (let key in obj) {
                    if (obj.hasOwnProperty(key)) {
                        keyValuePairs.push(key + "=" + obj[key]);
                    }
                }
                const result = keyValuePairs.join('\n');
                return result;
            }
        """.trimIndent())

        scriptEngine.eval(captchaCode)
    }

    fun getApi(challengeData: VerificationInfo) {
        getApi(Json.encodeToString(VerificationInfo.serializer(), challengeData))
    }
    fun getApi(challengeData: String) {
        invoke(GET_API, challengeData)
    }

    fun invoke(funcName: String, vararg args: String?): Any? {
        val invocable = scriptEngine as Invocable
        return invocable.invokeFunction(funcName, *args)
    }
}

class CallbackFunction(private val className: String, private val function: (Array<out Any>?) -> Unit): Function, ScriptableObject() {
    override fun getClassName(): String {
        return className
    }

    override fun call(
        cx: org.mozilla.javascript.Context?,
        scope: Scriptable?,
        thisObj: Scriptable?,
        args: Array<out Any>?
    ) {
        function(args)
    }

    override fun construct(cx: org.mozilla.javascript.Context?, scope: Scriptable?, args: Array<out Any>?): Scriptable {
        TODO("No Constructor")
    }
}

interface UserScriptInterface {
    companion object {
        fun test(): String {
            return "TEST RESPONSE"
        }
    }
}