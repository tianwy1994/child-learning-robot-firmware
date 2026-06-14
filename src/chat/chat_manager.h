#ifndef CHAT_MANAGER_H
#define CHAT_MANAGER_H

#include <Arduino.h>
#include "../include/config.h"

class HttpClient;

/**
 * 聊天管理器 —— 封装与服务端的 AI 聊天交互。
 *
 * 支持三种角色：
 *   - COMPANION（学习陪伴）：日常对话、鼓励
 *   - GRADER（作业批改）：分析作业内容
 *   - EXPLAINER（错题讲解）：讲解错题
 *
 * 接口：POST /api/hardware/chat/send
 */
class ChatManager {
public:
    /**
     * 发送聊天消息并获取 AI 回复。
     *
     * @param httpClient  HTTP 客户端
     * @param message     用户消息文本
     * @param role        聊天角色
     * @return AI 回复文本，失败返回空字符串
     */
    String sendMessage(HttpClient& httpClient, const String& message, ChatRole role = ROLE_COMPANION);

    /**
     * 获取当前会话 ID。
     */
    String getSessionId();

    /**
     * 重置会话（开始新对话）。
     */
    void resetSession();

private:
    String _sessionId = "";
};

#endif // CHAT_MANAGER_H
