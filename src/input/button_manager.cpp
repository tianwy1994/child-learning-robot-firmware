#include "button_manager.h"

void ButtonManager::begin() {
    pinMode(PIN_BUTTON, INPUT_PULLUP);
    Serial.println("[Button] 按钮初始化完成 (GPIO " _STRINGIFY(PIN_BUTTON) ")");
}

void ButtonManager::update() {
    bool rawState = digitalRead(PIN_BUTTON);

    // 消抖处理
    if (rawState != _lastRawState) {
        _lastDebounceTime = millis();
    }
    _lastRawState = rawState;

    if (millis() - _lastDebounceTime < BUTTON_DEBOUNCE_MS) {
        return;
    }

    // 消抖后的稳定状态
    bool pressed = (rawState == LOW);  // 按钮按下时为 LOW（上拉）

    // 按下瞬间
    if (pressed && _debouncedState == HIGH) {
        _debouncedState = LOW;
        _pressStartTime = millis();
        _longPressFired = false;
        if (_pressedCb) _pressedCb();
    }
    // 松开瞬间
    else if (!pressed && _debouncedState == LOW) {
        _debouncedState = HIGH;
        unsigned long duration = millis() - _pressStartTime;

        if (!_longPressFired && duration < BUTTON_SHORT_PRESS_MS) {
            // 短按
            Serial.printf("[Button] 短按 (%lu ms)\n", duration);
            if (_shortPressCb) _shortPressCb();
        }

        if (_releasedCb) _releasedCb();
    }
    // 持续按住 — 检测长按
    else if (pressed && !_longPressFired) {
        if (millis() - _pressStartTime >= BUTTON_LONG_PRESS_MS) {
            _longPressFired = true;
            Serial.println("[Button] 长按检测");
            if (_longPressCb) _longPressCb();
        }
    }
}

bool ButtonManager::isPressed() {
    return _debouncedState == LOW;
}

unsigned long ButtonManager::pressDuration() {
    if (_debouncedState == LOW) {
        return millis() - _pressStartTime;
    }
    return 0;
}

void ButtonManager::onShortPress(ButtonEventCallback callback) {
    _shortPressCb = callback;
}

void ButtonManager::onLongPress(ButtonEventCallback callback) {
    _longPressCb = callback;
}

void ButtonManager::onPressed(ButtonEventCallback callback) {
    _pressedCb = callback;
}

void ButtonManager::onReleased(ButtonEventCallback callback) {
    _releasedCb = callback;
}
