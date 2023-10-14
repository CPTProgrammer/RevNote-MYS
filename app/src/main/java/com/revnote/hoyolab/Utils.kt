package com.revnote.hoyolab

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.util.*
import javax.script.Invocable
import kotlin.random.Random

@Serializable
data class MiHoYoApi<T>(
    val retcode: Int,
    val message: String,
    val data: T? = null
)

fun getDS2(uid: String, serverName: String): String {
    // Part 1: current unix timestamp
    val timestamp = System.currentTimeMillis() / 1000;

    // Part 2: a random integer from 100,000 to 200,000
    var randomString = Random.nextInt(100_000, 200_000)
    if (randomString == 100_000){
        randomString = 642_367
    }

    // Part 3: MD5 hash of salt
    val salt = "xV8v4Qu54lUKrEYFZkJhB8cuOh9Asafs"

    // val body = "{\"role\": \"${uid}\"}"
    val body = ""
    val quest = "role_id=${uid}&server=${serverName}".split('&').sorted().joinToString("&")
    // Log.i("quest", quest)

    val sign = "salt=${salt}&t=${timestamp}&r=${randomString}&b=${body}&q=${quest}".md5()

    return "${timestamp},${randomString},${sign}"
}
@OptIn(ExperimentalStdlibApi::class)
fun getDS2(payload: String): String {
    // Part 1: current unix timestamp
    val timestamp = System.currentTimeMillis() / 1000;

    // Part 2: a random integer from 100,000 to 200,000
    var randomString = Random.nextInt(100_000, 200_000)
    if (randomString == 100_000){
        randomString = 642_367
    }

    // Part 3: MD5 hash of salt
    val salt = "xV8v4Qu54lUKrEYFZkJhB8cuOh9Asafs"

    @Serializable
    data class PayloadJson(
        val body: String,
        @Serializable(with = JsonAsStringSerializer::class)
        val query: String
    )
//    val payloadJson = Json.parseToJsonElement(payload).jsonObject
//    val body = payloadJson["body"]!!.jsonPrimitive  .toString().replace("\"", "")
//
//    var quest = ""
//    payloadJson["query"]?.let {
//        quest = payloadJson["query"]!!.jsonObject.toMap().entries.joinToString(
//            separator = "&"
//        ).replace("\"", "").split('&').sorted().joinToString("&") // 好乱啊
//    }

    val payloadJson = Json.decodeFromString<PayloadJson>(payload)
    val queryJSON = JSONObject(payloadJson.query)
    val queryJSONKeys = queryJSON.names() ?: JSONArray()
    val queryMap: MutableMap<String, String> = mutableMapOf()

    val body = payloadJson.body
    var quest: String
    for (i in 0..<queryJSONKeys.length()){
        val key = queryJSONKeys.getString(i)
        queryMap[key] = queryJSON.getString(key)
    }
    quest = queryMap.toSortedMap().map {(key, value) -> "$key=$value"}.joinToString(separator = "&")

    val sign = "salt=${salt}&t=${timestamp}&r=${randomString}&b=${body}&q=${quest}".md5()
    Log.i("webviewSign", "salt=${salt}&t=${timestamp}&r=${randomString}&b=${body}&q=${quest}")

    return "${timestamp},${randomString},${sign}"
}

fun getDS(): String {
    // Part 1: current unix timestamp
    val timestamp = System.currentTimeMillis() / 1000;

    // Part 2: random string of length 6
    val randomString = generateRandomString(6)

    // Part 3: MD5 hash of salt
    val salt = "1XgQyjgs3iGBwEwgnqySnqtPdw0Yi2mP" // 2.57.1 - K2 Salt

    val sign = "salt=${salt}&t=${timestamp}&r=${randomString}".md5()

    return "${timestamp},${randomString},${sign}"
}

@SuppressLint("HardwareIds")
fun getDeviceId(context: Context): String {
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.System.ANDROID_ID)
    val uuid = UUID.nameUUIDFromBytes(androidId.toByteArray())
    return uuid.toString()
}

fun generateRandomString(length: Int): String {
    val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (1..length)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
fun generateRandomMd5String(length: Int): String {
    val charPool: List<Char> = ('a'..'f') + ('0'..'9')
    return (1..length)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

object JsonAsStringSerializer: JsonTransformingSerializer<String>(tSerializer = String.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        return JsonPrimitive(value = element.toString())
    }
}

//fun readTextMateLanguage(file: InputStream) {
////    val registry = Registry(RegistryOptions(
////        regexLib = OnigLib(),
////        loadGrammar = {
////            parsePLIST()
////        }
////    ))
//    return parsePLIST(file)
//}