#ifndef DISPLAY_MANAGER_H
#define DISPLAY_MANAGER_H

#include <Arduino.h>
#include "config.h"

class DisplayManager {
public:
    void begin();
    void update();

    void showLocked();
    void showAuthenticated();
    void showExpired();
    void showWiFiConnecting();
    void showWiFiConnected();
    void showStarting();
    void showOff();
    void showDeviceState(DeviceState state);

private:
    void setColor(int r, int g, int b);

    int _ledR = 0, _ledG = 0, _ledB = 0;
    bool _blinking = false;
    int _blinkInterval = 500;
    unsigned long _lastBlink = 0;
    bool _blinkState = false;
};

#endif // DISPLAY_MANAGER_H
