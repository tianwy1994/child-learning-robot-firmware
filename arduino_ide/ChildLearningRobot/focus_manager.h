#ifndef FOCUS_MANAGER_H
#define FOCUS_MANAGER_H

#include <Arduino.h>
#include "config.h"

class HttpClient;
class AudioPlayer;

class FocusManager {
public:
    bool startFocus(HttpClient& httpClient, const String& taskDescription = "学习");
    bool endFocus(HttpClient& httpClient);
    bool pollStatus(HttpClient& httpClient, AudioPlayer& audioPlayer);
    bool isFocusing();
    bool isInBreak();

private:
    bool _focusing = false;
    bool _inBreak = false;
    unsigned long _lastPollTime = 0;
    void ackReminder(HttpClient& httpClient, const String& type);
};

#endif // FOCUS_MANAGER_H
