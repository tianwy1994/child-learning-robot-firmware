package com.childlearning.robot.domain.usecase

import com.childlearning.robot.data.model.ChatMessage
import com.childlearning.robot.data.repository.ChatRepository
import com.childlearning.robot.domain.enums.ChatRole
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 聊天用例
 * 对应固件 ChatManager — 管理聊天会话和消息
 */
@Singleton
class ChatUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {
    /** 聊天历史（内存中，对应固件无持久化） */
    private val _messages = mutableListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages.toList()

    /**
     * 发送消息并获取回复
     * 对应固件 ChatManager::sendMessage()
     */
    suspend fun sendMessage(text: String, role: ChatRole = ChatRole.COMPANION): Result<ChatMessage> {
        // 添加用户消息
        val userMessage = ChatMessage(content = text, isFromUser = true)
        _messages.add(userMessage)

        // 调用 API
        val result = chatRepository.sendMessage(text, role)
        return result.map { response ->
            val replyMessage = ChatMessage(content = response.reply, isFromUser = false)
            _messages.add(replyMessage)
            replyMessage
        }
    }

    /**
     * 重置会话
     * 对应固件 ChatManager::resetSession()
     */
    fun resetSession() {
        chatRepository.resetSession()
        _messages.clear()
    }
}
