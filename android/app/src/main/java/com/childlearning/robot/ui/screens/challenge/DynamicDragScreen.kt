package com.childlearning.robot.ui.screens.challenge

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.childlearning.robot.core.network.DragCard
import com.childlearning.robot.core.network.TargetSlot
import com.childlearning.robot.ui.components.GradientButton
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun DynamicDragScreen(
    challengeId: Long,
    onBack: () -> Unit,
    viewModel: ChallengeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val challenge = viewModel.currentChallenge.collectAsStateWithLifecycle()
    val evalResult = viewModel.evaluationResult.collectAsStateWithLifecycle()

    var slotCardMap by remember { mutableStateOf(mapOf<Int, Int>()) } // slotId -> cardId
    var draggedCard by remember { mutableStateOf<DragCard?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragStartPosition by remember { mutableStateOf(Offset.Zero) }
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(challengeId) {
        viewModel.loadChallengeDetail(challengeId)
    }

    LaunchedEffect(uiState.value) {
        if (uiState.value is ChallengeUiState.Evaluated) {
            showConfetti = evalResult.value?.isCorrect == true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                    )
                )
                .padding(16.dp)
        ) {
            // 顶部栏
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                ) {
                    Text("← 返回", color = Color.White)
                }

                Button(
                    onClick = { viewModel.speakQuestion(challengeId) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                ) {
                    Text("🔊 朗读", color = Color.White)
                }
            }

            when (uiState.value) {
                is ChallengeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is ChallengeUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = (uiState.value as ChallengeUiState.Error).message,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is ChallengeUiState.Success,
                is ChallengeUiState.Submitting,
                is ChallengeUiState.Evaluated -> {
                    challenge.value?.let { ch ->
                        val schema = ch.pageUiSchema

                        // 题目内容
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = ch.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                Text(
                                    text = ch.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(top = 8.dp),
                                    lineHeight = 24.sp
                                )
                            }
                        }

                        // 提示语
                        schema?.tipText?.let { tip ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF6366f1).copy(alpha = 0.9f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Text(
                                    text = "💡 $tip",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        // 拖拽区域
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 左侧素材区
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .fillMaxSize(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.9f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "📦 魔法素材",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        schema?.dragCards?.forEach { card ->
                                            val isPlaced = slotCardMap.values.contains(card.cardId)

                                            DragCardView(
                                                card = card,
                                                enabled = !isPlaced && uiState.value != ChallengeUiState.Evaluated,
                                                onDragStart = { offset ->
                                                    draggedCard = card
                                                    dragStartPosition = offset
                                                },
                                                onDrag = { offset ->
                                                    dragOffset = offset
                                                },
                                                onDragEnd = { endOffset ->
                                                    // 检测是否拖到某个插槽
                                                    schema.targetSlots.forEach { slot ->
                                                        // 简化碰撞检测
                                                        val slotArea = Rect(
                                                            slot.x.toFloat(),
                                                            slot.y.toFloat(),
                                                            (slot.x + slot.w).toFloat(),
                                                            (slot.y + slot.h).toFloat()
                                                        )
                                                        if (slotArea.contains(endOffset)) {
                                                            // 放入插槽，自动替换原有卡片
                                                            slotCardMap = slotCardMap.toMutableMap().apply {
                                                                put(slot.slotId, card.cardId)
                                                            }
                                                        }
                                                    }
                                                    draggedCard = null
                                                    dragOffset = Offset.Zero
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 右侧插槽区
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.9f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "🎯 魔法插槽",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    Box(modifier = Modifier.fillMaxSize()) {
                                        schema?.targetSlots?.forEach { slot ->
                                            val placedCardId = slotCardMap[slot.slotId]
                                            val placedCard = schema.dragCards.find { it.cardId == placedCardId }
                                            val context = LocalContext.current
            val isCorrect = evalResult.value != null &&
                                                    schema.correctMapping[slot.slotId.toString()] == placedCardId.toString()
            val isWrong = evalResult.value != null &&
                                                    placedCardId != null &&
                                                    schema.correctMapping[slot.slotId.toString()] != placedCardId.toString()

            // 提交后震动反馈
            LaunchedEffect(isCorrect, isWrong) {
                if (isCorrect) {
                    vibrate(context, 100) // 正确长震动
                } else if (isWrong) {
                    vibrate(context, 50) // 错误短震动
                    kotlinx.coroutines.delay(100)
                    vibrate(context, 50) // 两次短震动
                }
            }

                                            TargetSlotView(
                                                slot = slot,
                                                placedCard = placedCard,
                                                isCorrect = isCorrect,
                                                isWrong = isWrong,
                                                enabled = uiState.value != ChallengeUiState.Evaluated
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 提交按钮
                        AnimatedVisibility(
                            visible = uiState.value != ChallengeUiState.Evaluated,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            GradientButton(
                                text = if (uiState.value == ChallengeUiState.Submitting) "⏳ 魔法评估中..." else "🚀 提交答案",
                                onClick = {
                                    val mapping = slotCardMap.mapKeys { it.key.toString() }
                                        .mapValues { it.value.toString() }
                                    viewModel.submitDragAnswer(ch.id, mapping)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                enabled = slotCardMap.isNotEmpty() && uiState.value != ChallengeUiState.Submitting
                            )
                        }

                        // 评估结果
                        AnimatedVisibility(
                            visible = uiState.value == ChallengeUiState.Evaluated,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            evalResult.value?.let { result ->
                                EvaluationResultCard(
                                    result = result,
                                    onContinue = {
                                        viewModel.resetEvaluation()
                                        onBack()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 拖拽中悬浮卡片
        if (draggedCard != null) {
            DraggingCardOverlay(
                card = draggedCard!!,
                offset = dragOffset
            )
        }

        // 撒花特效
        if (showConfetti) {
            ConfettiOverlay()
        }
    }
}

@Composable
fun DragCardView(
    card: DragCard,
    enabled: Boolean,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit
) {
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }

    val cardColor = remember { Color(android.graphics.Color.parseColor(card.color)) }
    val textColor = if (isDarkColor(cardColor)) Color.White else Color.Black

    val alpha = if (enabled) 1f else 0.5f

    Box(
        modifier = Modifier
            .size(card.w.dp, card.h.dp)
            .graphicsLayer(
                scaleX = scale.value,
                scaleY = scale.value,
                rotationZ = rotation.value,
                alpha = alpha
            )
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(cardColor.copy(alpha = 0.8f), cardColor)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                color = cardColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                onDragStart(offset)
                                scale.animateTo(1.2f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                                rotation.animateTo(5f)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount)
                            },
                            onDragEnd = {
                                onDragEnd(dragOffset)
                                scale.animateTo(1f)
                                rotation.animateTo(0f)
                            },
                            onDragCancel = {
                                scale.animateTo(1f)
                                rotation.animateTo(0f)
                            }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = card.emoji, fontSize = 24.sp)
            Text(
                text = card.text,
                color = textColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun TargetSlotView(
    slot: TargetSlot,
    placedCard: DragCard?,
    isCorrect: Boolean,
    isWrong: Boolean,
    enabled: Boolean
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isCorrect -> Color(0xFF10b981)
            isWrong -> Color(0xFFef4444)
            placedCard != null -> Color(0xFF6366f1)
            else -> Color(0xFFcbd5e1)
        },
        animationSpec = tween(300), label = "borderColor"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isCorrect -> Color(0xFFdcfce7).copy(alpha = 0.9f)
            isWrong -> Color(0xFFfee2e2).copy(alpha = 0.9f)
            placedCard != null -> Color(0xFFf5f3ff).copy(alpha = 0.9f)
            else -> Color.White.copy(alpha = 0.5f)
        },
        animationSpec = tween(300), label = "bgColor"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(slot.x.dp.roundToPx(), slot.y.dp.roundToPx()) }
            .size(slot.w.dp, slot.h.dp)
            .shadow(if (slot.glowEffect) 16.dp else 4.dp, RoundedCornerShape(16.dp))
            .background(bgColor, RoundedCornerShape(16.dp))
            .border(
                width = 3.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (placedCard != null) {
            val cardColor = Color(android.graphics.Color.parseColor(placedCard.color))
            val textColor = if (isDarkColor(cardColor)) Color.White else Color.Black
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = placedCard.emoji, fontSize = 24.sp)
                Text(
                    text = placedCard.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            if (isCorrect) {
                Text("✅", modifier = Modifier.align(Alignment.TopEnd))
            } else if (isWrong) {
                Text("❌", modifier = Modifier.align(Alignment.TopEnd))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📥", fontSize = 24.sp)
                Text(
                    text = slot.label,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun DraggingCardOverlay(
    card: DragCard,
    offset: Offset
) {
    val cardColor = remember { Color(android.graphics.Color.parseColor(card.color)) }
    val textColor = if (isDarkColor(cardColor)) Color.White else Color.Black

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .size(card.w.dp, card.h.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(cardColor.copy(alpha = 0.9f), cardColor)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                color = cardColor.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = card.emoji, fontSize = 28.sp)
            Text(
                text = card.text,
                color = textColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun EvaluationResultCard(
    result: EvaluationResult,
    onContinue: () -> Unit
) {
    val bgColor = when {
        result.score >= 80 -> Color(0xFFdcfce7)
        result.score >= 60 -> Color(0xFFfef3c7)
        else -> Color(0xFFfee2e2)
    }

    val borderColor = when {
        result.score >= 80 -> Color(0xFF10b981)
        result.score >= 60 -> Color(0xFFf59e0b)
        else -> Color(0xFFef4444)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(3.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${result.score}分",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                color = when {
                    result.score >= 80 -> Color(0xFF10b981)
                    result.score >= 60 -> Color(0xFFf59e0b)
                    else -> Color(0xFFef4444)
                }
            )

            Text(
                text = "⭐".repeat(result.stars) + "☆".repeat(5 - result.stars),
                fontSize = 32.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Text(
                text = result.encourage,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            result.explanation?.let { explanation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💡 魔法讲解",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color(0xFF6366f1),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Text(
                text = "🎁 获得 ${result.expEarned} 经验值！",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFf59e0b),
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            GradientButton(
                text = "继续下一题 →",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ConfettiOverlay() {
    val confettiCount = 100
    val colors = listOf(
        Color(0xFFff6b6b), Color(0xFFffa94d), Color(0xFF69db7c),
        Color(0xFF74c0fc), Color(0xFFb197fc), Color(0xFFf783ac),
        Color(0xFFffd43b), Color(0xFF38d9a9)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        repeat(confettiCount) {
            val initialX = remember { Random.nextFloat() * 1000 }
            val fallDuration = remember { 3000 + Random.nextInt(3000) }
            val offsetY = remember { Animatable(0f) }

            LaunchedEffect(Unit) {
                offsetY.animateTo(
                    targetValue = 2000f,
                    animationSpec = tween(durationMillis = fallDuration)
                )
            }

            Box(
                modifier = Modifier
                    .offset(
                        x = initialX.dp,
                        y = offsetY.value.dp
                    )
                    .size(10.dp)
                    .background(
                        color = colors.random(),
                        shape = if (Random.nextBoolean()) RoundedCornerShape(50) else RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

fun isDarkColor(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance < 0.5
}

data class Rect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun contains(offset: Offset): Boolean {
        return offset.x >= left && offset.x <= right && offset.y >= top && offset.y <= bottom
    }
}
