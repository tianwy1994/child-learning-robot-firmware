#ifndef CONFIG_H
#define CONFIG_H

// ============================================================
// WiFi 配置
// ============================================================
#define WIFI_SSID           "YOUR_WIFI_SSID"
#define WIFI_PASSWORD       "YOUR_WIFI_PASSWORD"
#define WIFI_TIMEOUT_MS     10000
#define WIFI_RETRY_DELAY_MS 5000
#define WIFI_MAX_RETRIES    5

// ============================================================
// 服务器配置
// ============================================================
#define SERVER_HOST         "192.168.1.100"   // 硬件服务端 IP
#define SERVER_PORT         8080
#define SERVER_BASE_URL     "http://" SERVER_HOST ":" #SERVER_PORT

// 认证服务地址（用于 token 验证）
#define AUTH_SERVER_HOST    "192.168.1.100"
#define AUTH_SERVER_PORT    8081

// ============================================================
// BLE 配置
// ============================================================
#define BLE_DEVICE_NAME     "ChildLearningRobot"
#define BLE_SERVICE_UUID    "0000FFE0-0000-1000-8000-00805F9B34FB"
#define BLE_TOKEN_CHAR_UUID "0000FFE1-0000-1000-8000-00805F9B34FB"

// ============================================================
// NVS 配置（Token 存储）
// ============================================================
#define NVS_NAMESPACE       "auth"
#define NVS_KEY_TOKEN       "device_token"
#define NVS_KEY_DEVICE_ID   "device_id"

// ============================================================
// 传感器引脚
// ============================================================
#define PIN_DHT             4       // DHT22 温湿度传感器
#define PIN_PRESSURE        5       // 压力传感器（ADC）
#define PIN_BATTERY         6       // 电池电压检测（ADC）

// ============================================================
// 执行器引脚
// ============================================================
#define PIN_MOTOR_BASE      10      // 电机起始引脚 (10-17, 共8个)
#define PIN_PUMP_WATER      18      // 清水泵
#define PIN_PUMP_SCRUB      19      // 搓泥宝泵
#define PIN_PUMP_GEL        20      // 沐浴露泵
#define PIN_LED_R           21      // RGB LED 红
#define PIN_LED_G           47      // RGB LED 绿
#define PIN_LED_B           48      // RGB LED 蓝

// ============================================================
// I2S 音频引脚（MAX98357A 功放模块）
// ============================================================
#define PIN_I2S_BCLK        41      // I2S 位时钟
#define PIN_I2S_LRC         42      // I2S 左右声道时钟
#define PIN_I2S_DIN         43      // I2S 数据输出

// ============================================================
// 音频配置
// ============================================================
#define AUDIO_SAMPLE_RATE       16000   // 采样率 16kHz
#define AUDIO_BITS_PER_SAMPLE   16      // 位深 16-bit
#define AUDIO_BUFFER_SIZE       1024    // DMA 缓冲区大小
#define AUDIO_DMA_BUFFER_COUNT  8       // DMA 缓冲区数量
#define AUDIO_TTS_API_PATH      "/api/hardware/tts/speak"
#define AUDIO_PRESET_API_PATH   "/api/hardware/tts/preset"

// ============================================================
// 按钮引脚
// ============================================================
#define PIN_BUTTON          0       // 启动按钮（GPIO 0）
#define BUTTON_DEBOUNCE_MS  50
#define BUTTON_LONG_PRESS   3000    // 长按 3 秒紧急停止

// ============================================================
// 状态机配置
// ============================================================
#define SENSOR_UPDATE_INTERVAL  1000    // 传感器更新间隔 (ms)
#define HTTP_TIMEOUT_MS         15000   // HTTP 请求超时
#define STATUS_CHECK_INTERVAL   30000   // 状态检查间隔 (ms)

// ============================================================
// 认证状态
// ============================================================
enum AuthState {
    AUTH_LOCKED,        // 未登录，设备不可用
    AUTH_AUTHENTICATED, // 已认证，正常工作
    AUTH_EXPIRED        // token 过期
};

// ============================================================
// 设备状态
// ============================================================
enum DeviceState {
    STATE_STANDBY,
    STATE_DETECTION,
    STATE_WETTING,
    STATE_APPLY_SCRUB,
    STATE_SCRUBBING,
    STATE_APPLY_GEL,
    STATE_RINSING,
    STATE_COMPLETED,
    STATE_EMERGENCY_STOP,
    STATE_ERROR
};

#endif // CONFIG_H
