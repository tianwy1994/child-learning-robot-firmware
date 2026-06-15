package com.childlearning.robot.ui.screens.focus

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.childlearning.robot.ui.theme.*

/**
 * 专注学习界面
 * 对应固件 STATE_FOCUSING / STATE_FOCUS_BREAK
 *
 * 固件功能：
 * - 短按开始/结束专注
 * - 每 30 秒轮询服务端状态
 * - 收到 POSTURE/BREAK/CONTINUE 提醒时播放预设语音
 * - BREAK → STATE_FOCUS_BREAK, CONTINUE → STATE_FOCUSING
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    onBack: () -> Unit,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var taskDescription by remember { mutableStateOf("") }
    var showStartDialog by remember { mutableStateOf(false) }

    val timerColor by animateColorAsState(
        targetValue = when {
            uiState.isBreak -> FocusBreakOrange
            uiState.isFocusing -> FocusGreen
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "timerColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("专注学习") },
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
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!uiState.isFocusing) {
                // 未开始专注 — 显示开始按钮
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "准备开始专注学习",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { showStartDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始专注", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                // 专注中 — 显示计时器和状态
                Text(
                    text = if (uiState.isBreak) "休息中" else "专注中",
                    style = MaterialTheme.typography.headlineSmall,
                    color = timerColor
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 计时器圆圈
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(timerColor.copy(alpha = 0.1f))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatTime(uiState.elapsedTimeMs),
                            style = MaterialTheme.typography.displayMedium,
                            color = timerColor
                        )
                        Text(
                            text = "已专注",
                            style = MaterialTheme.typography.bodySmall,
                            color = timerColor
                        )
                    }
                }

                // 提醒信息
                if (uiState.lastReminder != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.lastReminder!!,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 结束按钮
                Button(
                    onClick = { viewModel.endFocus() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !uiState.isLoading
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("结束专注", style = MaterialTheme.typography.titleMedium)
                }
            }

            // 错误提示
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // 开始专注对话框
    if (showStartDialog) {
        AlertDialog(
            onDismissRequest = { showStartDialog = false },
            title = { Text("开始专注") },
            text = {
                OutlinedTextField(
                    value = taskDescription,
                    onValueChange = { taskDescription = it },
                    label = { Text("学习任务描述") },
                    placeholder = { Text("例如：完成数学作业") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStartDialog = false
                        viewModel.startFocus(taskDescription.ifBlank { "专注学习" })
                        taskDescription = ""
                    }
                ) {
                    Text("开始")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
