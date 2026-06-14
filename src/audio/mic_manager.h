#ifndef MIC_MANAGER_H
#define MIC_MANAGER_H

#include <Arduino.h>
#include <driver/i2s.h>
#include "../include/config.h"

/**
 * 麦克风管理器 —— 通过 I2S 接口从 INMP441 麦克风录音。
 *
 * 硬件接线（INMP441 → ESP32-S3）：
 *   SCK  → GPIO 44
 *   WS   → GPIO 45
 *   SD   → GPIO 46
 *   VDD  → 3.3V
 *   GND  → GND
 *   L/R  → GND（左声道）
 *
 * 使用 I2S_NUM_1（与喇叭的 I2S_NUM_0 独立，可同时工作）。
 */
class MicManager {
public:
    ~MicManager();

    bool begin();
    void stop();

    /**
     * 开始录音。
     * @return true=成功开始
     */
    bool startRecording();

    /**
     * 停止录音。
     * @return 录制的音频数据指针
     */
    const uint8_t* stopRecording(size_t& outSize);

    /**
     * 在 loop() 中持续调用，从 I2S 读取音频数据到缓冲区。
     * 录音时返回 true，缓冲区满或未录音时返回 false。
     */
    bool update();

    bool isRecording();
    bool isBufferFull();
    size_t getRecordedSize();

    /**
     * 获取已录制的音频数据（录音进行中也可调用）。
     */
    const uint8_t* getRecordedData();

private:
    bool _initialized = false;
    bool _recording = false;
    uint8_t* _buffer = nullptr;
    size_t _bufferSize = 0;
    size_t _writeOffset = 0;

    void initI2S();
    void deinitI2S();
};

#endif // MIC_MANAGER_H
