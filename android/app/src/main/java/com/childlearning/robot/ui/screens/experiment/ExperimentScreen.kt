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
import com.childlearning.robot.core.audio.TtsPlayer
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

data class ExpCategory(val name: String, val icon: String, val experiments: List<Experiment>)
data class Experiment(val id: String, val title: String, val desc: String, val difficulty: Int, val voiceIntro: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentScreen(onBack: () -> Unit, ttsPlayer: TtsPlayer? = null) {
    val context = LocalContext.current
    var categories by remember { mutableStateOf<List<ExpCategory>>(emptyList()) }
    var selectedExp by remember { mutableStateOf<Experiment?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { categories = loadCatalog(context) }

    if (selectedExp != null) {
        ExperimentWebView(
            experiment = selectedExp!!,
            context = context,
            ttsPlayer = ttsPlayer,
            onBack = { selectedExp = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("科学实验站", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = { IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }},
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0f3460))
                )
            },
            containerColor = Color(0xFF1a1a2e)
        ) { padding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.forEach { cat ->
                    item {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.icon, fontSize = 22.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(cat.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8DFF5))
                            Spacer(Modifier.width(8.dp))
                            Text("${cat.experiments.size}个实验", fontSize = 13.sp, color = Color(0xFF8888AA))
                        }
                    }
                    items(cat.experiments) { exp ->
                        val isEnglish = exp.title.contains("（英文）")
                        Card(
                            onClick = { selectedExp = exp },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isEnglish) Color(0xFF1A1A2E) else Color(0xFF16213E)),
                            border = BorderStroke(1.dp, if (isEnglish) Color(0xFF333355) else Color(0xFF2A2A5E))
                        ) {
                            Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(exp.title.removeSuffix("（英文）"), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFE8DFF5))
                                        if (isEnglish) {
                                            Spacer(Modifier.width(6.dp))
                                            Text("EN", fontSize = 10.sp, color = Color(0xFF8888AA),
                                                modifier = Modifier.background(Color(0xFF2A2A5E), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text(exp.desc, fontSize = 12.sp, color = Color(0xFF8888AA), modifier = Modifier.padding(top = 4.dp))
                                }
                                Text("⭐".repeat(exp.difficulty), fontSize = 12.sp)
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

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperimentWebView(
    experiment: Experiment, context: Context,
    ttsPlayer: TtsPlayer? = null, onBack: () -> Unit
) {
    var progress by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf(false) }
    var errMsg by remember { mutableStateOf<String?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isEnglish = experiment.title.contains("（英文）")

    val baseUrl = "https://phet.colorado.edu/sims/html/${experiment.id}/latest/"
    val lang = if (isEnglish) "en" else "zh_CN"
    val localFile = "file:///android_asset/experiments/phet/${experiment.id}/${experiment.id}_${lang}.html"
    val remoteUrl = "${baseUrl}${experiment.id}_${lang}.html"
    val urlToLoad = localFile

    // Parse voice intro sections
    val voiceSections = remember(experiment) {
        parseVoiceSections(experiment.voiceIntro)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(experiment.title.removeSuffix("（英文）"), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }},
                actions = {
                    if (isEnglish) Text("EN", fontSize = 11.sp, color = Color(0xFF8888AA),
                        modifier = Modifier.background(Color(0xFF2A2A5E), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0f3460))
            )
        },
        containerColor = Color(0xFF1a1a2e)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Voice buttons
            if (voiceSections.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VoiceChip("🔊 全部", isSpeaking) {
                        speakAll(experiment, voiceSections, ttsPlayer, scope) { isSpeaking = it }
                    }
                    voiceSections.forEachIndexed { i, (icon, _, _) ->
                        VoiceChip(icon, isSpeaking) {
                            speakSection(voiceSections, i, ttsPlayer, scope) { isSpeaking = it }
                        }
                    }
                    if (isSpeaking) {
                        VoiceChip("⏹ 停止", false) {
                            ttsPlayer?.stop()
                            isSpeaking = false
                        }
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = false
                            settings.displayZoomControls = false
                            settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
                            settings.databaseEnabled = true

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, p: Int) { progress = p; if (p >= 100) loaded = true }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) { loaded = false; errMsg = null }
                                override fun onReceivedError(view: WebView?, code: Int, desc: String, url: String?) {
                                    if (url?.startsWith("file://") == true) view?.loadUrl(remoteUrl)
                                    else { errMsg = "加载失败($code)"; loaded = true }
                                }
                            }
                            loadUrl(urlToLoad)
                        }
                    }
                )

                if (!loaded && errMsg == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(progress = { progress / 100f }, color = Color(0xFF6366F1), modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("加载中 $progress%", color = Color(0xFF8888AA), fontSize = 14.sp)
                        }
                    }
                }

                if (errMsg != null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Text("⚠️", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("加载失败", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8DFF5))
                            Spacer(Modifier.height(8.dp))
                            Text(errMsg ?: "", fontSize = 13.sp, color = Color(0xFF8888AA), textAlign = TextAlign.Center)
                            Spacer(Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { errMsg = null; loaded = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))) { Text("重试") }
                                OutlinedButton(onClick = { errMsg = null; loaded = false },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8888AA))) { Text("在线加载") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceChip(label: String, isActive: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (isActive) Color(0xFF6EE7B7) else Color(0xFFA5B4FC)
        ),
        modifier = Modifier
            .background(
                if (isActive) Color(0x3310B981) else Color(0x332A2A5E),
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, if (isActive) Color(0x4410B981) else Color(0x333636F1), RoundedCornerShape(20.dp))
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// Voice section parsing
private data class VoiceSection(val icon: String, val label: String, val text: String)

private fun parseVoiceSections(voiceIntro: String?): List<VoiceSection> {
    if (voiceIntro.isNullOrBlank()) return emptyList()
    val sections = mutableListOf<VoiceSection>()
    val lines = voiceIntro.split("\n").map { it.trim() }.filter { it.isNotBlank() }
    for (line in lines) {
        val icon = when {
            line.startsWith("🔬") -> "🔬"
            line.startsWith("📋") -> "📋"
            line.startsWith("✨") -> "✨"
            line.startsWith("🎯") -> "🎯"
            line.startsWith("🏠") -> "🏠"
            else -> "📖"
        }
        val label = when (icon) {
            "🔬" -> "背景"
            "📋" -> "步骤"
            "✨" -> "效果"
            "🎯" -> "学到"
            "🏠" -> "生活"
            else -> "讲解"
        }
        val text = line.replace(Regex("^[🔬📋✨🎯🏠]"), "").trim()
        sections.add(VoiceSection(icon, label, text))
    }
    return sections
}

private suspend fun speakText(ttsPlayer: TtsPlayer?, text: String, onState: (Boolean) -> Unit) {
    if (text.isBlank()) return
    onState(true)
    try {
        ttsPlayer?.speak(text)
    } catch (_: Exception) {}
    onState(false)
}

private fun speakAll(
    exp: Experiment, sections: List<VoiceSection>,
    ttsPlayer: TtsPlayer?, scope: kotlinx.coroutines.CoroutineScope,
    onState: (Boolean) -> Unit
) {
    val fullText = sections.joinToString("。") { it.text }
    scope.launch { speakText(ttsPlayer, fullText, onState) }
}

private fun speakSection(
    sections: List<VoiceSection>, idx: Int,
    ttsPlayer: TtsPlayer?, scope: kotlinx.coroutines.CoroutineScope,
    onState: (Boolean) -> Unit
) {
    if (idx < sections.size) {
        scope.launch { speakText(ttsPlayer, sections[idx].text, onState) }
    }
}

// Load catalog
private fun loadCatalog(context: Context): List<ExpCategory> {
    return try {
        val json = context.assets.open("experiments/catalog.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val arr = root.getJSONArray("categories")
        (0 until arr.length()).map { i ->
            val cat = arr.getJSONObject(i)
            val exps = cat.getJSONArray("experiments")
            ExpCategory(
                name = cat.getString("name"),
                icon = cat.getString("icon"),
                experiments = (0 until exps.length()).map { j ->
                    val e = exps.getJSONObject(j)
                    Experiment(
                        id = e.getString("id"),
                        title = e.getString("title"),
                        desc = e.getString("desc"),
                        difficulty = e.getInt("difficulty"),
                        voiceIntro = e.optString("voiceIntro", null)
                    )
                }
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}
