package com.childlearning.robot.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.childlearning.robot.domain.enums.AppState
import com.childlearning.robot.ui.components.ChatBubble
import com.childlearning.robot.ui.components.VoiceButton
import com.childlearning.robot.ui.theme.*

/**
 * 语音交互界面
 * 对应固件 STATE_LISTENING → STATE_PROCESSING → STATE_SPEAKING
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    onBack: () -> Unit,
    viewModel: VoiceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // 录音权限
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRecordPermission = granted
        if (!granted) {
            Toast.makeText(context, "需要录音权限才能使用语音功能", Toast.LENGTH_SHORT).show()
        }
    }

    // 自动滚动
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 状态颜色
    val statusColor by animateColorAsState(
        targetValue = when (uiState.appState) {
            is AppState.Listening -> Error
            is AppState.Processing -> Warning
            is AppState.Speaking -> Secondary
            is AppState.Error -> Error
            else -> Primary
        },
        label = "statusColor"
    )

    val statusText = when (uiState.appState) {
        is AppState.Listening -> "🎙️ 正在听你说话..."
        is AppState.Processing -> "🧠 思考中..."
        is AppState.Speaking -> "🔊 播放中..."
        is AppState.Error -> "❌ 出错了"
        else -> "🎤 按住按钮开始说话"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音对话") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 状态指示条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(statusColor)
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = statusColor,
                modifier = Modifier.padding(8.dp)
            )

            // 对话消息列表
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message = message)
                }
            }

            // 错误提示
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 语音按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                VoiceButton(
                    isRecording = uiState.appState is AppState.Listening,
                    isProcessing = uiState.appState is AppState.Processing,
                    onRecordStart = {
                        if (hasRecordPermission) {
                            viewModel.startRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onRecordStop = {
                        if (hasRecordPermission) {
                            viewModel.stopRecordingAndProcess()
                        }
                    }
                )
            }
        }
    }
}
