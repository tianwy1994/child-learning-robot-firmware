package com.childlearning.robot.ui.screens.experiment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.childlearning.robot.core.storage.TokenStore
import kotlinx.coroutines.flow.first

/**
 * 科学实验站
 *
 * 在 App 内 WebView 中加载远程 experiments-mobile 页面，
 * 不拉起系统浏览器，自动传递 token 实现免登录。
 * 所有实验列表和交互由服务端的 experiments-mobile 页面提供。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 拦截系统返回键 → 直接返回首页
    BackHandler(onBack = onBack)

    // 等待 token 加载完成后再创建 WebView
    var tokenReady by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        try {
            val store = TokenStore(context)
            val t = store.tokenFlow.first()
            url = "http://192.168.31.117:8080/experiments-mobile/?token=$t"
        } catch (_: Exception) {
            url = "http://192.168.31.117:8080/experiments-mobile/"
        }
        tokenReady = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("小智科学实验室", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("星空下的科学探索", fontSize = 10.sp, color = Color(0xFF8888AA))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // 如果 WebView 可以回退，先回退
                        if (webViewRef?.canGoBack() == true) {
                            webViewRef?.goBack()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF05080f))
            )
        },
        containerColor = Color(0xFF05080f)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // token 未就绪时显示 loading
            if (!tokenReady) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFF818cf8),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("加载中…", color = Color(0xFF8888AA), fontSize = 14.sp)
                    }
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.allowFileAccess = false
                        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        settings.databaseEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, p: Int) {
                                progress = p
                                if (p >= 100) loaded = true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loaded = false
                                error = null
                            }

                            override fun onReceivedError(
                                view: WebView?, code: Int,
                                desc: String, url: String?
                            ) {
                                error = "加载失败 ($code)"
                                loaded = true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                // 所有导航都在当前 WebView 内打开，不拉起系统浏览器
                                request?.url?.let { view?.loadUrl(it.toString()) }
                                return true
                            }
                        }

                        webViewRef = this
                        loadUrl(url)
                    }
                }
            )
            }

            // 加载指示器
            if (!loaded && error == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = { progress / 100f },
                            color = Color(0xFF818cf8),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "加载中 $progress%",
                            color = Color(0xFF8888AA),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // 错误提示
            if (error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "加载失败",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE8DFF5)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            error ?: "",
                            fontSize = 13.sp,
                            color = Color(0xFF8888AA),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "请检查网络连接后重试",
                            fontSize = 12.sp,
                            color = Color(0xFF555577),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                error = null
                                loaded = false
                                webViewRef?.loadUrl(url)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF818cf8))
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}
