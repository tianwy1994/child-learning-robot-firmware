#include "display_manager.h"

void DisplayManager::begin() {
    ledcAttach(PIN_LED_R, 5000, 8);
    ledcAttach(PIN_LED_G, 5000, 8);
    ledcAttach(PIN_LED_B, 5000, 8);
    showOff();
    Serial.println("[Display] LED 初始化完成");
}

void DisplayManager::update() {
    if (_blinking && millis() - _lastBlink > _blinkInterval) {
        _lastBlink = millis();
        _blinkState = !_blinkState;
        if (_blinkState) setColor(_ledR, _ledG, _ledB);
        else setColor(0, 0, 0);
    }
}

void DisplayManager::showLocked() {
    _blinking = true; _blinkInterval = 500;
    _ledR = 255; _ledG = 0; _ledB = 0;
}

void DisplayManager::showAuthenticated() {
    _blinking = false; setColor(0, 255, 0);
}

void DisplayManager::showExpired() {
    _blinking = true; _blinkInterval = 1000;
    _ledR = 255; _ledG = 255; _ledB = 0;
}

void DisplayManager::showWiFiConnecting() {
    _blinking = true; _blinkInterval = 300;
    _ledR = 0; _ledG = 0; _ledB = 255;
}

void DisplayManager::showWiFiConnected() {
    _blinking = false; setColor(0, 0, 255);
}

void DisplayManager::showStarting() {
    _blinking = false; setColor(255, 255, 255);
}

void DisplayManager::showOff() {
    _blinking = false; setColor(0, 0, 0);
}

void DisplayManager::showDeviceState(DeviceState state) {
    switch (state) {
        case STATE_BOOT:
            setColor(255, 255, 255);
            break;
        case STATE_IDLE:
            _blinking = false; setColor(0, 100, 0);
            break;
        case STATE_FOCUSING:
            _blinking = false; setColor(0, 255, 0);
            break;
        case STATE_FOCUS_BREAK:
            _blinking = true; _blinkInterval = 1500;
            _ledR = 0; _ledG = 200; _ledB = 255;
            break;
        case STATE_LISTENING:
            _blinking = true; _blinkInterval = 200;
            _ledR = 255; _ledG = 165; _ledB = 0;
            break;
        case STATE_PROCESSING:
            _blinking = true; _blinkInterval = 500;
            _ledR = 0; _ledG = 0; _ledB = 255;
            break;
        case STATE_SPEAKING:
            _blinking = false; setColor(255, 255, 0);
            break;
        case STATE_ERROR:
            _blinking = true; _blinkInterval = 200;
            _ledR = 255; _ledG = 0; _ledB = 0;
            break;
    }
}

void DisplayManager::setColor(int r, int g, int b) {
    ledcWrite(PIN_LED_R, r);
    ledcWrite(PIN_LED_G, g);
    ledcWrite(PIN_LED_B, b);
}
