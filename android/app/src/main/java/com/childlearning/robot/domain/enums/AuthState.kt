package com.childlearning.robot.domain.enums

/**
 * 认证状态枚举
 * 对应固件 config.h 中的 AuthState
 */
sealed class AuthState {
    /** 未登录 — 显示登录界面 */
    data object Locked : AuthState()

    /** 已认证 — 正常使用 */
    data object Authenticated : AuthState()

    /** Token 过期 — 需要重新登录 */
    data object Expired : AuthState()
}
