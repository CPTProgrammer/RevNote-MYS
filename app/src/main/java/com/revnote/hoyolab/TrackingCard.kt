package com.revnote.hoyolab

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.revnote.hoyolab.ui.theme.HoYoLABTheme
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.max

val Int.dpTextUnit: TextUnit
    @Composable
    get() = with(LocalDensity.current) { this@dpTextUnit.dp.toSp() }
val Dp.toSp: TextUnit
    @Composable
    get() = with(LocalDensity.current) { this@toSp.toSp() }
val Dp.toPx: Float
    @Composable
    get() = with(LocalDensity.current) { this@toPx.toPx() }

@OptIn(ExperimentalTextApi::class)
val fontFamily = FontFamily(
    Font(R.font.alibabapuhuiti_regular, FontWeight.Normal),
    Font(R.font.alibabapuhuiti_medium, FontWeight.Medium),
    Font(R.font.alibabapuhuiti_semibold, FontWeight.SemiBold),
    Font(R.font.alibabapuhuiti_bold, FontWeight.Bold),
    Font(R.font.alibabapuhuiti_extrabold, FontWeight.ExtraBold),
    Font(R.font.alibabapuhuiti_heavy, FontWeight.W800),
    Font(R.font.alibabapuhuiti_black, FontWeight.Black)
)
val genshinFont = FontFamily(
    Font(R.font.genshin_font)
)

// from https://developer.android.google.cn/jetpack/compose/layouts/alignment-lines
fun Modifier.firstBaselineToTop(
    firstBaselineToTop: Dp
) = layout { measurable, constraints ->
    // Measure the composable
    val placeable = measurable.measure(constraints)

    // Check the composable has a first baseline
    check(placeable[FirstBaseline] != AlignmentLine.Unspecified)
    val firstBaseline = placeable[FirstBaseline]

    // Height of the composable with padding - first baseline
    val placeableY = firstBaselineToTop.roundToPx() - firstBaseline
    val height = placeable.height + placeableY
    layout(placeable.width, height) {
        // Where the composable gets placed
        placeable.placeRelative(0, placeableY)
    }
}

data class CardColorScheme(
    val backgroundColor: Color = Color(0xFF000000),
    val backgroundColorSecondary: Color = Color(0xFF000000),
    val boxBackgroundColor: Color = Color(0xFF000000),
    val boxBackgroundColorSecondary: Color = Color(0xFF000000),
    val foregroundColor: Color = Color(0xFF000000),
    val foregroundColorSecondary: Color = Color(0xFF000000),
    val foregroundColorTertiary: Color = Color(0xFF000000),
)

