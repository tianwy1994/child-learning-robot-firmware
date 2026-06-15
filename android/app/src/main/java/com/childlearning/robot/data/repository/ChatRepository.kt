package com.childlearning.robot.data.repository

import com.childlearning.robot.core.network.ApiService
import com.childlearning.robot.core.network.ChatRequest
import com.childlearning.robot.core.network.ChatResponse
import com.childlearning.robot.domain.enums.ChatRole
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI 聊天仓库
 * 对应固件 ChatManager — 管理 sessionId 和聊天 API 调用
 *
 * 固件逻辑：
 * - sessionId 存在内存中，首次对话不传，后续传上次返回的 sessionId
 * - resetSession() 清除 sessionId 开始新对话
 * - 专注模式下 role 固定为 COMPANION
 */
@Singleton
class ChatRepository @Inject constructor(
    private val apiService: ApiService
) {
    /** 当前会话 ID，对应固件 ChatManager._sessionId */
    private var sessionId: String? = null

    /**
     * 发送聊天消息
     * 对应固件 ChatManager::sendMessage(message, role)
     */
    suspend fun sendMessage(
        message: String,
        role: ChatRole = ChatRole.COMPANION
    ): Result<ChatResponse> {
        return try {
            val request = ChatRequest(
                message = message,
                role = role.value,
                sessionId = sessionId
            )
            val response = apiService.sendChatMessage(request)
            if (response.isSuccess && response.data != null) {
                // 缓存 sessionId，对应固件 _sessionId = response.sessionId
                sessionId = response.data.sessionId
                Result.success(response.data)
            } else {
                Result.failure(Exception("聊天请求失败: code=${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 重置会话
     * 对应固件 ChatManager::resetSession()
     */
    fun resetSession() {
        sessionId = null
    }
}
