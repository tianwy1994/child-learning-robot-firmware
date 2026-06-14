#include "chat_manager.h"
#include "http_client.h"
#include <ArduinoJson.h>

String ChatManager::sendMessage(HttpClient& httpClient, const String& message, ChatRole role) {
    if (message.isEmpty()) { Serial.println("[Chat] 错误：消息为空"); return ""; }

    const char* roleStr;
    switch (role) {
        case ROLE_COMPANION: roleStr = "COMPANION"; break;
        case ROLE_GRADER:    roleStr = "GRADER"; break;
        case ROLE_EXPLAINER: roleStr = "EXPLAINER"; break;
        default:             roleStr = "COMPANION"; break;
    }

    JsonDocument doc;
    doc["message"] = message;
    doc["role"] = roleStr;
    if (!_sessionId.isEmpty()) doc["sessionId"] = _sessionId;

    String jsonBody;
    serializeJson(doc, jsonBody);

    Serial.printf("[Chat] 发送消息 [%s]: %s\n", roleStr,
                  message.substring(0, min(50, (int)message.length())).c_str());

    int statusCode;
    String response = httpClient.post(API_CHAT_SEND, jsonBody, statusCode);

    if (statusCode == 200) {
        JsonDocument respDoc;
        DeserializationError err = deserializeJson(respDoc, response);
        if (!err && respDoc["code"].as<int>() == 200 && respDoc["data"].is<JsonObject>()) {
            String reply = respDoc["data"]["reply"].as<String>();
            String sid = respDoc["data"]["sessionId"].as<String>();
            if (!sid.isEmpty()) _sessionId = sid;
            Serial.printf("[Chat] AI 回复: %s\n", reply.substring(0, 50).c_str());
            return reply;
        } else {
            Serial.printf("[Chat] 响应格式错误: %s\n", response.c_str());
        }
    } else {
        Serial.printf("[Chat] 请求失败: %d\n", statusCode);
    }
    return "";
}

String ChatManager::getSessionId() { return _sessionId; }
void ChatManager::resetSession() { _sessionId = ""; Serial.println("[Chat] 会话已重置"); }
