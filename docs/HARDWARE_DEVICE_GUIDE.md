# 硬件端（学习机）详细对接文档

> 项目: child-learning-robot-firmware
> 类型: Android APK (Kotlin + Jetpack Compose)
> 运行设备: Android 学习平板 (minSdk 24, targetSdk 34)

---

## 一、项目结构

```
android/app/src/main/java/com/childlearning/robot/
├── App.kt                          # @HiltAndroidApp 入口
├── MainActivity.kt                 # 主 Activity
├── core/
│   ├── audio/
│   │   ├── AudioRecorder.kt        # 麦克风录音 (16kHz 16bit mono PCM)
│   │   ├── AudioPlayer.kt          # PCM 音频播放 (AudioTrack)
│   │   └── TtsPlayer.kt            # TTS 播放器 (调用服务端合成)
│   ├── camera/
│   │   ├── CameraManager.kt        # 拍照管理 (CameraX)
│   │   ├── QrCodeGenerator.kt      # 二维码生成 (设备ID展示)
│   │   └── QrCodeAnalyzer.kt       # 二维码扫描 (ML Kit)
│   ├── di/
│   │   └── AppModule.kt            # Hilt 依赖注入
│   ├── network/
│   │   ├── ApiService.kt           # Retrofit API 接口定义
│   │   ├── ApiResult.kt            # 统一响应格式 {code, data}
│   │   ├── AuthInterceptor.kt      # OkHttp 拦截器 (自动加 token)
│   │   └── NetworkModule.kt        # Hilt 网络模块 (IP 配置)
│   └── storage/
│       ├── TokenStore.kt           # JWT Token 持久化 (DataStore)
│       └── DeviceIdStore.kt        # 设备 ID 生成和存储
├── data/
│   ├── model/ChatMessage.kt        # 聊天消息模型
│   └── repository/
│       ├── ChatRepository.kt       # AI 聊天 (session管理)
│       ├── FocusRepository.kt      # 专注模式
│       ├── GameRepository.kt       # 签到积分
│       ├── HomeworkRepository.kt   # 作业 OCR/提交
│       └── SttRepository.kt        # 语音识别
├── domain/
│   ├── enums/ (AppState, AuthState, ChatRole)
│   └── usecase/
│       ├── AuthUseCase.kt          # 认证 + 设备绑定
│       ├── ChatUseCase.kt          # 聊天消息管理
│       ├── FocusUseCase.kt         # 专注 + 提醒轮询
│       ├── GameUseCase.kt          # 自动签到
│       ├── HomeworkUseCase.kt      # 作业拍照+OCR
│       └── VoiceUseCase.kt         # 语音交互流水线
└── ui/
    ├── components/ (ChatBubble, LevelBadge, StatusCard, VoiceButton)
    ├── navigation/AppNavigation.kt # 导航 (AuthState驱动)
    ├── screens/
    │   ├── login/   (LoginScreen, LoginViewModel)
    │   ├── home/    (HomeScreen, HomeViewModel)
    │   ├── chat/    (ChatScreen, ChatViewModel)     # 文字聊天
    │   ├── voice/   (VoiceScreen, VoiceViewModel)   # 语音对话
    │   ├── focus/   (FocusScreen, FocusViewModel)   # 专注学习
    │   ├── game/    (GameScreen, GameViewModel)     # 签到积分
    │   └── homework/(HomeworkScreen, HomeworkViewModel) # 作业帮
    └── theme/ (Color, Theme)
```

---

## 二、服务器地址配置

### 唯一需要修改的配置

**文件**: `core/network/NetworkModule.kt`

```kotlin
// 硬件服务端地址 (port 8080) — 处理 AI聊天/语音/专注/打卡/作业
const val HARDWARE_BASE_URL = "http://192.168.1.100:8080/"

// 认证服务端地址 (port 8081) — 处理登录/设备绑定
const val AUTH_BASE_URL = "http://192.168.1.100:8081/"
```

**注意**: `AUTH_BASE_URL` 当前仅作为常量声明，实际 Retrofit 的 baseUrl 使用 `HARDWARE_BASE_URL`。认证接口 (`/api/auth/*`) 需要硬件服务端反向代理到认证服务端，或者部署时两个服务共用同一个端口。如果认证端和硬件端分开部署，需要在 `NetworkModule` 中创建第二个 Retrofit 实例指向 `AUTH_BASE_URL`。

### 超时配置

