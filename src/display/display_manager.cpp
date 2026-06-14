#include "display_manager.h"

void DisplayManager::begin() {
    // 配置 LED 引脚为 PWM 输出
    ledcAttach(PIN_LED_R, 5000, 8);
    ledcAttach(PIN_LED_G, 5000, 8);
    ledcAttach(PIN_LED_B, 5000, 8);

    showOff();
    Serial.println("[Display] LED 初始化完成");
}

void DisplayManager::update() {
    // 处理闪烁逻辑
    if (_blinking && millis() - _lastBlink > _blinkInterval) {
        _lastBlink = millis();
        _blinkState = !_blinkState;

        if (_blinkState) {
            setColor(_ledR, _ledG, _ledB);
        } else {
            setColor(0, 0, 0);
        }
    }
}

void DisplayManager::showLocked() {
    _blinking = true;
    _blinkInterval = 500;
    _ledR = 255; _ledG = 0; _ledB = 0;  // 红色
    Serial.println("[Display] 状态：未登录（红色闪烁）");
}

void DisplayManager::showAuthenticated() {
    _blinking = false;
    setColor(0, 255, 0);  // 绿色常亮
    Serial.println("[Display] 状态：已认证（绿色常亮）");
}

void DisplayManager::showExpired() {
    _blinking = true;
    _blinkInterval = 1000;
    _ledR = 255; _ledG = 255; _ledB = 0;  // 黄色
    Serial.println("[Display] 状态：token 过期（黄色闪烁）");
}

void DisplayManager::showWiFiConnecting() {
    _blinking = true;
    _blinkInterval = 300;
    _ledR = 0; _ledG = 0; _ledB = 255;  // 蓝色
}

void DisplayManager::showWiFiConnected() {
    _blinking = false;
    setColor(0, 0, 255);  // 蓝色常亮
}

void DisplayManager::showStarting() {
    _blinking = false;
    setColor(255, 255, 255);  // 白色
}

void DisplayManager::showOff() {
    _blinking = false;
    setColor(0, 0, 0);
}

void DisplayManager::showDeviceState(DeviceState state) {
    switch (state) {
        case STATE_BOOT:
            setColor(255, 255, 255);  // 白色
            break;
        case STATE_IDLE:
            _blinking = false;
            setColor(0, 100, 0);  // 暗绿
            break;
        case STATE_FOCUSING:
            _blinking = false;
            setColor(0, 255, 0);  // 绿色常亮
            break;
        case STATE_FOCUS_BREAK:
            _blinking = true;
            _blinkInterval = 1500;
            _ledR = 0; _ledG = 200; _ledB = 255;  // 青色慢闪
            break;
        case STATE_LISTENING:
            _blinking = true;
            _blinkInterval = 200;
            _ledR = 255; _ledG = 165; _ledB = 0;  // 橙色快闪
            break;
        case STATE_PROCESSING:
            _blinking = true;
            _blinkInterval = 500;
            _ledR = 0; _ledG = 0; _ledB = 255;  // 蓝色闪烁
            break;
        case STATE_SPEAKING:
            _blinking = false;
            setColor(255, 255, 0);  // 黄色常亮
            break;
        case STATE_ERROR:
            _blinking = true;
            _blinkInterval = 200;
            _ledR = 255; _ledG = 0; _ledB = 0;  // 红色快闪
            break;
    }
}

void DisplayManager::setColor(int r, int g, int b) {
    ledcWrite(PIN_LED_R, r);
    ledcWrite(PIN_LED_G, g);
    ledcWrite(PIN_LED_B, b);
}
