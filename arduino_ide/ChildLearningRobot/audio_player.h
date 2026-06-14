#ifndef AUDIO_PLAYER_H
#define AUDIO_PLAYER_H

#include <Arduino.h>
#include <driver/i2s.h>
#include "config.h"
#include "http_client.h"

/**
 * I2S 音频播放器 —— 通过 MAX98357A 功放模块驱动喇叭。
 *
 * 功能：
 *   1. 从服务端获取 TTS 音频（PCM 格式）
 *   2. 通过 I2S 接口输出到 MAX98357A 功放
 *   3. 支持播放预置语音和实时合成语音
 *   4. 非阻塞设计，播放过程中可执行其他任务
 *
 * 硬件接线：
 *   ESP32-S3 GPIO 41 → MAX98357A BCLK
 *   ESP32-S3 GPIO 42 → MAX98357A LRC
 *   ESP32-S3 GPIO 43 → MAX98357A DIN
 *   ESP32-S3 GND    → MAX98357A GND
 *   ESP32-S3 5V     → MAX98357A VIN
 *   MAX98357A Speaker+/Speaker- → 喇叭
 */
class AudioPlayer {
public:
    /**
     * 初始化 I2S 驱动。
     * 必须在 setup() 中调用。
     */
    void begin();

    /**
     * 更新播放状态（非阻塞分块发送）。
     * 必须在 loop() 中持续调用。
     */
    void update();

    /**
     * 通过 HTTP 请求服务端 TTS 接口获取音频并播放。
     *
     * @param httpClient  HTTP 客户端（需已设置 token）
     * @param text        要合成的文本
     * @param speed       语速：slow / normal / fast
     * @return true=请求成功开始播放, false=请求失败
     */
    bool playText(HttpClient& httpClient, const String& text, const String& speed = "normal");

    /**
     * 播放预置语音。
     *
     * @param httpClient  HTTP 客户端
     * @param name        预置名称（greeting, encourage 等）
     * @return true=请求成功开始播放, false=请求失败
     */
    bool playPreset(HttpClient& httpClient, const String& name);

    /**
     * 直接播放已有的 PCM 数据。
     *
     * @param data  PCM 16-bit 16kHz mono 数据
     * @param size  数据大小（字节）
     */
    void playPcm(const uint8_t* data, size_t size);

    /**
     * 停止当前播放。
     */
    void stop();

    /**
     * 是否正在播放。
     */
    bool isPlaying();

    /**
     * 设置音量（0-100）。
     * 通过软件增益实现，不影响 I2S 硬件音量。
     */
    void setVolume(int volume);

private:
    // I2S 缓冲区（使用 PSRAM 分配大缓冲区）
    uint8_t* _audioBuffer = nullptr;
    size_t _audioSize = 0;
    size_t _audioOffset = 0;
    bool _playing = false;
    int _volume = 80;  // 默认音量 80%

    // 分块发送参数
    static const size_t CHUNK_SIZE = 1024;  // 每次 I2S 写入的字节数

    /**
     * 配置并安装 I2S 驱动。
     */
    void initI2S();

    /**
     * 应用音量增益到音频数据。
     */
    void applyVolume(uint8_t* data, size_t size);
};

#endif // AUDIO_PLAYER_H
