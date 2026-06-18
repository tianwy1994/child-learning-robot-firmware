package com.childlearning.robot.ui.screens.challenge

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.childlearning.robot.core.network.ChallengeCard
import com.childlearning.robot.ui.components.LevelBadge

/**
 * 领域分类定义（参考硬件端 challenge.html）
 */
private data class DomainInfo(
    val key: String,
    val name: String,
    val icon: String,
    val gradient: List<Color>
)

private val domains = listOf(
    DomainInfo("ALL", "全部", "🌟", listOf(Color(0xFF6C63FF), Color(0xFF9C27B0))),
    DomainInfo("ENGLISH", "英语读写", "🔤", listOf(Color(0xFF667eea), Color(0xFF764ba2))),
    DomainInfo("SCIENCE", "科学思维", "🔬", listOf(Color(0xFF11998e), Color(0xFF38ef7d))),
    DomainInfo("VALUES", "价值观", "💝", listOf(Color(0xFFf093fb), Color(0xFFf5576c))),
    DomainInfo("LOGIC", "逻辑推理", "🧩", listOf(Color(0xFFa18cd1), Color(0xFFfbc2eb))),
    DomainInfo("ENGINEERING", "工科理工", "⚙️", listOf(Color(0xFFf6d365), Color(0xFFfda085))),
    DomainInfo("PATRIOTISM", "爱国教育", "🇨🇳", listOf(Color(0xFFff6b6b), Color(0xFFee5a24))),
    DomainInfo("COMPREHENSION", "阅读理解", "📖", listOf(Color(0xFF4facfe), Color(0xFF00f2fe))),
    DomainInfo("MATH", "数学", "🔢", listOf(Color(0xFF43e97b), Color(0xFF38f9d7))),
    DomainInfo("LIFESKILL", "生活技能", "🏠", listOf(Color(0xFFfa709a), Color(0xFFfee140))),
    DomainInfo("CREATIVITY", "创意创造", "🎨", listOf(Color(0xFFa18cd1), Color(0xFF5f27cd))),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeListScreen(
    onChallengeClick: (Long) -> Unit,
    onBack: () -> Unit = {},
    viewModel: ChallengeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val challenges by viewModel.dailyChallenges.collectAsState()
    var selectedDomain by remember { mutableStateOf("ALL") }

    LaunchedEffect(Unit) {
        viewModel.loadDailyChallenges()
    }

    // 筛选后的题目
    val filteredChallenges = if (selectedDomain == "ALL") challenges
    else challenges.filter { it.domainKey == selectedDomain }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("魔法挑战乐园") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6C63FF),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FE))
        ) {
            // ===== 领域分类 Tab =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF6C63FF))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                domains.forEach { domain ->
                    val isSelected = selectedDomain == domain.key
                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                        animationSpec = tween(200), label = "tabBg"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) domain.gradient[0] else Color.White,
                        animationSpec = tween(200), label = "tabText"
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bgColor)
                            .clickable { selectedDomain = domain.key }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = domain.icon, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = domain.name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }

            // ===== 题目列表 =====
            when (uiState) {
                is ChallengeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF6C63FF))
                    }
                }
                is ChallengeUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "😢 ${(uiState as ChallengeUiState.Error).message}",
                            color = Color(0xFF999999),
                            fontSize = 15.sp
                        )
                    }
                }
                is ChallengeUiState.Success -> {
                    if (filteredChallenges.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "📚", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (selectedDomain == "ALL") "暂无题目" else "该领域暂无题目",
                                    color = Color(0xFF999999),
                                    fontSize = 15.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
                        ) {
                            items(filteredChallenges, key = { it.id }) { challenge ->
                                ChallengeItem(
                                    challenge = challenge,
                                    onClick = { onChallengeClick(challenge.id) }
                                )
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ChallengeItem(
    challenge: ChallengeCard,
    onClick: () -> Unit
) {
    val domainColor = getDomainColor(challenge.domainKey)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 领域图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(getDomainGradient(challenge.domainKey))),
                contentAlignment = Alignment.Center
            ) {
                Text(text = challenge.domainIcon, fontSize = 24.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = challenge.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A2E),
                    maxLines = 1
                )

                Text(
                    text = challenge.description,
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    LevelBadge(text = challenge.domainName, color = domainColor)
                    LevelBadge(text = "⭐".repeat(challenge.difficulty), color = Color(0xFFFF9800))
                    LevelBadge(text = "+${challenge.expReward}", color = Color(0xFF10b981))
                    if (challenge.completed) {
                        LevelBadge(text = "✅", color = Color(0xFF10b981))
                    }
                }
            }
        }
    }
}

fun getDomainColor(domainKey: String): Color {
    return when (domainKey) {
        "ENGLISH" -> Color(0xFF3b82f6)
        "SCIENCE" -> Color(0xFF10b981)
        "VALUES" -> Color(0xFFec4899)
        "LOGIC" -> Color(0xFF8b5cf6)
        "ENGINEERING" -> Color(0xFFf59e0b)
        "PATRIOTISM" -> Color(0xFFef4444)
        "COMPREHENSION" -> Color(0xFF06b6d4)
        "MATH" -> Color(0xFF84cc16)
        "LIFESKILL" -> Color(0xFFf97316)
        "CREATIVITY" -> Color(0xFFa855f7)
        else -> Color(0xFF6366f1)
    }
}

private fun getDomainGradient(domainKey: String): List<Color> {
    return domains.find { it.key == domainKey }?.gradient
        ?: listOf(Color(0xFF6C63FF), Color(0xFF9C27B0))
}
