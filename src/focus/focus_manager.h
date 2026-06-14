#ifndef FOCUS_MANAGER_H
#define FOCUS_MANAGER_H

#include <Arduino.h>
#include "../include/config.h"

class HttpClient;
class AudioPlayer;

/**
 * 专注模式管理器 —— 管理学习专注会话和提醒。
 *
 * 功能：
 *   1. 开始/结束专注会话
 *   2. 定期轮询专注状态
 *   3. 处理提醒（坐姿、休息、继续学习）
 *   4. 通过 TTS 播放提醒语音
 *
 * 接口：
 *   POST /api/hardware/focus/start
 *   POST /api/hardware/focus/end
 *   GET  /api/hardware/focus/status
 *   POST /api/hardware/focus/reminder/ack
 */
class FocusManager {
public:
    /**
     * 开始专注。
     * @param taskDescription 任务描述
     * @return true=成功
     */
    bool startFocus(HttpClient& httpClient, const String& taskDescription = "学习");

    /**
     * 结束专注。
     * @return true=成功
     */
    bool endFocus(HttpClient& httpClient);

    /**
     * 轮询专注状态（每 30 秒调用一次）。
     * 如果有待播放的提醒，会自动通过 TTS 播放并确认。
     *
     * @return 是否有活跃的专注会话
     */
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
