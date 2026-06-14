#include "game_manager.h"
#include "../network/http_client.h"
#include "../audio/audio_player.h"
#include <ArduinoJson.h>

bool GameManager::dailyCheckin(HttpClient& httpClient, AudioPlayer& audioPlayer) {
    if (!httpClient.isAuthenticated()) {
        return false;
    }

    // 每天只打卡一次
    if (_checkedInToday && !isNewDay()) {
        return true;
    }

    Serial.println("[Game] 执行每日打卡...");

    int statusCode;
    String response = httpClient.post(API_GAME_CHECKIN, "{}", statusCode);

    if (statusCode == 200) {
        JsonDocument doc;
        DeserializationError err = deserializeJson(doc, response);
        if (!err && doc["code"].as<int>() == 200) {
            _checkedInToday = true;
            _lastCheckinDay = millis();

            // 打卡成功，播放鼓励语音
            JsonObject data = doc["data"];
            if (data["firstCheckin"].as<bool>()) {
                Serial.println("[Game] 首次打卡！");
                audioPlayer.playPreset(httpClient, "encourage");
            } else {
                int streak = data["streakDays"].as<int>();
                Serial.printf("[Game] 连续打卡 %d 天\n", streak);
            }
            return true;
        }
    }

    Serial.printf("[Game] 打卡失败: %d\n", statusCode);
    return false;
}

void GameManager::announceProfile(HttpClient& httpClient, AudioPlayer& audioPlayer) {
    if (!httpClient.isAuthenticated()) return;

    int statusCode;
    String response = httpClient.get(API_GAME_PROFILE, statusCode);

    if (statusCode == 200) {
        JsonDocument doc;
        DeserializationError err = deserializeJson(doc, response);
        if (!err && doc["code"].as<int>() == 200) {
            JsonObject data = doc["data"];
            int level = data["level"].as<int>();
            int exp = data["experience"].as<int>();
            int streak = data["streakDays"].as<int>();

            Serial.printf("[Game] 等级: %d, 经验: %d, 连续打卡: %d 天\n", level, exp, streak);

            // 通过 TTS 播报
            String msg = "当前等级 " + String(level) + "，经验值 " + String(exp) +
                         "，连续打卡 " + String(streak) + " 天。继续加油哦！";
            audioPlayer.playText(httpClient, msg);
        }
    }
}

bool GameManager::hasCheckedInToday() {
    return _checkedInToday && !isNewDay();
}

bool GameManager::isNewDay() {
    // 简单判断：超过 20 小时视为新的一天
    // 实际应该用 RTC 或 NTP 判断日期变化
    if (_lastCheckinDay == 0) return true;
    return (millis() - _lastCheckinDay) > 20UL * 3600UL * 1000UL;
}
