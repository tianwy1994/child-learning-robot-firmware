package com.childlearning.robot.domain.enums

/**
 * AI 聊天角色
 * 对应固件 config.h 中的 ChatRole
 */
enum class ChatRole(val value: String) {
    /** 学习伙伴 — 默认角色 */
    COMPANION("COMPANION"),

    /** 作业批改 — 用于作业 OCR 后的批改 */
    GRADER("GRADER"),

    /** 错题讲解 — 用于错题解释 */
    EXPLAINER("EXPLAINER")
}
