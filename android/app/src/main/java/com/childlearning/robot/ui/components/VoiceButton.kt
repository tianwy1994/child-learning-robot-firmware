package com.childlearning.robot.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.childlearning.robot.ui.theme.Error
import com.childlearning.robot.ui.theme.Primary

/**
 * 语音按钮 — 长按说话
 * 对应固件 ButtonManager 的长按逻辑 (hold > 3s = 长按)
 *
 * Android 改为：按住录音，松手发送
 */
@Composable
fun VoiceButton(
    isRecording: Boolean,
    isProcessing: Boolean,
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 录音中的脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val backgroundColor = when {
        isRecording -> Error
        isProcessing -> MaterialTheme.colorScheme.secondary
        else -> Primary
    }

    val scale = if (isRecording) pulseScale else 1f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(100.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(backgroundColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onRecordStart()
                            tryAwaitRelease()
                            onRecordStop()
                        }
                    )
                }
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "语音",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                isRecording -> "松手发送"
                isProcessing -> "识别中..."
                else -> "按住说话"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
