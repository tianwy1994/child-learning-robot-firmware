#include "stt_client.h"
#include "http_client.h"
#include <ArduinoJson.h>
#include <WiFi.h>
#include <HTTPClient.h>

String SttClient::recognize(HttpClient& httpClient, const uint8_t* audioData, size_t audioSize) {
    if (!audioData || audioSize == 0) {
        Serial.println("[STT] 错误：无音频数据");
        return "";
    }

    if (!WiFi.isConnected()) {
        Serial.println("[STT] 错误：WiFi 未连接");
        return "";
    }

    Serial.printf("[STT] 发送音频进行识别: %d bytes (%.1f 秒)\n",
                  audioSize, audioSize / (float)(AUDIO_SAMPLE_RATE * 2));

    String url = String(SERVER_BASE_URL) + API_STT_RECOGNIZE;
    HTTPClient http;
    http.begin(url);
    http.setTimeout(30000);  // STT 可能需要更长时间
    http.addHeader("Content-Type", "audio/pcm");
    http.addHeader("X-Audio-Sample-Rate", "16000");
    http.addHeader("X-Audio-Bits", "16");
    http.addHeader("X-Audio-Channels", "1");

    // 携带设备 token
    String token = httpClient.getToken();
    if (!token.isEmpty()) {
        http.addHeader("Authorization", "Bearer " + token);
    }

    int statusCode = http.POST((uint8_t*)audioData, audioSize);

    String result = "";
    if (statusCode == 200) {
        String response = http.getString();
        JsonDocument doc;
        DeserializationError err = deserializeJson(doc, response);
        if (!err && doc["code"].as<int>() == 200 && doc["data"].is<JsonObject>()) {
            result = doc["data"]["text"].as<String>();
            Serial.printf("[STT] 识别结果: %s\n", result.substring(0, 50).c_str());
        } else {
            Serial.printf("[STT] 响应格式错误: %s\n", response.c_str());
        }
    } else if (statusCode == 401) {
        httpClient.handleUnauthorized();
        Serial.println("[STT] 认证失败 (401)");
    } else {
        Serial.printf("[STT] 请求失败: %d\n", statusCode);
    }

    http.end();
    return result;
}
