package com.childlearning.robot.ui.screens.emotion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

private val EMOTIONS = listOf(
    Triple("😄", "很开心", Color(0xFFFFF176)),
    Triple("😊", "开心", Color(0xFFFFF9C4)),
    Triple("😐", "一般般", Color(0xFFE0E0E0)),
    Triple("😔", "有点难过", Color(0xFFBBDEFB)),
    Triple("😢", "很难过", Color(0xFF90CAF9)),
    Triple("😠", "有点生气", Color(0xFFFFCDD2)),
    Triple("😰", "有点担心", Color(0xFFD7CCC8)),
    Triple("🤩", "超级棒", Color(0xFFFFE0B2))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionScreen(
    onBack: () -> Unit,
    viewModel: EmotionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今日心情", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF3E5F5))
            )
        },
        containerColor = Color(0xFFF3E5F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 今日心情已记录提示
            if (uiState.todayEmotion != null) {
                val today = uiState.todayEmotion!!
                val match = EMOTIONS.find { it.first == today.emoji }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = today.emoji ?: "😊", fontSize = 36.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "今天已记录心情",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF7B1FA2)
                            )
                            Text(
                                text = match?.second ?: today.label ?: "",
                                fontSize = 13.sp,
                                color = Color(0xFF9C27B0)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "现在感觉怎么样？",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A148C),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "选一个最像你现在心情的表情",
                fontSize = 13.sp,
                color = Color(0xFF9C27B0),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(EMOTIONS) { (emoji, label, bgColor) ->
                    val isSelected = uiState.selectedEmotion == emoji
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.selectEmotion(emoji) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(bgColor)
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color(0xFF7B1FA2), CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 32.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) Color(0xFF7B1FA2) else Color(0xFF666666),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveEmotion(onBack) },
                enabled = uiState.selectedEmotion != null && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("记录今天的心情 💜", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 近7天心情
            if (uiState.weeklyEmotions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "最近7天",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A148C),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    uiState.weeklyEmotions.takeLast(7).forEach { day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val score = day.avgScore ?: 3.0
                            val emoji = when {
                                score >= 4.5 -> "😄"; score >= 3.5 -> "😊"
                                score >= 2.5 -> "😐"; score >= 1.5 -> "😔"
                                else -> "😢"
                            }
                            Text(text = emoji, fontSize = 22.sp)
                            Text(
                                text = day.date.takeLast(5),
                                fontSize = 10.sp,
                                color = Color(0xFF999999)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
