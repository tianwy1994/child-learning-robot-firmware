package com.childlearning.robot.ui.screens.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.childlearning.robot.ui.components.LevelBadge

/**
 * 签到/积分界面
 * 对应固件 GameManager — 每日签到和积分展示
 *
 * 固件功能：
 * - WiFi 连接后自动签到 (20 小时防重复)
 * - 签到成功播放 encourage 预设语音
 * - 语音播报等级信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    onBack: () -> Unit,
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("签到积分") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadProfile() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 等级信息
            uiState.profile?.let { profile ->
                LevelBadge(
                    level = profile.level,
                    experience = profile.experience,
                    streakDays = profile.streakDays
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 签到按钮
            Button(
                onClick = { viewModel.checkin() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isLoading
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("每日签到", style = MaterialTheme.typography.titleMedium)
            }

            // 签到结果
            if (uiState.checkinMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.checkinMessage!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 等级说明
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "等级说明",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buildString {
                            appendLine("🌱 Lv.1 学习小萌新 (0 XP)")
                            appendLine("🔍 Lv.2 知识探索者 (100 XP)")
                            appendLine("🛠️ Lv.3 习惯养成师 (300 XP)")
                            appendLine("⭐ Lv.4 学霸小达人 (600 XP)")
                            appendLine("🎓 Lv.5 学习小导师 (1000 XP)")
                            appendLine("⚔️ Lv.6 智慧勇士 (1500 XP)")
                            appendLine("🧙 Lv.7 知识魔法师 (2500 XP)")
                            appendLine("🦸 Lv.8 学习小超人 (4000 XP)")
                            appendLine("🚀 Lv.9 星际探险家 (6000 XP)")
                            append("👑 Lv.10 传说学霸 (10000 XP)")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
}