@Composable
fun TrackingCard (
    modifier: Modifier = Modifier,
    data: YsTrackingInfo,
    context: Context
) {
    var cardDp by remember { mutableStateOf(0.dp) }
    var cardWidth by remember { mutableStateOf(0.dp) }

    var openWeb by remember { mutableStateOf(false) }

//    var cardDpLoaded by remember { mutableStateOf(false) }
//
//    LaunchedEffect(cardDp) {
//        cardDpLoaded = cardDp != 0.dp
//    }

    val colorScheme = CardColorScheme(
        backgroundColor = Color(0xFFEFE5D7),
        backgroundColorSecondary = Color(0xFFE2CEB3),
        boxBackgroundColor = Color(0xFFFDF6F0),
        boxBackgroundColorSecondary = Color(0xFFF7EDE5),
        foregroundColor = Color(0xFF685A47)
    )

    MaterialTheme (
        typography = Typography(
            displayMedium = TextStyle(
                fontFamily = fontFamily,
                fontSize = 16.dpTextUnit,
                lineHeight = 20.dpTextUnit,
                fontWeight = FontWeight.Black
            )
        )
    ) {
        Column (
            modifier = modifier.onSizeChanged{
                cardWidth = with(Density(context)) { it.width.toDp() }
                cardDp = cardWidth / 392f
            }.fillMaxWidth().padding(18 * cardDp, 18 * cardDp, 18 * cardDp, 0 * cardDp).aspectRatio(2f).background(
                color = colorScheme.backgroundColor,
                shape = RoundedCornerShape(18 * cardDp)
            )
        ) {
            Row (
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                Box(
                    modifier = Modifier.padding(12 * cardDp, 12 * cardDp, 0.dp, 0.dp).width(108 * cardDp).height(148 * cardDp).background(
                        color = colorScheme.boxBackgroundColor,
                        shape = RoundedCornerShape(12 * cardDp)
                    ).graphicsLayer {
                        clip = true
                        shape = RoundedCornerShape(12 * cardDp)
                    }
                ) {
                    VerticalProgressIndicator(
                        width = 108 * cardDp,
                        height = 148 * cardDp,
                        color = colorScheme.boxBackgroundColorSecondary,
                        progress = (data.data.max_resin * 8f * 60f - max(data.data.resin_recovery_time.toInt() - (System.currentTimeMillis() - data.lastRefreshTime) / 1000, 0)) / (data.data.max_resin * 8f * 60f)
                    )
                }
                Column(
                    modifier = Modifier.padding(12 * cardDp, 12 * cardDp, 0.dp, 0.dp).width(164 * cardDp).height(148 * cardDp)
                ) {
                    Column (
                        modifier = Modifier.padding(0.dp).width(164 * cardDp).height(100 * cardDp).background(
                            color = colorScheme.boxBackgroundColor,
                            shape = RoundedCornerShape(12 * cardDp)
                        )
                    ) {

                    }
                    Row (
                        modifier = Modifier.padding(0.dp, 12 * cardDp, 0.dp, 0.dp).width(164 * cardDp).height(36 * cardDp).background(
                            color = colorScheme.boxBackgroundColor,
                            shape = RoundedCornerShape(12 * cardDp)
                        ),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        for (expedition in data.data.expeditions) {
                            val remainedTime = max(expedition.remained_time.toInt() - (System.currentTimeMillis() - data.lastRefreshTime) / 1000, 0)
                            Box (
                                modifier = Modifier.padding(3 * cardDp, 6 * cardDp).width(24 * cardDp).height(24 * cardDp).circleShadow(
                                    color = Color(0xFF33DD7C),
                                    alpha = 0.99f,
                                    shadowRadius = if (remainedTime > 0) 0.dp else (3 * cardDp)
                                ).background(
                                    color = Color(0xFF887D6E),
                                    shape = CircleShape
                                )
                            ) {
                                CircularProgressIndicator(
                                    progress = (72000f - remainedTime) / 72000f,
                                    strokeWidth = 2 * cardDp,
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color(if (remainedTime > 0) 0xFFF14B28 else 0xFF33DD7C)
                                )
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(expedition.avatar_side_icon).scale(Scale.FIT).build(),
                                    contentDescription = null,
                                    modifier = Modifier.wrapContentSize(unbounded = true).width(28 * cardDp).height(28 * cardDp).offset(0.dp, -4 * cardDp),
                                    contentScale = ContentScale.FillBounds
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.padding(12 * cardDp, 12 * cardDp, 0.dp, 0.dp).width(36 * cardDp).height(148 * cardDp).background(
                        color = colorScheme.boxBackgroundColor,
                        shape = RoundedCornerShape(12 * cardDp)
                    )
                ) {
                    IconButton(
                        onClick = {
                            openWeb = true
                        },
                        modifier = Modifier.size(36 * cardDp)
                    ) {
                        Icon(
                            contentDescription = null,
                            imageVector = Icons.Default.Key,
                            tint = colorScheme.foregroundColor
                        )
                    }
                }
            }
            Row (
                modifier = Modifier.fillMaxWidth().padding(18 * cardDp, 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "距离上次更新数据: ${(System.currentTimeMillis() - data.lastRefreshTime) / (60 * 60 * 1000)}小时${((System.currentTimeMillis() - data.lastRefreshTime) % (60 * 60 * 1000)) / (60 * 1000)}分钟",
                    fontFamily = genshinFont,
                    // color = Color(0xFFCDB592),
                    color = colorScheme.backgroundColorSecondary,
                    fontSize = 12 * cardDp.toSp,
                    style = TextStyle(letterSpacing = 0.sp),
                    modifier = Modifier.firstBaselineToTop(14 * cardDp)
                )
                Text(
                    text = "UID: ${data.uid}",
                    fontFamily = genshinFont,
                    color = colorScheme.backgroundColorSecondary,
                    fontSize = 12 * cardDp.toSp,
                    style = TextStyle(letterSpacing = 0.sp),
                    modifier = Modifier.firstBaselineToTop(14 * cardDp)
                )
            }
        }
    }

    if (openWeb){
        var webView: WebView? = null
        AlertDialog(
            modifier = Modifier.fillMaxSize().padding(0.dp, 10.dp),
            onDismissRequest = {},
            confirmButton = {
                Button(
                    onClick = {
                        openWeb = false
                    }
                ) { Text("关闭") }
            },
            title = { Text("详细数据页面（验证码）") },
            text = {
                Surface ( modifier = Modifier.ignoreHorizontalParentPadding(24.dp).fillMaxSize() ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            // 设置cookie
                            val cookies = data.cookie.split(";\\s*".toRegex())
                            val cookieMap = mutableMapOf<String, String>()
                            for (cookie in cookies) {
                                val temp = cookie.split("=")
                                if (temp.size < 2){
                                    continue
                                }
                                cookieMap[temp[0]] = temp[1]
                            }

                            val deviceId = getDeviceId(context)
                            runBlocking {
                                val deviceFpData = TrackingNetwork.getFp(deviceId, context)
                                cookieMap["DEVICEFP"] = deviceFpData.deviceFp
                                cookieMap["DEVICEFP_SEED_ID"] = deviceFpData.fpSeedId
                                cookieMap["DEVICEFP_SEED_TIME"] = deviceFpData.fpSeedTime
                            }

                            val cookieManager: CookieManager = CookieManager.getInstance()
                            for ((key, value) in cookieMap){
                                cookieManager.setCookie(".mihoyo.com", "$key=$value")
                            }

                            val userAgent = "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; ${Build.MODEL} Build/N2G47H; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/68.0.3440.70 Mobile Safari/537.36 miHoYoBBS/2.57.1"

                            val headers = mutableMapOf(
                                "user-agent" to userAgent,
                                "upgrade-insecure-requests" to "1",
                                "accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
                                "accept-encoding" to "gzip, deflate",
                                "accept-language" to "zh-CN,en-US;q=0.9",
                                "x-requested-with" to "com.mihoyo.hyperion"
                            )

                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                webViewClient = object: WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                    }

                                    /*override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): WebResourceResponse? {
                                        try {
                                            val httpClient = OkHttpClient()

                                            Log.i("webview_intercept", request?.url.toString())
                                            val builder = Request.Builder().url(request?.url.toString())

                                            for ((key, value) in headers) {
                                                builder.addHeader(key, value)
                                            }

                                            builder.addHeader("Cookie", cookieMap.map { (key, value) -> "$key=$value" }.joinToString("; "))

                                            val newRequest = builder.build()
                                            val response = httpClient.newCall(newRequest).execute()

                                            return WebResourceResponse("", "", response.body?.byteStream())
                                        } catch (e: Error) {
                                            e.printStackTrace()
                                        }
                                        return null
                                    }*/
                                }
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.javaScriptCanOpenWindowsAutomatically = true

                                settings.userAgentString = userAgent

                                val bbsUid = cookieMap["ltuid"]

                                val serverName = if (data.gameId == 0) { if (data.uid[0] == '5') "cn_qd01" else "cn_gf01" } else { "prod_gf_cn" }
                                this.addJavascriptInterface(MiHoYoJSInterface(context, this, cookieMap, data.uid, serverName, headers) { url: String ->
                                    loadUrl(url, headers)
                                }, "MiHoYoJSInterface")

                                loadUrl("https://webstatic.mihoyo.com/app/community-game-records/?bbs_presentation_style=fullscreen&bbs_auth_required=true&v=101&gid=2&user_id=${bbsUid}&game_id=2&uid=${bbsUid}#/ys", headers)
                                //loadUrl("https://bbs.mihoyo.com/app/community-game-records/?bbs_presentation_style=fullscreen", headers)
                                webView = this
                            }
                        },
                        update = {
                            webView = it
                        }
                    )
                }
            }
        )
    }
}

