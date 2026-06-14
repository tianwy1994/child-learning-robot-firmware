#ifndef MIC_MANAGER_H
#define MIC_MANAGER_H

#include <Arduino.h>
#include <driver/i2s.h>
#include "config.h"

class MicManager {
public:
    ~MicManager();
    bool begin();
    void stop();
    bool startRecording();
    const uint8_t* stopRecording(size_t& outSize);
    bool update();
    bool isRecording();
    bool isBufferFull();
    size_t getRecordedSize();
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
