package com.childlearning.robot.ui.screens.challenge

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.childlearning.robot.core.network.ChallengeDetailResponse
import com.childlearning.robot.ui.components.GradientButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 挑战做题页面 — 点击选择模式
 *
 * 布局：
 * - 顶部：返回按钮 + 题目标题 + 朗读按钮
 * - 中部：题目内容卡片
 * - 选项区：动态出现的可点击选项卡片
 * - 底部：提交/结果/讲解
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailScreen(
    challengeId: Long,
    onBack: () -> Unit,
    viewModel: ChallengeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val challenge by viewModel.currentChallenge.collectAsState()
    val evalResult by viewModel.evaluationResult.collectAsState()
    val context = LocalContext.current

    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(challengeId) {
        viewModel.loadChallengeDetail(challengeId)
    }

    LaunchedEffect(uiState) {
        if (uiState is ChallengeUiState.Evaluated) {
            showConfetti = evalResult?.isCorrect == true
            if (evalResult?.isCorrect == true) {
                vibrate(context, 200)
            } else {
                vibrate(context, 50)
                delay(100)
                vibrate(context, 50)
            }
        }
    }

    // 重置选择（加载新题时）
    LaunchedEffect(challenge) {
        selectedOption = null
        viewModel.resetEvaluation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = challenge?.title ?: "趣味挑战",
                        maxLines = 1,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.speakQuestion(challengeId) }) {
                        Text("🔊 朗读", color = Color(0xFF6C63FF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A1A2E)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FE))
        ) {
            when (uiState) {
                is ChallengeUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF6C63FF)
                    )
                }
                is ChallengeUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "😢", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = (uiState as ChallengeUiState.Error).message,
                            color = Color(0xFF999999),
                            fontSize = 15.sp
                        )
                    }
                }
                else -> {
                    val ch = challenge
                    if (ch != null) {
                        ChallengeContent(
                            challenge = ch,
                            selectedOption = selectedOption,
                            onSelectOption = { option ->
                                if (uiState !is ChallengeUiState.Evaluated && uiState !is ChallengeUiState.Submitting) {
                                    selectedOption = option
                                    vibrate(context, 30)
                                    viewModel.submitAnswer(challengeId, option)
                                }
                            },
                            uiState = uiState,
                            evalResult = evalResult,
                            onContinue = {
                                viewModel.resetEvaluation()
                                onBack()
                            }
                        )
                    }
                }
            }

            // 撒花特效
            if (showConfetti) {
                ConfettiOverlay()
            }
        }
    }
}