```kotlin
private const val CONNECT_TIMEOUT = 15L  // 连接超时 15 秒
private const val READ_TIMEOUT = 30L     // 读取超时 30 秒
private const val WRITE_TIMEOUT = 60L    // 写入超时 60 秒 (音频上传)
```

---

## 三、认证流程（与认证端 child-learning-auth 的对接）

### 3.1 完整流程

```
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: 用户打开学习机 App                                       │
│   → AppNavigation 检查 AuthState                                 │
│   → 无 token → 跳转 LoginScreen                                  │
├─────────────────────────────────────────────────────────────────┤
│ Step 2: 用户输入手机号+密码                                       │
│   → LoginViewModel.login(phone, password)                        │
│   → AuthUseCase.login() → POST /api/auth/login                  │
│   → 认证端(8081) 验证 → 返回 {token, userAccountId, userId, ...} │
│   → TokenStore.saveToken() 保存 token 到本地 DataStore            │
│   → AuthState 变为 Authenticated                                 │
├─────────────────────────────────────────────────────────────────┤
│ Step 3: 展示设备ID，等待绑定                                      │
│   → DeviceIdStore.getDeviceId() 获取设备唯一ID                    │
│   → 登录成功后自动进入 WAITING_FOR_BIND 状态                       │
│   → 开始每2秒轮询 GET /api/auth/device/status?deviceId=xxx       │
│   → 用户需要在手机App上打开「绑定设备」扫描此设备ID                  │
├─────────────────────────────────────────────────────────────────┤
│ Step 4: 手机App绑定设备                                           │
│   → 手机App → POST /api/auth/device/bind {deviceId, childUserId} │
│   → 认证端(8081) 创建绑定记录，签发 device token                   │
├─────────────────────────────────────────────────────────────────┤
│ Step 5: 学习机检测到绑定成功                                       │
│   → 轮询返回 {bound: true}                                       │
│   → 状态变为 BOUND → loginSuccess = true                         │
│   → AppNavigation 导航到 HomeScreen                               │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 关键代码位置

| 文件 | 职责 |
|------|------|
| `AuthUseCase.kt` | `login()` 登录, `checkDeviceBinding()` 轮询绑定状态 |
| `LoginViewModel.kt` | `login()` 触发登录, `startPollingBinding()` 每2秒轮询 |
| `LoginScreen.kt` | UI: 登录表单 → 等待绑定 → 绑定成功 |
| `TokenStore.kt` | 本地 DataStore 存储 token |
| `DeviceIdStore.kt` | 生成 Android 设备唯一 ID |
| `AuthInterceptor.kt` | 自动为所有请求添加 `Authorization: Bearer <token>` |
| `AppNavigation.kt` | AuthState 驱动导航 (Locked→登录页, Authenticated→主页) |

### 3.3 DeviceId 生成规则

```kotlin
// DeviceIdStore.kt
// 使用 Android ID 后8位 + UUID 前8位 组合，首次生成后持久化
// 格式: "A1B2C3D4-12345678"
val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
val deviceId = "${androidId.takeLast(8)}-${UUID.randomUUID().toString().take(8)}".uppercase()
```

### 3.4 Token 生命周期

```
登录 → 保存 token 到 DataStore
     ↓
每次请求 → AuthInterceptor 自动添加 Authorization header
     ↓
收到 401 → AuthInterceptor 自动清除 token → 触发 onUnauthorized
     ↓
AuthUseCase.handleUnauthorized() → 清除 token → 发射 unauthorizedEvent
     ↓
AppNavigation 监听 → 跳转登录页
```

### 3.5 与认证端对接注意事项

- **登录接口**: 使用 `phone` 字段（手机号），不是 `username`
- **登录响应**: 包含 `token`, `userAccountId`, `userId`, `phone`, `nickname`
- **设备绑定轮询**: 每 2 秒轮询一次，5 分钟超时
- **轮询接口**: `GET /api/auth/device/status?deviceId=xxx`，需要携带 user token
- **认证端端口**: 8081，数据库中表 `learning_robot_auth`
- JWT 密钥必须与硬件服务端一致

---

## 四、与硬件服务端 (child-learning-robot) 的对接

### 4.1 AI 聊天

**接口**: `POST /api/hardware/chat/send`

```kotlin
// ChatRepository.kt
// 发送消息 → 保存 sessionId 用于多轮对话
suspend fun sendMessage(message: String, role: ChatRole): Result<ChatResponse>

