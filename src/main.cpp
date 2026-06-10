/**
 * 儿童学习陪伴机器人 — 主程序
 *
 * 启动流程：
 *   1. 初始化串口、LED
 *   2. 初始化认证管理器（从 NVS 读取 token）
 *   3. 初始化 BLE 服务（等待 APP 发送 token）
 *   4. 初始化 WiFi（连接路由器）
 *   5. 如果有 token，初始化 HTTP 客户端
 *   6. 主循环：传感器更新 → 状态机更新 → HTTP 通信
 *
 * 认证流程：
 *   设备首次使用 → 无 token → LED 红色闪烁 → 等待 APP 绑定
 *   APP 绑定成功 → BLE 接收 token → 存入 NVS → LED 绿色常亮
 *   设备重启 → 从 NVS 读取 token → 直接进入已认证状态
 */

#include <Arduino.h>
#include "../include/config.h"
#include "auth/auth_manager.h"
#include "ble/ble_server.h"
#include "network/wifi_manager.h"
#include "network/http_client.h"
#include "display/display_manager.h"
#include "audio/audio_player.h"

// 全局对象
AuthManager authManager;
BleServer bleServer;
WifiManager wifiManager;
HttpClient httpClient;
DisplayManager displayManager;
AudioPlayer audioPlayer;

// 状态变量
unsigned long lastSensorUpdate = 0;
unsigned long lastStatusCheck = 0;
DeviceState currentState = STATE_STANDBY;
bool greetingPlayed = false;  // 是否已播放开机问候语

// BLE token 接收回调
void onTokenReceived(const String& token) {
    Serial.println("[Main] 收到新 token");
    authManager.setToken(token);
    httpClient.setToken(token);
    displayManager.showAuthenticated();

    // 可选：通知服务端设备已上线
    // httpClient.post("/api/hardware/device/online", "{}", statusCode);
}

// 认证状态变化回调
void onAuthStateChanged(AuthState newState) {
    switch (newState) {
        case AUTH_LOCKED:
            displayManager.showLocked();
            break;
        case AUTH_AUTHENTICATED:
            displayManager.showAuthenticated();
            httpClient.setToken(authManager.getToken());
            break;
        case AUTH_EXPIRED:
            displayManager.showExpired();
            break;
    }
}

// WiFi 连接状态回调
void onWiFiConnected(bool connected) {
    if (connected) {
        displayManager.showWiFiConnected();
        Serial.printf("[Main] WiFi 已连接，IP: %s\n", wifiManager.getLocalIP().c_str());

        // WiFi 连接成功后，如果已认证，播放开机问候语
        if (authManager.isAuthenticated() && !greetingPlayed) {
            greetingPlayed = true;
            audioPlayer.playPreset(httpClient, "greeting");
        }
    } else {
        displayManager.showWiFiConnecting();
    }
}

// HTTP 401 回调（token 无效）
void onUnauthorized() {
    Serial.println("[Main] token 无效，清除 token");
    authManager.clearToken();
    displayManager.showLocked();
}

void setup() {
    // 初始化串口
    Serial.begin(115200);
    delay(1000);
    Serial.println("\n================================");
    Serial.println("儿童学习陪伴机器人 启动中...");
    Serial.println("================================");

    // 初始化 LED
    displayManager.begin();
    displayManager.showStarting();

    // 初始化认证管理器
    authManager.begin();
    authManager.onStateChange(onAuthStateChanged);

    Serial.printf("[Main] 设备 ID: %s\n", authManager.getDeviceId().c_str());
    Serial.printf("[Main] 认证状态: %s\n",
        authManager.isAuthenticated() ? "已认证" : "未认证");

    // 初始化 BLE 服务
    bleServer.begin(authManager.getDeviceId());
    bleServer.onTokenReceived(onTokenReceived);

    // 初始化 WiFi
    wifiManager.begin();
    wifiManager.onConnectionChange(onWiFiConnected);

    // 初始化 HTTP 客户端
    httpClient.onUnauthorized(onUnauthorized);
    if (authManager.isAuthenticated()) {
        httpClient.setToken(authManager.getToken());
    }

    // 初始化音频播放器
    audioPlayer.begin();

    // 根据认证状态设置 LED
    if (authManager.isAuthenticated()) {
        displayManager.showAuthenticated();
    } else {
        displayManager.showLocked();
    }

    Serial.println("[Main] 初始化完成，进入主循环");
    Serial.println("================================\n");
}

void loop() {
    // 更新各模块
    authManager.update();
    wifiManager.update();
    displayManager.update();
    audioPlayer.update();

    // 定期检查状态（每 30 秒）
    if (millis() - lastStatusCheck > STATUS_CHECK_INTERVAL) {
        lastStatusCheck = millis();

        // 打印状态
        Serial.printf("[Status] WiFi: %s, Auth: %s, Token: %s\n",
            wifiManager.isConnected() ? "已连接" : "未连接",
            authManager.isAuthenticated() ? "已认证" : "未认证",
            authManager.hasToken() ? "有" : "无");

        // 如果已认证且 WiFi 已连接，向服务端发送心跳并检查提醒
        if (authManager.isAuthenticated() && wifiManager.isConnected()) {
            int statusCode;
            String response = httpClient.get("/api/hardware/focus/status", statusCode);
            if (statusCode == 200) {
                Serial.println("[Main] 心跳成功");

                // 解析响应，检查是否有待播放的提醒
                JsonDocument doc;
                DeserializationError err = deserializeJson(doc, response);
                if (!err && doc["data"].is<JsonObject>()) {
                    JsonObject data = doc["data"];
                    if (data["reminder"].is<JsonObject>()) {
                        const char* preset = data["reminder"]["preset"];
                        const char* type = data["reminder"]["type"];
                        if (preset && type && !audioPlayer.isPlaying()) {
                            Serial.printf("[Main] 收到提醒: type=%s, preset=%s\n", type, preset);
                            // 播放预置语音
                            audioPlayer.playPreset(httpClient, String(preset));
                            // 确认提醒已播放
                            String ackBody = "{\"type\":\"" + String(type) + "\"}";
                            int ackCode;
                            httpClient.post("/api/hardware/focus/reminder/ack", ackBody, ackCode);
                            if (ackCode == 200) {
                                Serial.printf("[Main] 提醒已确认: %s\n", type);
                            }
                        }
                    }
                }
            } else if (statusCode == 401) {
                Serial.println("[Main] 心跳失败：token 无效");
            }
        }
    }

    // 主循环延时
    delay(10);
}
