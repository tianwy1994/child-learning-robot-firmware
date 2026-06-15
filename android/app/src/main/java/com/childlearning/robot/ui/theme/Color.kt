package com.childlearning.robot.ui.theme

import androidx.compose.ui.graphics.Color

// 主题色彩 — 对应固件 RGB LED 状态颜色
val Primary = Color(0xFF4CAF50)        // 绿色 — 对应 AUTH_AUTHENTICATED
val PrimaryVariant = Color(0xFF388E3C)
val Secondary = Color(0xFF2196F3)      // 蓝色 — 对应 WiFi 已连接

val Error = Color(0xFFF44336)          // 红色 — 对应 AUTH_LOCKED
val Warning = Color(0xFFFF9800)        // 黄色 — 对应 AUTH_EXPIRED

// 专注模式色彩
val FocusGreen = Color(0xFF66BB6A)
val FocusBreakOrange = Color(0xFFFFA726)

// 聊天色彩
val UserBubble = Color(0xFF2196F3)
val AiBubble = Color(0xFFE8F5E9)

// 等级色彩
val GoldStar = Color(0xFFFFD700)

// 背景
val LightBackground = Color(0xFFF5F5F5)
val DarkBackground = Color(0xFF121212)
