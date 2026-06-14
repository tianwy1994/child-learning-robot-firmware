#ifndef GAME_MANAGER_H
#define GAME_MANAGER_H

#include <Arduino.h>
#include "config.h"

class HttpClient;
class AudioPlayer;

class GameManager {
public:
    bool dailyCheckin(HttpClient& httpClient, AudioPlayer& audioPlayer);
    void announceProfile(HttpClient& httpClient, AudioPlayer& audioPlayer);
    bool hasCheckedInToday();

private:
    bool _checkedInToday = false;
    unsigned long _lastCheckinDay = 0;
    bool isNewDay();
};

#endif // GAME_MANAGER_H