// ChatUseCase.kt
// 管理消息列表，支持三种角色：
// COMPANION (学习陪伴) | GRADER (作业批改) | EXPLAINER (错题讲解)
```

**请求格式**:
```json
{
  "message": "小明有5个苹果，给了小红2个，还剩几个？",
  "role": "COMPANION",
  "sessionId": "session-uuid" /*首次不传，后续传上次返回的 sessionId */

}
```

**注意**: `userId` 从 JWT 中自动提取，不需要客户端传入

### 4.2 语音对话 (STT → Chat → TTS)

```
录音 → STT(语音识别) → Chat(AI对话) → TTS(语音合成) → 播放
```

**当前实现**: 硬件服务端没有 STT 接口，语音识别功能暂不可用。
硬件的 `SttRepository.kt` 和 `VoiceUseCase.kt` 已预留接口，待服务端实现 `POST /api/hardware/stt/recognize`。

**TTS 接口**:
- `POST /api/hardware/tts/speak` — 文本转语音，返回 PCM 二进制 (16kHz 16bit mono)
- `GET /api/hardware/tts/preset/{name}` — 获取预置语音
- `GET /api/hardware/tts/presets` — 列出所有预置语音

**预置语音列表**（与硬件服务端一致）:
```
greeting, goodbye, encourage, start_learning, rest,
correct_answer, wrong_answer, focus_start, focus_end,
focus_posture, focus_break, focus_continue, emergency
```

### 4.3 专注模式

**接口**:
- `POST /api/hardware/focus/start` — 开始专注，传入 `taskDescription`
- `POST /api/hardware/focus/end` — 结束专注
- `GET /api/hardware/focus/status` — 每30秒轮询，检查提醒
- `POST /api/hardware/focus/reminder/ack` — 确认提醒已播放

**提醒类型**:
| 类型 | 预设语音 | 触发时机 |
|------|----------|----------|
| POSTURE | focus_posture | 专注开始后立即提醒坐姿 |
| BREAK | focus_break | 专注满30分钟后提醒休息 |
| CONTINUE | focus_continue | 休息满10分钟后提醒继续 |

**客户端轮询逻辑** (FocusUseCase.kt):
```kotlin
// 每30秒轮询一次
// 收到 reminder → 播放预设语音 → 调用 ack 确认
// reminder.type = BREAK → 进入休息状态
// reminder.type = CONTINUE → 恢复专注状态
// data = null → 无活跃会话，停止轮询
```

### 4.4 每日签到

**接口**:
- `POST /api/hardware/game/checkin` — 每日签到
- `GET /api/hardware/game/profile` — 获取积分档案

**客户端逻辑** (GameUseCase.kt):
```kotlin
// 首次进入主页时自动签到 (20小时防重复)
// firstCheckin = true → 播放 encourage 预设语音
// 返回 {level, experience, streakDays}
```

### 4.5 作业帮

**接口**:
- `POST /api/hardware/homework/ocr` — OCR 识别 (multipart: `file`)
- `POST /api/hardware/homework/submit` — 提交作业 (multipart: `file` + `subject`)

**客户端流程**:
```
拍照/选图 → 图片预览 → 上传 OCR → 显示识别结果 → 选择科目 → 提交作业
```

**注意**: 两个接口都是 `multipart/form-data`，不是 JSON。
- 拍照使用 `FileProvider` 创建临时文件
- 需要 `CAMERA` 权限（运行时请求）
- 图片存储在 `context.cacheDir/homework_photos/`

### 4.6 设备心跳

**接口**: `POST /api/hardware/heartbeat`

```json
{
  "deviceId": "ABC12345-EF678901",
  "batteryLevel": 85,
  "networkType": "WIFI"
}
```

**注意**: 当前硬件端定义了接口但未实现自动心跳发送逻辑，需要在 HomeViewModel 或后台 Service 中添加定时器（每60秒）。

---

## 五、与用户App端 (child-learning-app) 的对接

### 5.1 对接关系

硬件端和用户App端**不直接通信**。它们通过认证服务端间接协作：

```
硬件端(学习机)                 用户App端(手机)
     │                              │
     │ 1. 登录获取 token             │ 1. 注册/登录获取 token
     │ 2. 展示设备ID                │
     │                              │ 2. 扫描设备ID / 手动输入
     │                              │ 3. POST /api/auth/device/bind
     │ 3. 轮询 GET /api/auth/device/status
     │    检测到 bound=true → 激活成功
     │                              │
     │ 4. 使用硬件功能               │ 4. 查看学习数据
     │    /api/hardware/*            │    /api/mobile/*
```

### 5.2 关键约定

- **设备ID格式**: 硬件端生成的 `ANDROID_ID后8位-UUID前8位`，如 `A1B2C3D4-87654321`
- **用户识别**: 使用手机号 (`phone`) 作为唯一标识
- **绑定超时**: 硬件端轮询超时 5 分钟，用户需要在 5 分钟内完成绑定

---

## 六、权限要求

| 权限 | 用途 | 必需 |
|------|------|------|
| `INTERNET` | 网络通信 | ✅ |
| `ACCESS_NETWORK_STATE` | 网络状态检测 | ✅ |
| `RECORD_AUDIO` | 语音录音 | ✅ |
| `CAMERA` | 拍照（作业帮） | ✅ |
| `READ_MEDIA_IMAGES` | 从相册选图 | 可选 |

**注意**: `RECORD_AUDIO` 和 `CAMERA` 是运行时权限，需要在对应功能使用时动态请求。

---

## 七、编译打包

### 7.1 环境要求

- Android Studio Hedgehog (2023.1) 或更新版本
- JDK 17+
- Android SDK 34
- Gradle 8.5+

### 7.2 打包命令

```bash
cd android
./gradlew assembleDebug    # 开发版 APK (app/build/outputs/apk/debug/)
./gradlew assembleRelease  # 正式版 APK (需要签名配置)
```

### 7.3 依赖库

| 库 | 版本 | 用途 |
|----|------|------|
| Jetpack Compose BOM | 2024.01.00 | UI 框架 |
| Hilt | 2.51.1 | 依赖注入 |
| Retrofit | 2.9.0 | HTTP 客户端 |
| OkHttp | 4.12.0 | 网络层 |
| DataStore | 1.0.0 | 本地存储 |
| CameraX | 1.3.1 | 拍照 |
| ML Kit Barcode | 17.2.0 | 二维码 |
| ZXing | 3.5.2 | 二维码生成 |
| Coil | 2.5.0 | 图片加载 |

### 7.4 重要配置

**AndroidManifest.xml**:
- `android:usesCleartextTraffic="true"` — 允许 HTTP 明文请求（开发环境，生产应改为 HTTPS）
- `FileProvider` — 拍照功能必需

**build.gradle.kts**:
- `applicationId = "com.childlearning.robot"`
- `minSdk = 24`, `targetSdk = 34`
- `compileSdk = 34`

---

## 八、当前已知限制

### 8.1 语音识别 (STT)
硬件服务端未实现 `/api/hardware/stt/recognize` 接口，语音对话功能不可用。
APP 端已预留 `SttRepository.kt` 和 `VoiceUseCase.kt`，服务端实现后即可对接。

### 8.2 设备心跳
`POST /api/hardware/heartbeat` 接口已定义，但 APP 端未实现自动定时发送。
需要在 `HomeViewModel` 或后台 Service 中添加每60秒的心跳发送逻辑。

### 8.3 认证端代理
当前 Retrofit 使用 `HARDWARE_BASE_URL` 作为 baseUrl，`AUTH_BASE_URL` 仅声明未使用。
如果认证端和硬件端分开部署在不同端口，需要创建第二个 Retrofit 实例。

### 8.4 编译状态
命令行编译遇到 Hilt/KSP 注解处理问题，建议使用 Android Studio 打开项目编译。

---

## 九、故障排查

| 问题 | 可能原因 | 解决方法 |
|------|----------|----------|
| 登录失败 | 认证端地址不对 | 检查 NetworkModule 中 HARDWARE_BASE_URL |
| 登录失败 | 手机号或密码错误 | 确认已在手机App注册 |
| 绑定超时 | 手机App未操作 | 确保手机App已登录并扫描设备ID |
| 绑定超时 | 认证端不通 | 检查认证端 8081 端口是否可访问 |
| 聊天无响应 | 硬件服务端不通 | 检查硬件服务端 8080 端口 |
| TTS 无声音 | 服务端 TTS 配置错误 | 检查服务端 `app.tts.provider` 配置 |
| 作业OCR失败 | 图片格式/大小 | 检查图片是否上传成功 |
| App 崩溃 | 权限未授予 | 确保相机和录音权限已授予 |
| 无法编译 | Hilt/KSP 缓存问题 | 用 Android Studio 打开，Sync + Build |
| 401 错误 | token 过期 | 重新登录 |
| 网络错误 | 设备未联网 | 检查 Android 设备的网络连接 |