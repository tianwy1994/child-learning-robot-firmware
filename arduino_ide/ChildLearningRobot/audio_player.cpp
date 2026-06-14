#include "audio_player.h"

// 最大音频缓冲区（使用 PSRAM，可分配较大空间）
// 30 秒 * 16000 采样率 * 2 字节 = 960KB
#define AUDIO_MAX_BUFFER_SIZE (30 * AUDIO_SAMPLE_RATE * 2)

void AudioPlayer::begin() {
    Serial.println("[Audio] 初始化 I2S 音频播放器...");

    // 分配音频缓冲区（优先使用 PSRAM）
    _audioBuffer = (uint8_t*)ps_malloc(AUDIO_MAX_BUFFER_SIZE);
    if (_audioBuffer == nullptr) {
        // PSRAM 分配失败，尝试小一些的缓冲区
        _audioBuffer = (uint8_t*)malloc(64 * 1024);  // 64KB fallback
        if (_audioBuffer == nullptr) {
            Serial.println("[Audio] 错误：无法分配音频缓冲区！");
            return;
        }
        Serial.println("[Audio] 警告：PSRAM 分配失败，使用 64KB 缓冲区");
    } else {
        Serial.printf("[Audio] PSRAM 缓冲区已分配: %d bytes\n", AUDIO_MAX_BUFFER_SIZE);
    }

    // 初始化 I2S
    initI2S();

    Serial.println("[Audio] I2S 音频播放器初始化完成");
    Serial.printf("[Audio] 引脚: BCLK=%d, LRC=%d, DIN=%d\n",
                  PIN_I2S_BCLK, PIN_I2S_LRC, PIN_I2S_DIN);
}

void AudioPlayer::initI2S() {
    // I2S 配置
    i2s_config_t i2s_config = {};
    i2s_config.mode = (i2s_mode_t)(I2S_MODE_MASTER | I2S_MODE_TX);
    i2s_config.sample_rate = AUDIO_SAMPLE_RATE;
    i2s_config.bits_per_sample = I2S_BITS_PER_SAMPLE_16BIT;
    i2s_config.channel_format = I2S_CHANNEL_FMT_ONLY_LEFT;
    i2s_config.communication_format = I2S_COMM_FORMAT_STAND_I2S;
    i2s_config.intr_alloc_flags = ESP_INTR_FLAG_LEVEL1;
    i2s_config.dma_buf_count = AUDIO_DMA_BUFFER_COUNT;
    i2s_config.dma_buf_len = AUDIO_BUFFER_SIZE;
    i2s_config.use_apll = false;
    i2s_config.tx_desc_auto_clear = true;  // 播放结束自动清零

    // I2S 引脚配置
    i2s_pin_config_t pin_config = {};
    pin_config.bck_io_num = PIN_I2S_BCLK;
    pin_config.ws_io_num = PIN_I2S_LRC;
    pin_config.data_out_num = PIN_I2S_DIN;
    pin_config.data_in_num = I2S_PIN_NO_CHANGE;

    // 安装 I2S 驱动
    esp_err_t err = i2s_driver_install(I2S_NUM_0, &i2s_config, 0, nullptr);
    if (err != ESP_OK) {
        Serial.printf("[Audio] I2S 驱动安装失败: %d\n", err);
        return;
    }

    err = i2s_set_pin(I2S_NUM_0, &pin_config);
    if (err != ESP_OK) {
        Serial.printf("[Audio] I2S 引脚设置失败: %d\n", err);
        return;
    }

    // 清零 DMA 缓冲区
    i2s_zero_dma_buffer(I2S_NUM_0);

    Serial.println("[Audio] I2S 驱动安装成功");
}

