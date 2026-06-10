# 小智同学 — ESP32-S3 硬件固件

## 概述

ESP32-S3 固件项目，实现设备认证、WiFi 联网、BLE 蓝牙通信、I2S 语音播报、每日打卡、专注提醒等功能。

## 构建环境

- **IDE**: PlatformIO (VS Code 插件)
- **框架**: Arduino
- **目标板**: ESP32-S3-DevKitC-1

## 快速开始

```bash
# 编译
pio run -e esp32s3

# 上传到设备
pio run -e esp32s3 -t upload

# 串口监视器
pio device monitor -e esp32s3 --baud 115200

# 运行测试
pio test -e esp32s3
```

## 配置

编辑 `include/config.h` 修改以下配置：

### WiFi 配置
```cpp
#define WIFI_SSID       "YOUR_WIFI_SSID"
#define WIFI_PASSWORD   "YOUR_WIFI_PASSWORD"
```

### 服务器配置
```cpp
#define SERVER_HOST     "192.168.1.100"   // 硬件服务端 IP
#define SERVER_PORT     8080
```

### 音频配置
```cpp
#define PIN_I2S_BCLK    41      // I2S 位时钟
#define PIN_I2S_LRC     42      // I2S 左右声道时钟
#define PIN_I2S_DIN     43      // I2S 数据输出
#define AUDIO_SAMPLE_RATE 16000 // 采样率
```

## 设备启动流程

```
1. 初始化串口、LED
2. 初始化认证管理器（从 NVS 读取 token）
3. 初始化 BLE 服务（等待 APP 发送 token）
4. 初始化 WiFi（连接路由器）
5. 初始化 HTTP 客户端
6. 初始化 I2S 音频播放器
7. WiFi 连接成功后播放开机问候语
8. 首次开机自动调用每日打卡 API
9. 主循环：音频更新 → 心跳 → 提醒检查
```

## 认证流程

### 首次使用

1. 设备开机，LED 红色闪烁（未登录状态）
2. 打开手机 APP，登录账号
3. 在 APP 中扫描设备二维码或输入设备 ID
4. APP 通过蓝牙发送 token 到设备
5. 设备接收 token，存入 NVS，LED 变为绿色常亮
6. 设备可以正常使用

### 日常使用

1. 设备开机，从 NVS 读取 token
2. 如果 token 有效，LED 绿色常亮，直接进入工作状态
3. 如果 token 过期，LED 黄色闪烁，需要重新绑定

## LED 状态指示

| 颜色 | 状态 | 含义 |
|------|------|------|
| 红色闪烁 | LOCKED | 未登录，需要绑定 |
| 绿色常亮 | AUTHENTICATED | 已认证，正常工作 |
| 黄色闪烁 | EXPIRED | token 过期，需要重新绑定 |
| 蓝色闪烁 | WiFi 连接中 | 正在连接 WiFi |
| 蓝色常亮 | WiFi 已连接 | WiFi 连接成功 |
| 白色 | 启动中 | 系统初始化 |

## 项目结构

```
src/
├── main.cpp              # 主程序入口
├── auth/
│   ├── auth_manager.h/cpp    # 认证管理器（NVS token 存储）
├── ble/
│   ├── ble_server.h/cpp      # BLE 服务端（接收 APP token）
├── network/
│   ├── wifi_manager.h/cpp    # WiFi 连接管理
│   ├── http_client.h/cpp     # HTTP 客户端（含二进制下载）
├── display/
│   ├── display_manager.h/cpp # RGB LED 状态显示
├── audio/
│   ├── audio_player.h/cpp    # I2S 音频播放器
├── sensors/              # 传感器驱动（待实现）
├── actuators/            # 执行器驱动（待实现）
└── state/                # 状态机（待实现）
```

## API 接口

设备请求硬件服务端时自动携带 `Authorization: Bearer <token>` header。

