#include "focus_manager.h"
#include "../network/http_client.h"
#include "../audio/audio_player.h"
#include <ArduinoJson.h>

bool FocusManager::startFocus(HttpClient& httpClient, const String& taskDescription) {
    JsonDocument doc;
    doc["taskDescription"] = taskDescription;
    String body;
    serializeJson(doc, body);

    int statusCode;
    String response = httpClient.post(API_FOCUS_START, body, statusCode);

    if (statusCode == 200) {
        JsonDocument respDoc;
        DeserializationError err = deserializeJson(respDoc, response);
        if (!err && respDoc["code"].as<int>() == 200) {
            _focusing = true;
            _inBreak = false;
            Serial.println("[Focus] 专注已开始");
            return true;
        }
    }

    Serial.printf("[Focus] 开始专注失败: %d\n", statusCode);
    return false;
}

bool FocusManager::endFocus(HttpClient& httpClient) {
    int statusCode;
    String response = httpClient.post(API_FOCUS_END, "{}", statusCode);

    if (statusCode == 200) {
        _focusing = false;
        _inBreak = false;
        Serial.println("[Focus] 专注已结束");
        return true;
    }

    Serial.printf("[Focus] 结束专注失败: %d\n", statusCode);
    return false;
}

bool FocusManager::pollStatus(HttpClient& httpClient, AudioPlayer& audioPlayer) {
    // 节流：30 秒轮询一次
    if (millis() - _lastPollTime < STATUS_CHECK_INTERVAL) {
        return _focusing;
    }
    _lastPollTime = millis();

    if (!httpClient.isAuthenticated()) {
        return false;
    }

    int statusCode;
    String response = httpClient.get(API_FOCUS_STATUS, statusCode);

    if (statusCode != 200) {
        if (statusCode == 401) {
            Serial.println("[Focus] token 无效");
        }
        return _focusing;
    }

    JsonDocument doc;
    DeserializationError err = deserializeJson(doc, response);
    if (err || doc["code"].as<int>() != 200) {
        return _focusing;
    }

    // 检查是否有活跃会话
    if (doc["data"].isNull()) {
        _focusing = false;
        _inBreak = false;
        return false;
    }

    _focusing = true;
    JsonObject data = doc["data"];

    // 检查是否有待播放的提醒
    if (data["reminder"].is<JsonObject>()) {
        JsonObject reminder = data["reminder"];
        const char* preset = reminder["preset"];
        const char* type = reminder["type"];

        if (preset && type) {
            Serial.printf("[Focus] 收到提醒: type=%s, preset=%s\n", type, preset);

            // 更新状态
            if (strcmp(type, "BREAK") == 0) {
                _inBreak = true;
            } else if (strcmp(type, "CONTINUE") == 0) {
                _inBreak = false;
            }

            // 播放提醒语音
            if (!audioPlayer.isPlaying()) {
                audioPlayer.playPreset(httpClient, String(preset));
            }

            // 确认提醒已播放
            ackReminder(httpClient, String(type));
        }
    }

    Serial.printf("[Focus] 状态: focusing=%d, break=%d\n", _focusing, _inBreak);
    return true;
}

void FocusManager::ackReminder(HttpClient& httpClient, const String& type) {
    JsonDocument doc;
    doc["type"] = type;
    String body;
    serializeJson(doc, body);

    int statusCode;
    httpClient.post(API_FOCUS_REMINDER_ACK, body, statusCode);

    if (statusCode == 200) {
        Serial.printf("[Focus] 提醒已确认: %s\n", type.c_str());
    } else {
        Serial.printf("[Focus] 提醒确认失败: %d\n", statusCode);
    }
}

bool FocusManager::isFocusing() {
    return _focusing;
}

bool FocusManager::isInBreak() {
    return _inBreak;
}
