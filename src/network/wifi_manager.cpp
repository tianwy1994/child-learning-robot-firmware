#include "wifi_manager.h"

void WifiManager::begin() {
    WiFi.mode(WIFI_STA);
    WiFi.setAutoReconnect(true);
    connect();
}

void WifiManager::update() {
    bool nowConnected = WiFi.isConnected();

    if (nowConnected && !_connected) {
        onConnected();
    } else if (!nowConnected && _connected) {
        onDisconnected();
    }

    // 重连逻辑
    if (!_connected && !_connecting && millis() - _lastRetry > WIFI_RETRY_DELAY_MS) {
        if (_retryCount < WIFI_MAX_RETRIES) {
            connect();
        }
    }
}

bool WifiManager::isConnected() {
    return _connected;
}

String WifiManager::getLocalIP() {
    return _connected ? WiFi.localIP().toString() : "未连接";
}

int WifiManager::getRSSI() {
    return _connected ? WiFi.RSSI() : 0;
}

void WifiManager::onConnectionChange(ConnectionCallback callback) {
    _connCallback = callback;
}

void WifiManager::connect() {
    Serial.printf("[WiFi] 连接到 %s...\n", WIFI_SSID);
    _connecting = true;
    _lastRetry = millis();
    _retryCount++;

    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    // 等待连接（非阻塞，超时后自动重试）
    unsigned long start = millis();
    while (!WiFi.isConnected() && millis() - start < WIFI_TIMEOUT_MS) {
        delay(100);
    }

    if (WiFi.isConnected()) {
        onConnected();
    } else {
        Serial.println("[WiFi] 连接超时");
        _connecting = false;
    }
}

void WifiManager::onConnected() {
    _connected = true;
    _connecting = false;
    _retryCount = 0;
    Serial.printf("[WiFi] 已连接，IP: %s, RSSI: %d dBm\n",
                  WiFi.localIP().toString().c_str(), WiFi.RSSI());

    if (_connCallback) {
        _connCallback(true);
    }
}

void WifiManager::onDisconnected() {
    _connected = false;
    Serial.println("[WiFi] 连接断开");

    if (_connCallback) {
        _connCallback(false);
    }
}
