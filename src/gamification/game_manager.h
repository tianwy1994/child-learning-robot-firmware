#ifndef GAME_MANAGER_H
#define GAME_MANAGER_H

#include <Arduino.h>
#include "../include/config.h"

class HttpClient;
class AudioPlayer;

/**
 * 游戏化管理器 —— 每日打卡、积分、成就。
 *
 * 接口：
 *   POST /api/hardware/game/checkin  — 每日打卡
 *   GET  /api/hardware/game/profile  — 获取游戏化档案
 */
class GameManager {
public:
    /**
     * 每日打卡（WiFi 连接后自动调用，每天只打一次）。
     * @return true=打卡成功或已打卡
     */
    bool dailyCheckin(HttpClient& httpClient, AudioPlayer& audioPlayer);

    /**
     * 获取游戏化档案并通过语音播报。
     */
    void announceProfile(HttpClient& httpClient, AudioPlayer& audioPlayer);

    bool hasCheckedInToday();

private:
    bool _checkedInToday = false;
    unsigned long _lastCheckinDay = 0;

    bool isNewDay();
};

#endif // GAME_MANAGER_H
