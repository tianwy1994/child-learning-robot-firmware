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
    /** 空闲状态 — 主页 */
    data object Idle : AppState()

    /** 专注学习中 — 计时 + 轮询提醒 */
    data object Focusing : AppState()

    /** 专注休息中 — 收到 BREAK 提醒后 */
    data object FocusBreak : AppState()

    /** 录音中 — 长按说话按钮 */
    data object Listening : AppState()

    /** 处理中 — STT → Chat → TTS 流水线 */
    data object Processing : AppState()

    /** 播放中 — TTS 音频播放 */
    data object Speaking : AppState()

    /** 错误状态 */
    data class Error(val message: String) : AppState()
}
