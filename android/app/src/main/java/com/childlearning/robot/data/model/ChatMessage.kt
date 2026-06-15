package com.childlearning.robot.data.model

/**
 * 聊天消息数据模型
 * 对应固件 ChatManager 中的消息结构
 */
data class ChatMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
