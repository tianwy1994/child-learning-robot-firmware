#include "button_manager.h"

void ButtonManager::begin() {
    pinMode(PIN_BUTTON, INPUT_PULLUP);
    Serial.println("[Button] 按钮初始化完成 (GPIO 0)");
}

void ButtonManager::update() {
    bool rawState = digitalRead(PIN_BUTTON);

    if (rawState != _lastRawState) {
        _lastDebounceTime = millis();
    }
    _lastRawState = rawState;

    if (millis() - _lastDebounceTime < BUTTON_DEBOUNCE_MS) {
        return;
    }

    bool pressed = (rawState == LOW);

    if (pressed && _debouncedState == HIGH) {
        _debouncedState = LOW;
        _pressStartTime = millis();
        _longPressFired = false;
        if (_pressedCb) _pressedCb();
    }
    else if (!pressed && _debouncedState == LOW) {
        _debouncedState = HIGH;
        unsigned long duration = millis() - _pressStartTime;

        if (!_longPressFired && duration < BUTTON_SHORT_PRESS_MS) {
            Serial.printf("[Button] 短按 (%lu ms)\n", duration);
            if (_shortPressCb) _shortPressCb();
        }

        if (_releasedCb) _releasedCb();
    }
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

void ButtonManager::onShortPress(ButtonEventCallback callback) { _shortPressCb = callback; }
void ButtonManager::onLongPress(ButtonEventCallback callback) { _longPressCb = callback; }
void ButtonManager::onPressed(ButtonEventCallback callback) { _pressedCb = callback; }
void ButtonManager::onReleased(ButtonEventCallback callback) { _releasedCb = callback; }
