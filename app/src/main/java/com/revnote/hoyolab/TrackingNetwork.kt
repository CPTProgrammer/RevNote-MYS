package com.revnote.hoyolab

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

@Serializable
data class PlayerData (
    val uid: String,
    val cookie: String,
    val game_id: Int
)

@Serializable
data class VerificationInfo (
    val challenge: String?,
    val gt: String?,
    val new_captcha: Int?,
    val success: Int
)

val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).build()

class TrackingNetwork {
    companion object {

        private const val ysApiCn = "https://api-takumi-record.mihoyo.com/game_record/app/genshin/api/dailyNote"
        private const val srApiCn = "https://api-takumi-record.mihoyo.com/game_record/app/hkrpg/api/note"

        suspend fun getFp(deviceId: String, context: Context): FPData = suspendCoroutine{continuation ->
            runBlocking {
                val tracking = TrackingData(context)
                val fpData = tracking.getDataSync("device_fp_data")
                if (fpData != "") {
                    continuation.resume(Json.decodeFromString<FPData>(fpData))
                    return@runBlocking
                }
                // Log.i("getFp", "reused fpData from DataStore")

                @Serializable
                data class GetFpBody(
                    val seed_id: String,
                    val device_id: String,
                    val platform: String, // = "5"
                    val seed_time: String,
                    val ext_fields: String, // = "{\"userAgent\":\"Mozilla/5.0 (Linux; Android 13; M2101K9C Build/TKQ1.220829.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) miHoYoBBS/2.44.1\",\"browserScreenSize\":281520,\"maxTouchPoints\":5,\"isTouchSupported\":true,\"browserLanguage\":\"zh-CN\",\"browserPlat\":\"iPhone\",\"browserTimeZone\":\"Asia/Shanghai\",\"webGlRender\":\"Apple GPU\",\"webGlVendor\":\"Apple Inc.\",\"numOfPlugins\":0,\"listOfPlugins\":\"unknown\",\"screenRatio\":3,\"deviceMemory\":\"unknown\",\"hardwareConcurrency\":\"4\",\"cpuClass\":\"unknown\",\"ifNotTrack\":\"unknown\",\"ifAdBlock\":0,\"hasLiedResolution\":1,\"hasLiedOs\":0,\"hasLiedBrowser\":0}",
                    val app_name: String, // = "account_cn",
                    val device_fp: String, // = "38d7ee834d1e9"
                )

                @Serializable
                data class responseData(
                    val device_fp: String,
                    val code: Int,
                    val msg: String
                )

                @Serializable
                data class responseBody(
                    val retcode: Int,
                    val message: String,
                    val data: responseData
                )

                val seedId = generateRandomMd5String(16)
                val seedTime = System.currentTimeMillis().toString()

                val getFpUrl = "https://public-data-api.mihoyo.com/device-fp/api/getFp"
                val bodyStr = Json.encodeToString(
                    GetFpBody(
                        seed_id = seedId,
                        device_id = deviceId,
                        platform = "5",
                        seed_time = seedTime,
                        ext_fields = "{\"userAgent\":\"Mozilla/5.0 (Linux; Android 13; M2101K9C Build/TKQ1.220829.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) miHoYoBBS/2.44.1\",\"browserScreenSize\":281520,\"maxTouchPoints\":5,\"isTouchSupported\":true,\"browserLanguage\":\"zh-CN\",\"browserPlat\":\"iPhone\",\"browserTimeZone\":\"Asia/Shanghai\",\"webGlRender\":\"Apple GPU\",\"webGlVendor\":\"Apple Inc.\",\"numOfPlugins\":0,\"listOfPlugins\":\"unknown\",\"screenRatio\":3,\"deviceMemory\":\"unknown\",\"hardwareConcurrency\":\"4\",\"cpuClass\":\"unknown\",\"ifNotTrack\":\"unknown\",\"ifAdBlock\":0,\"hasLiedResolution\":1,\"hasLiedOs\":0,\"hasLiedBrowser\":0}",
                        app_name = "account_cn",
                        device_fp = "38d7ee834d1e9"
                    )
                )
                val body = bodyStr.toRequestBody()
                // Log.i("bodyStr", bodyStr)

                val request = Request.Builder()
                    .url(getFpUrl)
                    .post(body)
                    .addHeader("x-rpc-app_version", "2.26.1")
                    .addHeader("x-rpc-client_type", "5")
                    .addHeader("x-rpc-sys_version", "13")
                    .addHeader("x-rpc-device_id", deviceId)
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 13; M2101K9C Build/TKQ1.220829.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/108.0.5359.128 Mobile Safari/537.36 miHoYoBBS/2.44.1"
                    )
                    .addHeader("Origin", "https://public-data-api.mihoyo.com")
                    .addHeader("Host", "public-data-api.mihoyo.com")
                    .addHeader("Referer", "https://webstatic.mihoyo.com/")
                    .addHeader("X-Requested-With", "com.mihoyo.hyperion")
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        throw e
                    }

                    override fun onResponse(call: Call, response: Response) {
                        //{"retcode":0,"message":"OK","data":{"device_fp":"38d7f004c6fa6","code":200,"msg":"ok"}}
                        if (response.body != null) {
                            // val data = Json.parseToJsonElement(response.body!!.string()).jsonObject.getValue("data")
                            val data = Json.decodeFromString<responseBody>(response.body!!.string())
                            // ("device_fp", data.data.device_fp)
                            val returnData = FPData(
                                fpSeedId = seedId,
                                fpSeedTime = seedTime,
                                deviceFp = data.data.device_fp
                            )
                            CoroutineScope(Dispatchers.IO).launch {
                                tracking.saveData("device_fp_data", Json.encodeToString(returnData))
                            }
                            continuation.resume(returnData)
                        } else {
                            continuation.resume(FPData())
                        }
                    }
                })
            }
        }

        suspend fun getTrackingData(playerData: PlayerData, context: Context): String = suspendCoroutine { continuation ->
            runBlocking {
                val cookies = playerData.cookie.split(";\\s*".toRegex())
                val cookieMap = mutableMapOf<String, String>()
                for (cookie in cookies) {
                    val temp = cookie.split("=")
                    if (temp.size < 2){
                        continue
                    }
                    cookieMap[temp[0]] = temp[1]
                }

                val uid = playerData.uid
                val serverName = if (playerData.game_id == 0) { if (uid[0] == '5') "cn_qd01" else "cn_gf01" } else { "prod_gf_cn" }
                val url = "${if (playerData.game_id == 0) ysApiCn else srApiCn}?role_id=${uid}&server=${serverName}"
                // Log.i("url", url)
                // val deviceId = generateRandomString(32)
                val deviceId = getDeviceId(context)

                /*val fpSeedTime = System.currentTimeMillis().toString()
                val fpSeedId = generateRandomMd5String(16)
                var deviceFp: String
                if (cookieMap.containsKey("DEVICEFP")) {
                    deviceFp = cookieMap["DEVICEFP"]!!
                } else {
                    deviceFp = getFp(deviceId, fpSeedId, fpSeedTime)
                }*/
                val deviceFpData = getFp(deviceId, context)
                // Log.i("test", deviceFpData.deviceFp)
                cookieMap["DEVICEFP"] = deviceFpData.deviceFp
                cookieMap["DEVICEFP_SEED_ID"] = deviceFpData.fpSeedId
                cookieMap["DEVICEFP_SEED_TIME"] = deviceFpData.fpSeedTime

                val changedCookie = cookieMap.entries.joinToString("; ") { (key, value) -> "${key}=${value}" }

                val deviceName = "${Build.BRAND}%20${Build.MODEL}"
                val androidVersion = Build.VERSION.RELEASE.toString()
                val appVersion = "2.57.1"

                // Log.i("DS", getDS(uid, serverName))
                // Log.i("DS", changedCookie)

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Host", "api-takumi-record.mihoyo.com")
                    .addHeader("DS", getDS2(uid, serverName))
                    .addHeader("Origin", "https://webstatic.mihoyo.com")
                    //.addHeader("x-rpc-app_version", "2.26.1")
                    .addHeader("x-rpc-app_version", appVersion)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android ${androidVersion}; ${Build.MODEL} Build/N2G47H; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/68.0.3440.70 Mobile Safari/537.36 miHoYoBBS/${appVersion}")
                    //.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android ${androidVersion}; M2101K9C Build/TKQ1.220829.002; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/108.0.5359.128 Mobile Safari/537.36 miHoYoBBS/${appVersion}")
                    .addHeader("x-rpc-device_id", deviceId)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("x-rpc-device_name", deviceName)
                    .addHeader("x-rpc-page", if (playerData.game_id == 0) "ys_#/ys" else "rpg_#/rpg")
                    .addHeader("x-rpc-device_fp", deviceFpData.deviceFp)
                    .addHeader("x-rpc-sys_version", androidVersion)
                    .addHeader("x-rpc-client_type", "5")
                    .addHeader("Referer", if (playerData.game_id == 0) "https://webstatic.mihoyo.com/app/community-game-records/?bbs_presentation_style=fullscreen&bbs_auth_required=true" else "https://webstatic.mihoyo.com/app/community-game-records/rpg/index.html?mhy_presentation_style=fullscreen")
                    // .addHeader("Accept-Encoding", "gzip, deflate") // 加了之后Okhttp不会解压response内容，导致乱码。。
                    .addHeader("Accept-Language", "zh-CN,en-US;q=0.9")
                    .addHeader("Cookie", changedCookie)
                    .addHeader("X-Requested-With", "com.mihoyo.hyperion")
                    .build()

                try {
                    // val response = client.newCall(request).execute()
                    // response.body?.string() ?: ""

                    client.newCall(request).enqueue(object: Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            continuation.resume("Failed to patch miHoYo api")
                        }
                        override fun onResponse(call: Call, response: Response) {
                            //{"data":null,"message":"invalid request","retcode":-10001}
                            //{"data":null,"message":"","retcode":1034}
                            //{"retcode":0,"message":"OK","data":{"current_resin":119,"max_resin":160,"resin_recovery_time":"19209","finished_task_num":0,"total_task_num":4,"is_extra_task_reward_received":false,"remain_resin_discount_num":3,"resin_discount_num_limit":3,"current_expedition_num":5,"max_expedition_num":5,"expeditions":[{"avatar_side_icon":"https://act-webstatic.mihoyo.com/hk4e/e20200928calculate/item_avatar_side_icon_u1490e/bd4d97a04749da6208851b5b88f4f6dd.png","status":"Finished","remained_time":"0"},{"avatar_side_icon":"https://act-webstatic.mihoyo.com/hk4e/e20200928calculate/item_avatar_side_icon_u1490e/aa79fdfe1d3b8bf1552a0167634cc5aa.png","status":"Ongoing","remained_time":"14636"},{"avatar_side_icon":"https://act-webstatic.mihoyo.com/hk4e/e20200928calculate/item_avatar_side_icon_u1490e/a6d9337223cae80e51a5d82f89b99d54.png","status":"Finished","remained_time":"0"},{"avatar_side_icon":"https://act-webstatic.mihoyo.com/hk4e/e20200928calculate/item_avatar_side_icon_u1490e/8b1f1a23ade6093374ce2fdaedecf9ec.png","status":"Finished","remained_time":"0"},{"avatar_side_icon":"https://act-webstatic.mihoyo.com/hk4e/e20200928calculate/item_avatar_side_icon_u1490e/41bf4a8d22074fddb2b40a41715ce059.png","status":"Ongoing","remained_time":"14636"}],"current_home_coin":2400,"max_home_coin":2400,"home_coin_recovery_time":"0","calendar_url":"","transformer":{"obtained":true,"recovery_time":{"Day":0,"Hour":0,"Minute":0,"Second":0,"reached":true},"wiki":"https://bbs.mihoyo.com/ys/obc/content/1562/detail?bbs_presentation_style=no_header","noticed":false,"latest_job_id":"0"}}}
                            //{"retcode":0,"message":"OK","data":{"current_stamina":38,"max_stamina":180,"stamina_recover_time":50923,"accepted_epedition_num":4,"total_expedition_num":4,"expeditions":[{"avatars":["https://act-webstatic.mihoyo.com/darkmatter/hkrpg/prod_gf_cn/item_icon_770efr/b79e5b48e96047984169567baf0b6283.png","https://act-webstatic.mihoyo.com/darkmatter/hkrpg/prod_gf_cn/item_icon_770efr/16ab5fcdbb9f795eaa190406d3315bea.png"],"status":"Finished","remaining_time":0,"name":"毁灭者的覆灭"},{"avatars":["https://act-webstatic.mihoyo.com/darkmatter/hkrpg/prod_gf_cn/item_icon_770efr/cdaa5fe7a9a5f37ad1ee51c1a85f7b31.png","https://act-webstatic.mihoyo.com/darkmatter/hkrpg/prod_gf_cn/item_icon_770efr/51b712b003ab00ba0f04649b62e211be.png"],"status":"Ongoing","remaining_time":58683,"name":"无名之地，无名之人"},{"avatars":["https://act-webstatic.mihoyo.com/darkmatter/hkrpg/prod_gf_cn/item_icon_770efr/c2c14f1274638488083c724bfa5d0f0c.png","https://act-webstatic.mihoyo.com/darkmatter/hkrpg/prod_gf_cn/item_icon_770efr/e09769c3a5ecf30b88fbcf2c96e91fa6.png"],"status":"Ongoing","remaining_time":58684,"name":"阿卡夏记录"},{"avatars":["https://act-webstatic.mihoyo.com/darkmatter/hkrpg/prod_gf_cn/item_icon_770efr/3ecca710032d918579f4d83edd7fd2e2.png","https://act-webstatic.mihoyo.com/darkmatter/hkrpg/prod_gf_cn/item_icon_770efr/09097055f0769626d22edac3bc553a90.png"],"status":"Ongoing","remaining_time":58686,"name":"看不见的手"}],"current_train_score":500,"max_train_score":500,"current_rogue_score":0,"max_rogue_score":14000,"weekly_cocoon_cnt":0,"weekly_cocoon_limit":3}}
                            //https://bbs.mihoyo.com/app/community-game-records/?bbs_presentation_style=fullscreen
                            //https://webstatic.mihoyo.com/app/community-game-records/?bbs_presentation_style=fullscreen&bbs_auth_required=true&v=101&gid=2&user_id=332206713&game_id=2&uid=332206713
                            continuation.resume(response.body?.string() ?: "No Response")
                        }
                    })
                } catch (e: Error) {
                    continuation.resume("Invalid Data")
                }
            }
        }

        /**
         * @param challengeTrace 16位类似于md5的字符串（仅包含0-9及a-f）
         */
        suspend fun getVerification(playerData: PlayerData, challengeTrace: String, context: Context): String = suspendCoroutine { continuation ->
            runBlocking {
                val cookies = playerData.cookie.split(";\\s*".toRegex())
                val cookieMap = mutableMapOf<String, String>()
                for (cookie in cookies) {
                    val temp = cookie.split("=")
                    if (temp.size < 2){
                        continue
                    }
                    cookieMap[temp[0]] = temp[1]
                }

                val url = "https://api-takumi-record.mihoyo.com/game_record/app/card/wapi/createVerification?is_high=true"

                val deviceId = getDeviceId(context)

                val deviceFpData = getFp(deviceId, context)

                cookieMap["DEVICEFP"] = deviceFpData.deviceFp
                cookieMap["DEVICEFP_SEED_ID"] = deviceFpData.fpSeedId
                cookieMap["DEVICEFP_SEED_TIME"] = deviceFpData.fpSeedTime

                val changedCookie = cookieMap.entries.joinToString("; ") { (key, value) -> "${key}=${value}" }

                val deviceName = "${Build.BRAND}%20${Build.MODEL}"
                val androidVersion = Build.VERSION.RELEASE.toString()
                val appVersion = "2.57.1"

                // Log.i("DS", getDS(uid, serverName))
                // Log.i("DS", changedCookie)

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Cookie", changedCookie)
                    .addHeader("DS", getDS2(JSONObject(mapOf(
                        "query" to JSONObject(mapOf(
                            "is_high" to "true"
                        )),
                        "body" to ""
                    )).toString()))
                    .addHeader("Host", "api-takumi-record.mihoyo.com")
                    .addHeader("Origin", "https://webstatic.mihoyo.com")
                    .addHeader("Pragma", "no-cache")
                    .addHeader("Referer", "https://webstatic.mihoyo.com/")
                    .addHeader("Sec-Fetch-Dest", "empty")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-site")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android ${androidVersion}; ${Build.MODEL} Build/N2G47H; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/68.0.3440.70 Mobile Safari/537.36 miHoYoBBS/${appVersion}")
                    .addHeader("X-Requested-With", "com.mihoyo.hyperion")
                    .addHeader("x-rpc-app_version", appVersion)
                    .addHeader("x-rpc-challenge_game", "2")
                    .addHeader("x-rpc-challenge_path", if (playerData.game_id == 0) ysApiCn else srApiCn)
                    .addHeader("x-rpc-challenge_trace", "$challengeTrace:$challengeTrace:0:1")
                    .addHeader("x-rpc-client_type", "5")
                    .addHeader("x-rpc-device_fp", deviceFpData.deviceFp)
                    .addHeader("x-rpc-device_id", deviceId)
                    .addHeader("x-rpc-device_name", deviceName)
                    .addHeader("x-rpc-page", if (playerData.game_id == 0) "ys_#/ys" else "rpg_#/rpg")
                    .addHeader("x-rpc-sys_version", androidVersion)
                    .addHeader("x-rpc-tool_verison", if (playerData.game_id == 0) "ys" else "rpg") //为什么是verison而不是version，是不是拼错了笑死
                    .build()

                try {
                    client.newCall(request).enqueue(object: Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            continuation.resume("Failed to patch miHoYo api")
                        }
                        override fun onResponse(call: Call, response: Response) {
                            continuation.resume(response.body?.string() ?: "No Response")
                        }
                    })
                } catch (e: Error) {
                    continuation.resume("Invalid Data")
                }
            }
        }

        /**
         * @param challengeTrace 16位类似于md5的字符串（仅包含0-9及a-f）
         */
        suspend fun verifyVerification(playerData: PlayerData, challengeTrace: String, context: Context): String = suspendCoroutine { continuation ->
            runBlocking {
                val cookies = playerData.cookie.split(";\\s*".toRegex())
                val cookieMap = mutableMapOf<String, String>()
                for (cookie in cookies) {
                    val temp = cookie.split("=")
                    if (temp.size < 2){
                        continue
                    }
                    cookieMap[temp[0]] = temp[1]
                }

                val url = "https://api-takumi-record.mihoyo.com/game_record/app/card/wapi/createVerification?is_high=true"

                val deviceId = getDeviceId(context)

                val deviceFpData = getFp(deviceId, context)

                cookieMap["DEVICEFP"] = deviceFpData.deviceFp
                cookieMap["DEVICEFP_SEED_ID"] = deviceFpData.fpSeedId
                cookieMap["DEVICEFP_SEED_TIME"] = deviceFpData.fpSeedTime

                val changedCookie = cookieMap.entries.joinToString("; ") { (key, value) -> "${key}=${value}" }

                val deviceName = "${Build.BRAND}%20${Build.MODEL}"
                val androidVersion = Build.VERSION.RELEASE.toString()
                val appVersion = "2.57.1"

                // Log.i("DS", getDS(uid, serverName))
                // Log.i("DS", changedCookie)

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                    .addHeader("Cache-Control", "no-cache")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Cookie", changedCookie)
                    .addHeader("DS", getDS2(JSONObject(mapOf(
                        "query" to JSONObject(mapOf(
                            "is_high" to "true"
                        )),
                        "body" to ""
                    )).toString()))
                    .addHeader("Host", "api-takumi-record.mihoyo.com")
                    .addHeader("Origin", "https://webstatic.mihoyo.com")
                    .addHeader("Pragma", "no-cache")
                    .addHeader("Referer", "https://webstatic.mihoyo.com/")
                    .addHeader("Sec-Fetch-Dest", "empty")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-site")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android ${androidVersion}; ${Build.MODEL} Build/N2G47H; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/68.0.3440.70 Mobile Safari/537.36 miHoYoBBS/${appVersion}")
                    .addHeader("X-Requested-With", "com.mihoyo.hyperion")
                    .addHeader("x-rpc-app_version", appVersion)
                    .addHeader("x-rpc-challenge_game", "2")
                    .addHeader("x-rpc-challenge_path", if (playerData.game_id == 0) ysApiCn else srApiCn)
                    .addHeader("x-rpc-challenge_trace", "$challengeTrace:$challengeTrace:0:1")
                    .addHeader("x-rpc-client_type", "5")
                    .addHeader("x-rpc-device_fp", deviceFpData.deviceFp)
                    .addHeader("x-rpc-device_id", deviceId)
                    .addHeader("x-rpc-device_name", deviceName)
                    .addHeader("x-rpc-page", if (playerData.game_id == 0) "ys_#/ys" else "rpg_#/rpg")
                    .addHeader("x-rpc-sys_version", androidVersion)
                    .addHeader("x-rpc-tool_verison", if (playerData.game_id == 0) "ys" else "rpg") //为什么是verison而不是version，是不是拼错了笑死
                    .build()

                try {
                    client.newCall(request).enqueue(object: Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            continuation.resume("Failed to patch miHoYo api")
                        }
                        override fun onResponse(call: Call, response: Response) {
                            continuation.resume(response.body?.string() ?: "No Response")
                        }
                    })
                } catch (e: Error) {
                    continuation.resume("Invalid Data")
                }
            }
        }
    }
}