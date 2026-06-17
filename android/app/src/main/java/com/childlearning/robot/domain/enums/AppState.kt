package com.childlearning.robot.domain.enums

/**
 * 应用状态枚举
 * 对应固件 config.h 中的 DeviceState
 *
 * 原始状态机:
 *   BOOT -> IDLE <--> FOCUSING
 *                |        |
 *                v        v
 *            LISTENING -> PROCESSING -> SPEAKING
 *   + FOCUS_BREAK (从 FOCUSING 进入)
 *   + ERROR
 *
 * Android 版去掉 STATE_BOOT（不需要硬件启动），其余保留
 */
sealed class AppState {
    // KDoc removed
    data object Idle : AppState()

    // KDoc removed
    data object Focusing : AppState()

    // KDoc removed
    data object FocusBreak : AppState()

    // KDoc removed
    data object Listening : AppState()

    // KDoc removed
    data object Processing : AppState()

    // KDoc removed
    data object Speaking : AppState()

    // KDoc removed
    data class Error(val message: String) : AppState()
}
