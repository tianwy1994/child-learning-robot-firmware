#include "mic_manager.h"

MicManager::~MicManager() {
    stop();
    if (_buffer) { free(_buffer); _buffer = nullptr; }
}

bool MicManager::begin() {
    Serial.println("[Mic] 初始化麦克风...");
    _bufferSize = MIC_RECORD_BUFFER_SIZE;
    _buffer = (uint8_t*)ps_malloc(_bufferSize);
    if (_buffer == nullptr) {
        _buffer = (uint8_t*)malloc(64 * 1024);
        if (_buffer == nullptr) {
            Serial.println("[Mic] 错误：无法分配录音缓冲区");
            return false;
        }
        _bufferSize = 64 * 1024;
        Serial.printf("[Mic] 警告：PSRAM 分配失败，使用 %d KB 缓冲区\n", _bufferSize / 1024);
    } else {
        Serial.printf("[Mic] PSRAM 缓冲区已分配: %d bytes (%d 秒)\n",
                      _bufferSize, _bufferSize / (AUDIO_SAMPLE_RATE * 2));
    }
    Serial.printf("[Mic] 引脚: SCK=%d, WS=%d, SD=%d\n", PIN_MIC_SCK, PIN_MIC_WS, PIN_MIC_SD);
    return true;
}

void MicManager::initI2S() {
    i2s_config_t i2s_config = {};
    i2s_config.mode = (i2s_mode_t)(I2S_MODE_MASTER | I2S_MODE_RX);
    i2s_config.sample_rate = AUDIO_SAMPLE_RATE;
    i2s_config.bits_per_sample = I2S_BITS_PER_SAMPLE_16BIT;
    i2s_config.channel_format = I2S_CHANNEL_FMT_ONLY_LEFT;
    i2s_config.communication_format = I2S_COMM_FORMAT_STAND_I2S;
    i2s_config.intr_alloc_flags = ESP_INTR_FLAG_LEVEL1;
    i2s_config.dma_buf_count = AUDIO_DMA_BUFFER_COUNT;
    i2s_config.dma_buf_len = AUDIO_BUFFER_SIZE;
    i2s_config.use_apll = false;

    i2s_pin_config_t pin_config = {};
    pin_config.bck_io_num = PIN_MIC_SCK;
    pin_config.ws_io_num = PIN_MIC_WS;
    pin_config.data_out_num = I2S_PIN_NO_CHANGE;
    pin_config.data_in_num = PIN_MIC_SD;

    esp_err_t err = i2s_driver_install(I2S_NUM_1, &i2s_config, 0, nullptr);
    if (err != ESP_OK) { Serial.printf("[Mic] I2S 驱动安装失败: %d\n", err); return; }
    err = i2s_set_pin(I2S_NUM_1, &pin_config);
    if (err != ESP_OK) { Serial.printf("[Mic] I2S 引脚设置失败: %d\n", err); i2s_driver_uninstall(I2S_NUM_1); return; }
    i2s_zero_dma_buffer(I2S_NUM_1);
    _initialized = true;
    Serial.println("[Mic] I2S 输入驱动安装成功");
}

void MicManager::deinitI2S() {
    if (_initialized) { i2s_driver_uninstall(I2S_NUM_1); _initialized = false; }
}

void MicManager::stop() {
    if (_recording) { size_t d; stopRecording(d); }
    deinitI2S();
}

bool MicManager::startRecording() {
    if (!_buffer) { Serial.println("[Mic] 错误：缓冲区未分配"); return false; }
    if (_recording) { size_t d; stopRecording(d); }
    if (!_initialized) initI2S();
    if (!_initialized) return false;
    _writeOffset = 0;
    _recording = true;
    Serial.println("[Mic] 开始录音...");
    return true;
}

const uint8_t* MicManager::stopRecording(size_t& outSize) {
    if (!_recording) { outSize = 0; return nullptr; }
    _recording = false;
    outSize = _writeOffset;
    Serial.printf("[Mic] 录音结束，共 %d bytes (%.1f 秒)\n",
                  _writeOffset, _writeOffset / (float)(AUDIO_SAMPLE_RATE * 2));
    deinitI2S();
    return _buffer;
}

bool MicManager::update() {
    if (!_recording || !_initialized || !_buffer) return false;
    if (_writeOffset >= _bufferSize) {
        Serial.println("[Mic] 缓冲区已满，自动停止录音");
        _recording = false;
        return false;
    }
    size_t bytesToRead = min((size_t)AUDIO_BUFFER_SIZE * 2, _bufferSize - _writeOffset);
    size_t bytesRead = 0;
    esp_err_t err = i2s_read(I2S_NUM_1, _buffer + _writeOffset, bytesToRead, &bytesRead, 0);
    if (err == ESP_OK && bytesRead > 0) _writeOffset += bytesRead;
    return true;
}

bool MicManager::isRecording() { return _recording; }
bool MicManager::isBufferFull() { return _writeOffset >= _bufferSize; }
size_t MicManager::getRecordedSize() { return _writeOffset; }
const uint8_t* MicManager::getRecordedData() { return _buffer; }
