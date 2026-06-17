package com.childlearning.robot.domain.enums

/**
 * 认证状态枚举
 * 对应固件 config.h 中的 AuthState
 */
sealed class AuthState {
    // KDoc removed
    data object Locked : AuthState()

    // KDoc removed
    data object Authenticated : AuthState()

    // KDoc removed
    data object Expired : AuthState()
}