@Preview // (showSystemUi = true, showBackground = false)
@Composable
fun CardPreview(){
    HoYoLABTheme {
        Surface (
            modifier = Modifier.wrapContentSize()
        ) {

        }
    }
}

fun Modifier.circleShadow(
    color: Color,
    alpha: Float = 0.2f,
    shadowRadius: Dp = 10.dp,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp
) = this.drawBehind {
    val transparentColor = android.graphics.Color.toArgb(color.copy(alpha = 0.0f).value.toLong())
    val shadowColor = android.graphics.Color.toArgb(color.copy(alpha = alpha).value.toLong())
    this.drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparentColor
        frameworkPaint.setShadowLayer(
            shadowRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            shadowColor
        )
        it.drawOval(
            0f,
            0f,
            this.size.width,
            this.size.height,
            paint
        )
    }
}

fun Modifier.ignoreHorizontalParentPadding(horizontal: Dp): Modifier {
    return this.layout { measurable, constraints ->
        val overridenWidth = constraints.maxWidth + 2 * horizontal.roundToPx()
        val placeable = measurable.measure(constraints.copy(maxWidth = overridenWidth))
        layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}

@Composable
fun VerticalProgressIndicator (
    width: Dp = 10.dp,
    height: Dp = 50.dp,
    color: Color = Color.Black,
    progress: Float = 0.5f
) {
    Box(
        modifier = Modifier.width(width).height(height)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(height * progress).background(color).align(Alignment.BottomCenter)
        ) { }
    }
}