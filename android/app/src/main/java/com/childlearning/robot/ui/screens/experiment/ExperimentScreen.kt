package com.childlearning.robot.ui.screens.experiment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.json.JSONObject

/**
 * 实验数据类 — 对应 simulator registry 中的 experimentMeta
 */
data class SimExperiment(
    val id: String,       // 实验 ID，如 "bending-light"
    val subject: String,  // 学科 key，如 "physics"
    val title: String,    // 标题
    val icon: String,     // Emoji 图标
    val done: Boolean     // 是否已完成开发
)

data class SimCategory(
    val name: String,
    val icon: String,
    val experiments: List<SimExperiment>
)

/** 本项目中实验的布局模式：列表 / WebView */
private enum class ExpMode { List, Detail }

/**
 * 科学实验站
 *
 * 从服务器加载 [child-learning-simulator] 构建的 70+ 交互实验。
 * 点击实验卡片 → WebView 加载对应实验页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(ExpMode.List) }
    var categories by remember { mutableStateOf<List<SimCategory>>(emptyList()) }
    var selectedExp by remember { mutableStateOf<SimExperiment?>(null) }
    val listState = rememberLazyListState()

    // 加载目录
    LaunchedEffect(Unit) {
        categories = loadCatalog(context)
    }

    // ── 详情模式 ──
    if (mode == ExpMode.Detail && selectedExp != null) {
        ExperimentWebView(
            experiment = selectedExp!!,
            onBack = {
                selectedExp = null
                mode = ExpMode.List
            }
        )
        return
    }

    // ── 列表模式 ──
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("科学实验站", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("小智仿真实验室", fontSize = 11.sp, color = Color(0xFF8888AA))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0f3460))
            )
        },
        containerColor = Color(0xFF1a1a2e)
    ) { padding ->
        if (categories.isEmpty()) {
            // 空状态
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔬", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("加载实验目录中…", color = Color(0xFF8888AA), fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator(
                        color = Color(0xFF6366F1),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 欢迎说明
                item {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "共 ${categories.sumOf { it.experiments.size }} 个交互实验",
                        fontSize = 13.sp,
                        color = Color(0xFF8888AA),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                categories.forEach { cat ->
                    item {
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.icon, fontSize = 22.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(cat.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8DFF5))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${cat.experiments.size} 个实验",
                                fontSize = 13.sp,
                                color = Color(0xFF8888AA)
                            )
                        }
                    }

                    items(cat.experiments) { exp ->
                        Card(
                            onClick = {
                                selectedExp = exp
                                mode = ExpMode.Detail
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (exp.done) Color(0xFF16213E) else Color(0xFF1A1A2E)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (exp.done) Color(0xFF2A2A5E) else Color(0xFF222244)
                            )
                        ) {
                            Row(
                                Modifier.padding(14.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 实验图标
                                Text(exp.icon, fontSize = 26.sp, modifier = Modifier.padding(end = 12.dp))

                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            exp.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFFE8DFF5)
                                        )
                                        if (!exp.done) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "开发中",
                                                fontSize = 10.sp,
                                                color = Color(0xFF8888AA),
                                                modifier = Modifier
                                                    .background(Color(0xFF2A2A5E), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        exp.subject.let { s ->
                                            when (s) {
                                                "math" -> "数学"
                                                "physics" -> "物理"
                                                "chemistry" -> "化学"
                                                "biology" -> "生物"
                                                "earth-science" -> "地球科学"
                                                else -> s
                                            }
                                        },
                                        fontSize = 11.sp,
                                        color = Color(0xFF555577),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Spacer(Modifier.width(8.dp))
                                Text("▶", fontSize = 20.sp, color = Color(0xFF6366F1))
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════
//  WebView 实验详情
// ═══════════════════════════════════════════

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperimentWebView(
    experiment: SimExperiment,
    onBack: () -> Unit
) {
    var progress by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf(false) }
    var errMsg by remember { mutableStateOf<String?>(null) }

    // 构建本地文件 URL
    // 格式: file:///android_asset/experiments/index.html#/experiment/{subject}/{id}
    val simulatorUrl = "file:///android_asset/experiments/index.html#/experiment/${experiment.subject}/${experiment.id}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(experiment.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("小智仿真实验室", fontSize = 10.sp, color = Color(0xFF8888AA))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0f3460))
            )
        },
        containerColor = Color(0xFF1a1a2e)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        // 启用触摸缩放（方便查看细节）
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        // 本地文件访问权限
                        settings.allowFileAccess = true
                        settings.allowFileAccessFromFileURLs = true
                        settings.allowUniversalAccessFromFileURLs = true
                        // 默认缓存策略
                        settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                        settings.databaseEnabled = true
                        settings.loadsImagesAutomatically = true

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, p: Int) {
                                progress = p
                                if (p >= 100) loaded = true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loaded = false
                                errMsg = null
                            }

                            override fun onReceivedError(
                                view: WebView?, code: Int,
                                desc: String, url: String?
                            ) {
                                errMsg = "加载失败 ($code): $desc"
                                loaded = true
                            }
                        }

                        loadUrl(simulatorUrl)
                    }
                }
            )

            // 加载指示器
            if (!loaded && errMsg == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = { progress / 100f },
                            color = Color(0xFF6366F1),
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
            if (errMsg != null) {
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
                            errMsg ?: "",
                            fontSize = 13.sp,
                            color = Color(0xFF8888AA),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "请检查实验资源是否完整",
                            fontSize = 12.sp,
                            color = Color(0xFF555577),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                errMsg = null
                                loaded = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
//  目录加载
// ═══════════════════════════════════════════

private fun loadCatalog(context: Context): List<SimCategory> {
    return try {
        val json = context.assets.open("experiments/catalog.json")
            .bufferedReader()
            .use { it.readText() }
        val root = JSONObject(json)
        val arr = root.getJSONArray("categories")
        (0 until arr.length()).map { i ->
            val cat = arr.getJSONObject(i)
            val exps = cat.getJSONArray("experiments")
            SimCategory(
                name = cat.getString("name"),
                icon = cat.getString("icon"),
                experiments = (0 until exps.length()).map { j ->
                    val e = exps.getJSONObject(j)
                    SimExperiment(
                        id = e.getString("id"),
                        subject = e.getString("subject"),
                        title = e.getString("title"),
                        icon = e.getString("icon"),
                        done = e.optBoolean("done", false)
                    )
                }
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}
