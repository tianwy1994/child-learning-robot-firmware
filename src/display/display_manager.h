#ifndef DISPLAY_MANAGER_H
#define DISPLAY_MANAGER_H

#include <Arduino.h>
#include "../include/config.h"

/**
 * LED 状态显示管理器。
 *
 * 通过 RGB LED 颜色指示设备状态：
 *   - 红色闪烁：未登录（LOCKED）
 *   - 绿色常亮：已认证（AUTHENTICATED）
 *   - 黄色闪烁：token 过期（EXPIRED）
 *   - 蓝色：WiFi 连接中
 *   - 白色：系统启动
 */
class DisplayManager {
public:
    void begin();
    void update();

    // 状态显示
    void showLocked();          // 红色闪烁：未登录
    void showAuthenticated();   // 绿色常亮：已认证
    void showExpired();         // 黄色闪烁：token 过期
    void showWiFiConnecting();  // 蓝色闪烁：WiFi 连接中
    void showWiFiConnected();   // 蓝色常亮：WiFi 已连接
    void showStarting();        // 白色：系统启动
    void showOff();             // 关闭 LED

    // 设备状态显示
    void showDeviceState(DeviceState state);

private:
    void setColor(int r, int g, int b);
    void blink(int r, int g, int b, int intervalMs);

    int _ledR = 0, _ledG = 0, _ledB = 0;
    bool _blinking = false;
    int _blinkInterval = 500;
    unsigned long _lastBlink = 0;
    bool _blinkState = false;
};

#endif // DISPLAY_MANAGER_H
