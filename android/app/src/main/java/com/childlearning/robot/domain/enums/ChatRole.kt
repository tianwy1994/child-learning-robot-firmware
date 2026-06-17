package com.childlearning.robot.domain.enums

/**
 * AI 聊天角色
 * 对应固件 config.h 中的 ChatRole
 */
enum class ChatRole(val value: String) {
    // KDoc removed
    COMPANION("COMPANION"),

    // KDoc removed
    GRADER("GRADER"),

    // KDoc removed
    EXPLAINER("EXPLAINER")
}
