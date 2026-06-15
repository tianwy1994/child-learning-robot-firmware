package com.childlearning.robot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.childlearning.robot.data.model.ChatMessage
import com.childlearning.robot.ui.theme.AiBubble
import com.childlearning.robot.ui.theme.UserBubble
import java.text.SimpleDateFormat
import java.util.*

/**
 * 聊天气泡
 * 对应固件无 UI，这是新增的 Android 界面组件
 */
@Composable
fun ChatBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.isFromUser
    val bubbleColor = if (isUser) UserBubble else AiBubble
    val textColor = if (isUser) androidx.compose.ui.graphics.Color.White
    else MaterialTheme.colorScheme.onSurface
    val alignment = if (isUser) Arrangement.End else Arrangement.Start
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) androidx.compose.ui.Alignment.End
        else androidx.compose.ui.Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                color = textColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = timeFormat.format(Date(message.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