void AudioPlayer::update() {
    if (!_playing || _audioBuffer == nullptr) {
        return;
    }

    // 检查是否播放完毕
    if (_audioOffset >= _audioSize) {
        _playing = false;
        // 短暂静音后停止
        i2s_zero_dma_buffer(I2S_NUM_0);
        Serial.println("[Audio] 播放完成");
        return;
    }

    // 计算本次写入的字节数
    size_t remaining = _audioSize - _audioOffset;
    size_t bytesToWrite = min(CHUNK_SIZE, remaining);

    // 复制数据块到临时缓冲区（避免修改原始数据）
    uint8_t chunk[CHUNK_SIZE];
    memcpy(chunk, _audioBuffer + _audioOffset, bytesToWrite);

    // 应用音量增益
    applyVolume(chunk, bytesToWrite);

    // 写入 I2S
    size_t bytesWritten = 0;
    esp_err_t err = i2s_write(I2S_NUM_0, chunk, bytesToWrite, &bytesWritten, 0);
    if (err != ESP_OK) {
        Serial.printf("[Audio] I2S 写入错误: %d\n", err);
        _playing = false;
        return;
    }

    _audioOffset += bytesWritten;
}

bool AudioPlayer::playText(HttpClient& httpClient, const String& text, const String& speed) {
    if (_audioBuffer == nullptr) {
        Serial.println("[Audio] 错误：音频缓冲区未分配");
        return false;
    }

    if (_playing) {
        stop();
    }

    // 构建请求 JSON
    String jsonBody = "{\"text\":\"" + text + "\",\"speed\":\"" + speed + "\"}";

    Serial.printf("[Audio] 请求 TTS: %s\n", text.substring(0, min(50, (int)text.length())).c_str());

    // 下载音频数据
    int bytesRead = httpClient.postBinary(API_TTS_SPEAK, jsonBody,
                                          _audioBuffer, AUDIO_MAX_BUFFER_SIZE);

    if (bytesRead <= 0) {
        Serial.println("[Audio] TTS 请求失败");
        return false;
    }

    _audioSize = bytesRead;
    _audioOffset = 0;
    _playing = true;

    Serial.printf("[Audio] 开始播放: %d bytes\n", bytesRead);
    return true;
}

bool AudioPlayer::playPreset(HttpClient& httpClient, const String& name) {
    if (_audioBuffer == nullptr) {
        Serial.println("[Audio] 错误：音频缓冲区未分配");
        return false;
    }

    if (_playing) {
        stop();
    }

    String path = String(API_TTS_PRESET) + "/" + name;

    Serial.printf("[Audio] 请求预置语音: %s\n", name.c_str());

    int bytesRead = httpClient.getBinary(path, _audioBuffer, AUDIO_MAX_BUFFER_SIZE);

    if (bytesRead <= 0) {
        Serial.println("[Audio] 预置语音请求失败");
        return false;
    }

    _audioSize = bytesRead;
    _audioOffset = 0;
    _playing = true;

    Serial.printf("[Audio] 开始播放预置语音 '%s': %d bytes\n", name.c_str(), bytesRead);
    return true;
}

void AudioPlayer::playPcm(const uint8_t* data, size_t size) {
    if (_audioBuffer == nullptr) {
        Serial.println("[Audio] 错误：音频缓冲区未分配");
        return;
    }

    if (_playing) {
        stop();
    }

    size_t copySize = min(size, (size_t)AUDIO_MAX_BUFFER_SIZE);
    memcpy(_audioBuffer, data, copySize);

    _audioSize = copySize;
    _audioOffset = 0;
    _playing = true;

    Serial.printf("[Audio] 开始播放本地 PCM: %d bytes\n", copySize);
}

void AudioPlayer::stop() {
    _playing = false;
    _audioOffset = 0;
    _audioSize = 0;
    i2s_zero_dma_buffer(I2S_NUM_0);
    Serial.println("[Audio] 播放已停止");
}

bool AudioPlayer::isPlaying() {
    return _playing;
}

void AudioPlayer::setVolume(int volume) {
    _volume = constrain(volume, 0, 100);
    Serial.printf("[Audio] 音量设置为: %d%%\n", _volume);
}

void AudioPlayer::applyVolume(uint8_t* data, size_t size) {
    if (_volume == 100) return;  // 满音量不需要处理
    if (_volume == 0) {
        memset(data, 0, size);
        return;
    }

    // 对 16-bit PCM 样本应用增益
    float gain = _volume / 100.0f;
    int16_t* samples = (int16_t*)data;
    size_t sampleCount = size / 2;

    for (size_t i = 0; i < sampleCount; i++) {
        samples[i] = (int16_t)(samples[i] * gain);
    }
}