### 硬件接口（需要设备 token）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/hardware/chat/send | AI 对话 |
| POST | /api/hardware/focus/start | 开始专注 |
| POST | /api/hardware/focus/end | 结束专注 |
| GET  | /api/hardware/focus/status | 专注状态（含提醒） |
| POST | /api/hardware/focus/reminder/ack | 确认提醒已播放 |
| POST | /api/hardware/homework/submit | 提交作业 |
| POST | /api/hardware/tts/speak | 文本转语音（返回 PCM） |
| GET  | /api/hardware/tts/preset/{name} | 获取预置语音 |
| GET  | /api/hardware/tts/presets | 列出所有预置语音 |
| POST | /api/hardware/game/checkin | 每日打卡 |
| GET  | /api/hardware/game/profile | 游戏化档案 |

### 专注提醒流程

设备每 30 秒轮询 `GET /api/hardware/focus/status`，服务端检查是否需要提醒：

1. **坐姿提醒**（POSTURE）：专注开始后立即提醒坐姿端正、保护视力
2. **休息提醒**（BREAK）：专注满 30 分钟提醒起来运动、远看放松
3. **继续学习提醒**（CONTINUE）：休息满 10 分钟提醒回来继续

设备收到提醒后播放语音，然后调用 `POST /api/hardware/focus/reminder/ack` 确认。

### 每日打卡

设备首次开机时自动调用 `POST /api/hardware/game/checkin`，服务端记录打卡并返回：
- 连续打卡天数
- 获得经验值
- 是否升级

## 语音播报

设备通过 I2S 接口连接 MAX98357A 功放模块驱动喇叭。

### 硬件接线

```
ESP32-S3          MAX98357A 功放模块
─────────         ─────────────────
GPIO 41  ──────→  BCLK (位时钟)
GPIO 42  ──────→  LRC  (左右声道时钟)
GPIO 43  ──────→  DIN  (数据输入)
GND      ──────→  GND
5V       ──────→  VIN

MAX98357A         喇叭
─────────         ────
Speaker+  ──────→  喇叭 +
Speaker-  ──────→  喇叭 -
```

### 所需物料

| 物料 | 说明 | 参考价格 |
|------|------|----------|
| MAX98357A I2S 功放模块 | 数字功放，3W 输出 | ¥5-10 |
| 3W 4Ω 小喇叭 | 儿童设备用小喇叭 | ¥3-5 |

### 预置语音（16 种）

| 名称 | 内容 |
|------|------|
| greeting | 小朋友你好呀！我是你的学习小伙伴 |
| goodbye | 再见啦小朋友，下次见哦！ |
| encourage | 真棒！你做得太好了！ |
| start_learning | 我们开始学习吧！ |
| rest | 学了这么久，休息一下吧 |
| correct_answer | 太厉害了！答对啦！ |
| wrong_answer | 没关系，再想想看 |
| focus_start | 专注时间开始啦！ |
| focus_end | 专注时间结束！ |
| focus_posture | 小朋友，坐姿要端正哦！ |
| focus_break | 已经学了30分钟啦！起来活动活动吧！ |
| focus_continue | 休息好了吗？我们继续学习吧！ |
| emergency | 哎呀，紧急停止了！ |

## 依赖库

- NimBLE-Arduino: BLE 蓝牙库
- ArduinoJson: JSON 解析库
- DHT sensor library: 温湿度传感器库
- Adafruit Unified Sensor: 传感器统一接口
- ESP32 I2S 驱动（内置，无需额外安装）

## 硬件物料清单

| # | 物料 | 数量 | 参考价 |
|---|------|------|--------|
| 1 | ESP32-S3-DevKitC-1 (N16R8) | 1 | ¥40 |
| 2 | MAX98357A I2S 功放模块 | 1 | ¥8 |
| 3 | 3W 4Ω 小喇叭 | 1 | ¥3 |
| 4 | DHT22 传感器 | 1 | ¥12 |
| 5 | RGB LED | 1 | ¥2 |
| 6 | 按钮 | 1 | ¥1 |
| 7 | 杜邦线/面包板 | 若干 | ¥10 |
| | **合计** | | **~¥76** |
