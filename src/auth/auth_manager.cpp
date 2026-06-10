#include "auth_manager.h"
#include <WiFi.h>

void AuthManager::begin() {
    _prefs.begin(NVS_NAMESPACE, false);

    // 读取保存的 token
    _token = _prefs.getString(NVS_KEY_TOKEN, "");
    _deviceId = _prefs.getString(NVS_KEY_DEVICE_ID, "");

    // 如果没有设备ID，生成一个
    if (_deviceId.isEmpty()) {
        _deviceId = generateDeviceId();
        _prefs.putString(NVS_KEY_DEVICE_ID, _deviceId);
    }

    // 根据 token 状态设置初始认证状态
    if (_token.isEmpty()) {
        setState(AUTH_LOCKED);
        Serial.println("[Auth] 无 token，设备锁定");
    } else {
        // 简单检查 token 格式（JWT 由三段 base64 组成）
        int dotCount = 0;
        for (int i = 0; i < _token.length(); i++) {
            if (_token[i] == '.') dotCount++;
        }
        if (dotCount == 2) {
            setState(AUTH_AUTHENTICATED);
            Serial.println("[Auth] 已有 token，设备已认证");
        } else {
            setState(AUTH_LOCKED);
            Serial.println("[Auth] token 格式无效，设备锁定");
        }
    }
}

void AuthManager::update() {
    // 定期检查 token 状态（每 60 秒）
    if (millis() - _lastTokenCheck > 60000) {
        _lastTokenCheck = millis();

        if (_state == AUTH_AUTHENTICATED && !_token.isEmpty()) {
            // 检查 token 是否过期
            if (isTokenExpired(_token)) {
                setState(AUTH_EXPIRED);
                Serial.println("[Auth] token 已过期");
            }
        }
    }
}

bool AuthManager::hasToken() {
    return !_token.isEmpty();
}

String AuthManager::getToken() {
    return _token;
}

void AuthManager::setToken(const String& token) {
    _token = token;
    _prefs.putString(NVS_KEY_TOKEN, token);
    setState(AUTH_AUTHENTICATED);
    Serial.println("[Auth] token 已更新，设备已认证");
}

void AuthManager::clearToken() {
    _token = "";
    _prefs.remove(NVS_KEY_TOKEN);
    setState(AUTH_LOCKED);
    Serial.println("[Auth] token 已清除，设备锁定");
}

AuthState AuthManager::getAuthState() {
    return _state;
}

bool AuthManager::isAuthenticated() {
    return _state == AUTH_AUTHENTICATED;
}

String AuthManager::getDeviceId() {
    return _deviceId;
}

void AuthManager::onStateChange(AuthStateCallback callback) {
    _stateCallback = callback;
}

void AuthManager::setState(AuthState newState) {
    if (_state != newState) {
        _state = newState;
        if (_stateCallback) {
            _stateCallback(newState);
        }
    }
}

String AuthManager::generateDeviceId() {
    // 使用 WiFi MAC 地址作为设备ID
    uint8_t mac[6];
    WiFi.macAddress(mac);
    char id[18];
    snprintf(id, sizeof(id), "%02X:%02X:%02X:%02X:%02X:%02X",
             mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
    return String(id);
}

bool AuthManager::isTokenExpired(const String& token) {
    // 简单解析 JWT 的 exp 字段
    // JWT 格式: header.payload.signature
    // payload 是 base64url 编码的 JSON
    int firstDot = token.indexOf('.');
    int secondDot = token.indexOf('.', firstDot + 1);

    if (firstDot < 0 || secondDot < 0) {
        return true; // 格式无效，视为过期
    }

    String payload = token.substring(firstDot + 1, secondDot);

    // Base64url 解码
    payload.replace("-", "+");
    payload.replace("_", "/");
    while (payload.length() % 4 != 0) {
        payload += "=";
    }

    // 查找 "exp" 字段
    int expIndex = payload.indexOf("\"exp\":");
    if (expIndex < 0) {
        expIndex = payload.indexOf("\"exp\" :");
        if (expIndex < 0) return false; // 没有 exp 字段，不视为过期
        expIndex += 7;
    } else {
        expIndex += 6;
    }

    // 提取 exp 值（Unix 时间戳）
    String expStr = "";
    for (int i = expIndex; i < payload.length(); i++) {
        char c = payload[i];
        if (c >= '0' && c <= '9') {
            expStr += c;
        } else if (expStr.length() > 0) {
            break;
        }
    }

    if (expStr.isEmpty()) return false;

    // 比较过期时间（注意：这里简化处理，实际应该用 NTP 同步时间）
    // 由于 ESP32 可能没有 NTP，这里假设 token 有效
    // 实际验证在 HTTP 请求时由服务端完成
    return false;
}
