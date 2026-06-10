#ifndef AUTH_MANAGER_H
#define AUTH_MANAGER_H

#include <Arduino.h>
#include <Preferences.h>
#include "../include/config.h"

/**
 * 认证管理器 —— 管理设备的认证状态和 token 生命周期。
 *
 * 职责：
 *   1. 从 NVS 读取/保存设备 token
 *   2. 管理认证状态（LOCKED / AUTHENTICATED / EXPIRED）
 *   3. 提供 token 给 HTTP 请求使用
 *   4. 处理 token 更新（BLE 接收新 token）
 */
class AuthManager {
public:
    void begin();
    void update();

    // Token 管理
    bool hasToken();
    String getToken();
    void setToken(const String& token);
    void clearToken();

    // 认证状态
    AuthState getAuthState();
    bool isAuthenticated();
    String getDeviceId();

    // 状态回调
    typedef void (*AuthStateCallback)(AuthState newState);
    void onStateChange(AuthStateCallback callback);

private:
    Preferences _prefs;
    String _token;
    String _deviceId;
    AuthState _state = AUTH_LOCKED;
    AuthStateCallback _stateCallback = nullptr;
    unsigned long _lastTokenCheck = 0;

    void setState(AuthState newState);
    String generateDeviceId();
    bool isTokenExpired(const String& token);
};

#endif // AUTH_MANAGER_H
