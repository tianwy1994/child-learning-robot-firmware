#ifndef BUTTON_MANAGER_H
#define BUTTON_MANAGER_H

#include <Arduino.h>
#include "config.h"

class ButtonManager {
public:
    void begin();
    void update();

    typedef void (*ButtonEventCallback)();
    void onShortPress(ButtonEventCallback callback);
    void onLongPress(ButtonEventCallback callback);
    void onPressed(ButtonEventCallback callback);
    void onReleased(ButtonEventCallback callback);

    bool isPressed();
    unsigned long pressDuration();

private:
    bool _lastRawState = HIGH;
    bool _debouncedState = HIGH;
    unsigned long _lastDebounceTime = 0;
    unsigned long _pressStartTime = 0;
    bool _longPressFired = false;

    ButtonEventCallback _shortPressCb = nullptr;
    ButtonEventCallback _longPressCb = nullptr;
    ButtonEventCallback _pressedCb = nullptr;
    ButtonEventCallback _releasedCb = nullptr;
};

#endif // BUTTON_MANAGER_H