@Composable
private fun ChallengeContent(
    challenge: ChallengeDetailResponse,
    selectedOption: String?,
    onSelectOption: (String) -> Unit,
    uiState: ChallengeUiState,
    evalResult: EvaluationResult?,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // ===== 题目内容卡片 =====
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 难度和奖励
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DifficultyBadge(difficulty = challenge.difficulty)
                    Text(
                        text = "🎁 +${challenge.expReward} 经验",
                        fontSize = 13.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = challenge.content,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A2E),
                    lineHeight = 26.sp
                )

                // 提示
                challenge.pageUiSchema?.tipText?.let { tip ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF0E6FF)
                        )
                    ) {
                        Text(
                            text = "💡 $tip",
                            fontSize = 13.sp,
                            color = Color(0xFF6C63FF),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val options = challenge.options
        val isEvaluated = uiState is ChallengeUiState.Evaluated

        // ===== 选项区 =====
        Text(
            text = if (isEvaluated) "📋 答案揭晓：" else "🤔 点击选择答案：",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val isUserCorrect = evalResult?.isCorrect == true

        options.forEachIndexed { index, option ->
            AnimatedOptionCard(
                option = option,
                index = index,
                isSelected = selectedOption == option,
                isEvaluated = isEvaluated,
                isUserCorrect = isUserCorrect,
                correctAnswer = challenge.answer,
                onClick = { onSelectOption(option) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 如果没有 options 但有 dragCards，用 dragCards 的 text 作为选项
        if (options.isEmpty()) {
            challenge.pageUiSchema?.dragCards?.forEachIndexed { index, card ->
                AnimatedOptionCard(
                    option = "${card.emoji} ${card.text}",
                    index = index,
                    isSelected = selectedOption == card.text,
                    isEvaluated = isEvaluated,
                    isUserCorrect = isUserCorrect,
                    correctAnswer = challenge.answer,
                    onClick = { onSelectOption(card.text) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== 评估中提示 =====
        AnimatedVisibility(
            visible = uiState is ChallengeUiState.Submitting,
            enter = fadeIn()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF6C63FF))
                Spacer(modifier = Modifier.width(8.dp))
                Text("⏳ 评估中...", fontSize = 14.sp, color = Color(0xFF999999))
            }
        }

        // ===== 评估结果 + 讲解 =====
        AnimatedVisibility(
            visible = isEvaluated && evalResult != null,
            enter = fadeIn() + expandVertically()
        ) {
            evalResult?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))
                EvaluationSection(
                    result = result,
                    challenge = challenge,
                    onContinue = onContinue
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ===== 动态选项卡片 =====
@Composable
private fun AnimatedOptionCard(
    option: String,
    index: Int,
    isSelected: Boolean,
    isEvaluated: Boolean,
    isUserCorrect: Boolean,
    correctAnswer: String?,
    onClick: () -> Unit
) {
    var appeared by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 依次出现动画
    LaunchedEffect(Unit) {
        delay(index * 100L)
        appeared = true
    }

    // 按压缩放
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    // 颜色状态：以服务端返回的 correct 为准
    // 答对了 → 选中的选项标绿；答错了 → 选中的标红，正确答案标绿
    val isCorrectAnswer = isEvaluated && if (isUserCorrect) isSelected else option == correctAnswer
    val isWrongSelection = isEvaluated && isSelected && !isUserCorrect

    val backgroundColor = when {
        isCorrectAnswer -> Color(0xFFE8F5E9)
        isWrongSelection -> Color(0xFFFFEBEE)
        isSelected -> Color(0xFFF0E6FF)
        else -> Color.White
    }

    val borderColor = when {
        isCorrectAnswer -> Color(0xFF4CAF50)
        isWrongSelection -> Color(0xFFF44336)
        isSelected -> Color(0xFF6C63FF)
        else -> Color(0xFFE8E8E8)
    }

    val iconEmoji = when {
        isCorrectAnswer -> "✅ "
        isWrongSelection -> "❌ "
        isSelected -> "👆 "
        else -> ""
    }

    AnimatedVisibility(
        visible = appeared,
        enter = fadeIn(animationSpec = tween(300)) +
                slideInHorizontally(
                    initialOffsetX = { if (index % 2 == 0) -it else it },
                    animationSpec = tween(400, easing = EaseOutCubic)
                ) +
                scaleIn(initialScale = 0.8f, animationSpec = tween(400))
    ) {
        Card(
            onClick = {
                if (!isEvaluated) {
                    pressed = true
                    coroutineScope.launch {
                        delay(100)
                        pressed = false
                    }
                    onClick()
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            border = androidx.compose.foundation.BorderStroke(2.dp, borderColor),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isSelected) 6.dp else 2.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(scaleX = scale, scaleY = scale)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 选项序号
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) Color(0xFF6C63FF)
                            else Color(0xFFF0F0F0)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${'A' + index}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF666666)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "$iconEmoji$option",
                    fontSize = 16.sp,
                    color = when {
                        isCorrectAnswer -> Color(0xFF2E7D32)
                        isWrongSelection -> Color(0xFFC62828)
                        isSelected -> Color(0xFF4A148C)
                        else -> Color(0xFF1A1A2E)
                    },
                    fontWeight = if (isSelected || isEvaluated) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )

                // 正确/错误标记
                if (isCorrectAnswer) {
                    Text("正确答案", fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                } else if (isWrongSelection) {
                    Text("选错了", fontSize = 12.sp, color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ===== 评估结果 + 讲解 =====
@Composable
private fun EvaluationSection(
    result: EvaluationResult,
    challenge: ChallengeDetailResponse,
    onContinue: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 分数
            Text(
                text = "${result.score}分",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = when {
                    result.score >= 80 -> Color(0xFF4CAF50)
                    result.score >= 60 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
            )

            // 星星
            Text(
                text = "⭐".repeat(result.stars) + "☆".repeat(5 - result.stars),
                fontSize = 28.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 鼓励语
            Text(
                text = result.encourage,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A2E),
                textAlign = TextAlign.Center
            )

            // 经验值
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = "🎁 获得 ${result.expEarned} 经验值！",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 讲解
            if (!result.explanation.isNullOrBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0E6FF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📚 知识讲解",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6C63FF),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = result.explanation,
                            fontSize = 14.sp,
                            color = Color(0xFF333333),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // 扩展思考
            challenge.explanation?.let { ext ->
                if (ext.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🤔 想一想",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = ext,
                                fontSize = 14.sp,
                                color = Color(0xFF333333),
                                lineHeight = 22.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            GradientButton(
                text = "继续下一题 →",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ===== 难度标签 =====
@Composable
private fun DifficultyBadge(difficulty: Int) {
    val (text, color) = when (difficulty) {
        1 -> "⭐ 简单" to Color(0xFF4CAF50)
        2 -> "⭐⭐ 中等" to Color(0xFFFF9800)
        3 -> "⭐⭐⭐ 困难" to Color(0xFFF44336)
        else -> "⭐ 入门" to Color(0xFF2196F3)
    }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun vibrate(context: Context, durationMs: Long) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs)
    }
}
