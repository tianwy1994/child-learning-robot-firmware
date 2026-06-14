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
#define _STRINGIFY(x)       #x
#define _TOSTRING(x)        _STRINGIFY(x)
#define SERVER_BASE_URL     "http://" SERVER_HOST ":" _TOSTRING(SERVER_PORT)

// 认证服务地址（用于 token 验证）
#define AUTH_SERVER_HOST    "192.168.1.100"
#define AUTH_SERVER_PORT    8081

// ============================================================
// API 路径
// ============================================================
#define API_CHAT_SEND           "/api/hardware/chat/send"
#define API_TTS_SPEAK           "/api/hardware/tts/speak"
#define API_TTS_PRESET          "/api/hardware/tts/preset"
#define API_FOCUS_START         "/api/hardware/focus/start"
#define API_FOCUS_END           "/api/hardware/focus/end"
#define API_FOCUS_STATUS        "/api/hardware/focus/status"
#define API_FOCUS_REMINDER_ACK  "/api/hardware/focus/reminder/ack"
#define API_HOMEWORK_SUBMIT     "/api/hardware/homework/submit"
#define API_HOMEWORK_OCR        "/api/hardware/homework/ocr"
#define API_GAME_CHECKIN        "/api/hardware/game/checkin"
#define API_GAME_PROFILE        "/api/hardware/game/profile"
#define API_STT_RECOGNIZE       "/api/hardware/stt/recognize"

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
// I2S 音频输出引脚（MAX98357A 功放 → 喇叭）
// ============================================================
#define PIN_I2S_BCLK        41      // I2S 位时钟
#define PIN_I2S_LRC         42      // I2S 左右声道时钟
#define PIN_I2S_DIN         43      // I2S 数据输出

// ============================================================
// I2S 音频输入引脚（INMP441 麦克风）
// ============================================================
#define PIN_MIC_SCK         44      // 麦克风时钟
#define PIN_MIC_WS          45      // 麦克风字选择
#define PIN_MIC_SD          46      // 麦克风数据

// ============================================================
// 音频配置
// ============================================================
#define AUDIO_SAMPLE_RATE       16000   // 采样率 16kHz
#define AUDIO_BITS_PER_SAMPLE   16      // 位深 16-bit
#define AUDIO_BUFFER_SIZE       1024    // DMA 缓冲区大小
#define AUDIO_DMA_BUFFER_COUNT  8       // DMA 缓冲区数量

// 录音配置
#define MIC_RECORD_MAX_SECONDS  15      // 最长录音 15 秒
#define MIC_RECORD_BUFFER_SIZE  (MIC_RECORD_MAX_SECONDS * AUDIO_SAMPLE_RATE * 2)  // 16-bit mono

// ============================================================
// 按钮引脚
// ============================================================
#define PIN_BUTTON              0       // 启动按钮（GPIO 0）
#define BUTTON_DEBOUNCE_MS      50      // 消抖时间
#define BUTTON_SHORT_PRESS_MS   1000    // 短按阈值（< 1秒）
#define BUTTON_LONG_PRESS_MS    3000    // 长按阈值（> 3秒）

// ============================================================
// 状态机配置
// ============================================================
#define SENSOR_UPDATE_INTERVAL  1000    // 传感器更新间隔 (ms)
#define HTTP_TIMEOUT_MS         15000   // HTTP 请求超时
#define STATUS_CHECK_INTERVAL   30000   // 专注状态轮询间隔 (ms)

// ============================================================
// 认证状态
// ============================================================
enum AuthState {
    AUTH_LOCKED,        // 未登录，设备不可用
    AUTH_AUTHENTICATED, // 已认证，正常工作
    AUTH_EXPIRED        // token 过期
};

// ============================================================
// 设备状态（学习陪伴机器人）
// ============================================================
enum DeviceState {
    STATE_BOOT,             // 启动中
    STATE_IDLE,             // 空闲，等待指令
    STATE_FOCUSING,         // 专注学习中
    STATE_FOCUS_BREAK,      // 学习休息中
    STATE_LISTENING,        // 录音中（语音输入）
    STATE_PROCESSING,       // 等待服务器响应（STT/Chat/TTS）
    STATE_SPEAKING,         // 播放语音回复
    STATE_ERROR             // 错误状态
};

// ============================================================
// 聊天角色
// ============================================================
enum ChatRole {
    ROLE_COMPANION,     // 学习陪伴
    ROLE_GRADER,        // 作业批改
    ROLE_EXPLAINER      // 错题讲解
};

#endif // CONFIG_H
