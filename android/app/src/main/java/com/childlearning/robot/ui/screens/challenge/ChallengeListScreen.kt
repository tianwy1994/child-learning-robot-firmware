package com.childlearning.robot.ui.screens.challenge

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import com.childlearning.robot.core.network.ChallengeDetailResponse
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
    val gradient: List<Color>,
    val glow: Color
)

private val domains = listOf(
    DomainInfo("ENGLISH", "语言星云", "🔤", listOf(Color(0xFF667eea), Color(0xFF764ba2)), Color(0xFF667eea)),
    DomainInfo("SCIENCE", "科学实验室", "🔬", listOf(Color(0xFF11998e), Color(0xFF38ef7d)), Color(0xFF11998e)),
    DomainInfo("VALUES", "品德之心", "💝", listOf(Color(0xFFf093fb), Color(0xFFf5576c)), Color(0xFFf093fb)),
    DomainInfo("LOGIC", "逻辑迷宫", "🧩", listOf(Color(0xFFa18cd1), Color(0xFFfbc2eb)), Color(0xFFa18cd1)),
    DomainInfo("ENGINEERING", "工程基地", "⚙️", listOf(Color(0xFFf6d365), Color(0xFFfda085)), Color(0xFFf6d365)),
    DomainInfo("PATRIOTISM", "红旗星", "🇨🇳", listOf(Color(0xFFff6b6b), Color(0xFFee5a24)), Color(0xFFff6b6b)),
    DomainInfo("COMPREHENSION", "知识图书馆", "📖", listOf(Color(0xFF4facfe), Color(0xFF00f2fe)), Color(0xFF4facfe)),
    DomainInfo("MATH", "数字星球", "🔢", listOf(Color(0xFF43e97b), Color(0xFF38f9d7)), Color(0xFF43e97b)),
    DomainInfo("LIFESKILL", "生活花园", "🏠", listOf(Color(0xFFfa709a), Color(0xFFfee140)), Color(0xFFfa709a)),
    DomainInfo("CREATIVITY", "创意工坊", "🎨", listOf(Color(0xFFa18cd1), Color(0xFF5f27cd)), Color(0xFFa18cd1)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeListScreen(
    onChallengeClick: (Long, ChallengeDetailResponse?) -> Unit,
    onBack: () -> Unit = {},
    viewModel: ChallengeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dailyChallenges by viewModel.dailyChallenges.collectAsState()
    val bankQuestions by viewModel.bankQuestions.collectAsState()
    val selectedDomain by viewModel.selectedDomain.collectAsState()
    var isBankMode by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // 首次进入时加载当前选中的领域（默认 ENGLISH）
        viewModel.loadBankQuestions(selectedDomain)
    }

    // 当前展示的题目
    val displayChallenges = if (isBankMode) bankQuestions else dailyChallenges

    // Space exploration dark theme colors
    val bgDeep = Color(0xFF0a0e1a)
    val bgSurface = Color(0xFF111827)
    val cardBg = Color(0xFF1e293b)
    val textPrimary = Color(0xFFe8ecf4)
    val textSecondary = Color(0xFF8892b0)
    val accentPrimary = Color(0xFF6366f1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "星际探索",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "穿越知识星系，点亮每一颗星球",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgDeep,
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
                .background(bgDeep)
        ) {
            // ===== Planet Belt (领域星球带) =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgDeep)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                domains.forEach { domain ->
                    val isSelected = selectedDomain == domain.key
                    val sphereAlpha by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.6f,
                        animationSpec = tween(200), label = "sphereAlpha"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) Color.White else textSecondary,
                        animationSpec = tween(200), label = "planetText"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                isBankMode = true
                                viewModel.loadBankQuestions(domain.key)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        // Planet sphere
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.linearGradient(
                                        domain.gradient.map { it.copy(alpha = sphereAlpha) }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = domain.icon, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = domain.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1
                        )
                    }
                }
            }

            // ===== 题目列表 =====
            when (uiState) {
                is ChallengeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentPrimary)
                    }
                }
                is ChallengeUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "😢 ${(uiState as ChallengeUiState.Error).message}",
                            color = textSecondary,
                            fontSize = 15.sp
                        )
                    }
                }
                is ChallengeUiState.Success -> {
                    if (displayChallenges.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "🌌", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "该星球暂无探索题目",
                                    color = textSecondary,
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
                            if (isBankMode) {
                                items(bankQuestions, key = { it.id }) { q ->
                                    BankQuestionItem(
                                        question = q,
                                        onClick = { onChallengeClick(q.id, q) }
                                    )
                                }
                            } else {
                                items(dailyChallenges, key = { it.id }) { challenge ->
                                    ChallengeItem(
                                        challenge = challenge,
                                        onClick = { onChallengeClick(challenge.id, null) }
                                    )
                                }
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1e293b)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Domain icon with gradient
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
                    color = Color(0xFFe8ecf4),
                    maxLines = 1
                )

                Text(
                    text = challenge.description,
                    fontSize = 12.sp,
                    color = Color(0xFF8892b0),
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

/**
 * 题库题目卡片（ChallengeDetailResponse 格式）
 */
@Composable
fun BankQuestionItem(
    question: ChallengeDetailResponse,
    onClick: () -> Unit
) {
    val domainColor = getDomainColor(question.domainKey)
    val icon = getDomainIcon(question.domainKey)
    val typeEmoji = when (question.type) {
        "CHOICE" -> "📝"
        "JUDGE" -> "✅"
        else -> "✏️"
    }
    val typeName = when (question.type) {
        "CHOICE" -> "选择"
        "JUDGE" -> "判断"
        else -> "填空"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1e293b)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(getDomainGradient(question.domainKey))),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 24.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = question.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFe8ecf4),
                    maxLines = 1
                )
                Text(
                    text = question.description,
                    fontSize = 12.sp,
                    color = Color(0xFF8892b0),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    LevelBadge(text = question.domainName, color = domainColor)
                    LevelBadge(text = "⭐".repeat(question.difficulty.coerceIn(1, 5)), color = Color(0xFFFF9800))
                    LevelBadge(text = "+${question.expReward}", color = Color(0xFF10b981))
                    LevelBadge(text = "$typeEmoji $typeName", color = Color(0xFF6366f1))
                }
            }
        }
    }
}

private fun getDomainIcon(domainKey: String): String {
    return when (domainKey) {
        "ENGLISH" -> "🔤"
        "SCIENCE" -> "🔬"
        "VALUES" -> "💝"
        "LOGIC" -> "🧩"
        "ENGINEERING" -> "⚙️"
        "PATRIOTISM" -> "🇨🇳"
        "COMPREHENSION" -> "📖"
        "MATH" -> "🔢"
        "LIFESKILL" -> "🏠"
        "CREATIVITY" -> "🎨"
        else -> "🌟"
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
