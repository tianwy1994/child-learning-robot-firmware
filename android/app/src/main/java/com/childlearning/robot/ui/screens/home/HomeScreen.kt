package com.childlearning.robot.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.childlearning.robot.ui.theme.*

/**
 * 首页 — 作业帮学习机风格
 *
 * 布局：
 * - 顶部渐变欢迎区（机器人头像 + 问候语 + 等级信息）
 * - 功能卡片网格（渐变背景 + 大 emoji + 动画）
 * - 底部设备状态栏
 */
@Composable
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToGame: () -> Unit,
    onNavigateToHomework: () -> Unit,
    onNavigateToChallenge: () -> Unit,
    onNavigateToBind: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ===== 顶部渐变欢迎区 =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6C63FF),
                                Color(0xFF9C27B0),
                                Color(0xFFF8F9FE)
                            )
                        )
                    )
                    .padding(top = 48.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    // 顶部栏
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = getGreeting(),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "小智同学 🌟",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 设置按钮
                        IconButton(
                            onClick = onNavigateToBind,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "设置",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 等级卡片
                    uiState.profile?.let { profile ->
                        LevelCard(
                            level = profile.level,
                            experience = profile.experience,
                            streakDays = profile.streakDays,
                            modifier = Modifier.graphicsLayer(scaleX = breathScale, scaleY = breathScale)
                        )
                    } ?: run {
                        // 未登录时显示默认卡片
                        DefaultLevelCard()
                    }

                    // 签到提示
                    uiState.checkinMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.9f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "🎉", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = msg,
                                    fontSize = 14.sp,
                                    color = Color(0xFF6C63FF),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ===== 功能区域 =====
            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                // 主要功能标题
                Text(
                    text = "✨ 学习乐园",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E),
                    modifier = Modifier.padding(bottom = 16.dp, top = 4.dp)
                )

                // 主功能卡片 — 2列大卡片
                val mainFeatures = listOf(
                    FeatureItem(
                        emoji = "🤖",
                        title = "AI 聊天",
                        subtitle = "和小智聊聊天",
                        gradient = GradientPurple,
                        onClick = onNavigateToChat
                    ),
                    FeatureItem(
                        emoji = "📸",
                        title = "作业帮",
                        subtitle = "拍照批改",
                        gradient = GradientBlue,
                        onClick = onNavigateToHomework
                    ),
                    FeatureItem(
                        emoji = "🎯",
                        title = "专注学习",
                        subtitle = "静心专注",
                        gradient = GradientGreen,
                        onClick = onNavigateToFocus
                    ),
                    FeatureItem(
                        emoji = "🧩",
                        title = "趣味挑战",
                        subtitle = "闯关赢星星",
                        gradient = GradientOrange,
                        onClick = onNavigateToChallenge
                    ),
                )

                mainFeatures.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        rowItems.forEach { feature ->
                            MainFeatureCard(
                                feature = feature,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // 如果是奇数，补一个空位
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 次要功能标题
                Text(
                    text = "🌈 更多精彩",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E),
                    modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
                )

                // 次要功能 — 横向小卡片
                val subFeatures = listOf(
                    FeatureItem(
                        emoji = "🏆",
                        title = "积分中心",
                        subtitle = "查看成就",
                        gradient = GradientCyan,
                        onClick = onNavigateToGame
                    ),
                )

                subFeatures.forEach { feature ->
                    SubFeatureCard(feature = feature)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ========== 等级卡片 ==========
@Composable
private fun LevelCard(
    level: Int,
    experience: Int,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 等级
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "⭐", fontSize = 28.sp)
                Text(
                    text = "Lv.$level",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6C63FF)
                )
            }

            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFEEE8FF)))

            // 经验
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "💎", fontSize = 28.sp)
                Text(
                    text = "$experience",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )
                Text(text = "经验", fontSize = 12.sp, color = Color(0xFF999999))
            }

            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFEEE8FF)))

            // 签到
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🔥", fontSize = 28.sp)
                Text(
                    text = "${streakDays}天",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B9D)
                )
                Text(text = "连续签到", fontSize = 12.sp, color = Color(0xFF999999))
            }
        }
    }
}

// ========== 未登录默认卡片 ==========
@Composable
private fun DefaultLevelCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = "👋", fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "欢迎来到小智同学！",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E)
                )
                Text(
                    text = "绑定设备后解锁全部功能",
                    fontSize = 13.sp,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}

// ========== 主功能大卡片 ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainFeatureCard(
    feature: FeatureItem,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )

    Card(
        onClick = {
            isPressed = true
            feature.onClick()
        },
        modifier = modifier
            .height(140.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(feature.gradient))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = feature.emoji,
                    fontSize = 36.sp
                )
                Column {
                    Text(
                        text = feature.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = feature.subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

// ========== 次要功能横卡 ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubFeatureCard(feature: FeatureItem) {
    Card(
        onClick = feature.onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 渐变图标背景
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(feature.gradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = feature.emoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A2E)
                )
                Text(
                    text = feature.subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ========== 数据类 ==========
private data class FeatureItem(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val gradient: List<Color>,
    val onClick: () -> Unit
)

// ========== 工具函数 ==========
private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 6 -> "🌙 夜深了，注意休息"
        hour < 9 -> "🌅 早上好"
        hour < 12 -> "☀️ 上午好"
        hour < 14 -> "🌞 中午好"
        hour < 18 -> "🌤️ 下午好"
        hour < 21 -> "🌆 傍晚好"
        else -> "🌙 晚上好"
    }
}
