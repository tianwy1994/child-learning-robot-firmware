package com.childlearning.robot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.childlearning.robot.ui.theme.GoldStar

/** 等级名称映射 */
private val LEVEL_NAMES = mapOf(
    1 to "学习小萌新", 2 to "知识探索者", 3 to "习惯养成师", 4 to "学霸小达人", 5 to "学习小导师",
    6 to "智慧勇士", 7 to "知识魔法师", 8 to "学习小超人", 9 to "星际探险家", 10 to "传说学霸"
)
private val LEVEL_ICONS = mapOf(
    1 to "🌱", 2 to "🔍", 3 to "🛠️", 4 to "⭐", 5 to "🎓",
    6 to "⚔️", 7 to "🧙", 8 to "🦸", 9 to "🚀", 10 to "👑"
)

/**
 * 等级徽章
 * 展示用户等级、经验值、连续签到天数
 */
@Composable
fun LevelBadge(
    level: Int,
    experience: Int,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 等级
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = LEVEL_ICONS[level] ?: "⭐",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Lv.$level",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = LEVEL_NAMES[level] ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }

        // 经验
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "💎",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "$experience",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "经验值",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }

        // 连续签到
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🔥",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "${streakDays}天",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "连续签到",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * 简单标签徽章
 * 展示带颜色的文字标签
 */
@Composable
fun LevelBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
