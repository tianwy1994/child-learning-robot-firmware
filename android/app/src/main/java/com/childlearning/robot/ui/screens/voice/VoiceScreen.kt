package com.childlearning.robot.ui.screens.voice

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.childlearning.robot.domain.enums.AppState
import com.childlearning.robot.ui.components.ChatBubble
import com.childlearning.robot.ui.components.VoiceButton
import com.childlearning.robot.ui.theme.*

/**
 * 语音交互界面
 * 对应固件 STATE_LISTENING → STATE_PROCESSING → STATE_SPEAKING
 *
 * 固件用物理按键（长按录音，松手发送）
 * Android 改为触摸长按按钮
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

    // 自动滚动
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 状态颜色 — 对应固件 DisplayManager 的 LED 颜色
    val statusColor by animateColorAsState(
        targetValue = when (uiState.appState) {
            is AppState.Listening -> Error          // 红色 — 录音中
            is AppState.Processing -> Warning       // 黄色 — 处理中
            is AppState.Speaking -> Secondary       // 蓝色 — 播放中
            is AppState.Error -> Error
            else -> Primary                         // 绿色 — 空闲
        },
        label = "statusColor"
    )

    val statusText = when (uiState.appState) {
        is AppState.Listening -> "正在听..."
        is AppState.Processing -> "思考中..."
        is AppState.Speaking -> "播放中..."
        is AppState.Error -> "出错了"
        else -> "准备好了"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音对话") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
            // 状态指示条 — 对应固件 RGB LED
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

            // 语音按钮 — 对应固件 ButtonManager 的长按录音
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                VoiceButton(
                    isRecording = uiState.appState is AppState.Listening,
                    isProcessing = uiState.appState is AppState.Processing,
                    onRecordStart = { viewModel.startRecording() },
                    onRecordStop = { viewModel.stopRecordingAndProcess() }
                )
            }
        }
    }
}
