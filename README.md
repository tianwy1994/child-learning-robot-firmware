# 📱 小智同学 — 硬件端（学习机）

基于 **Kotlin + Jetpack Compose + Hilt** 的 Android 学习平板应用，作为小智同学硬件设备的客户端，提供 AI 对话、语音交互、专注学习、作业帮、每日签到等功能。

## ✨ 核心功能

### 设备激活
- 手机号 + 密码登录（与认证服务端对接）
- 展示设备 ID，等待手机 App 扫码绑定
- 轮询绑定状态，绑定成功后进入主页

### AI 对话
- 三种角色：学习陪伴（COMPANION）、作业批改（GRADER）、错题讲解（EXPLAINER）
- 多轮对话，sessionId 管理

### 语音交互
- 录音 → TTS 合成 → 播放（STT 语音识别待实现）
- 16 种预置语音（问候、鼓励、专注提醒等）

### 专注模式
- 开始/结束专注计时
- 每 30 秒轮询提醒（坐姿、休息、继续）
- 播放对应预设语音

### 作业帮
- 拍照/选图 → OCR 识别 → 选择科目 → 提交批改
- multipart/form-data 上传

### 每日签到
- 首次进入主页自动签到
- 积分、等级、连续打卡天数

## 🛠️ 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Kotlin | - | 开发语言 |
| Jetpack Compose BOM | 2024.01.00 | UI 框架 |
| Hilt | 2.51.1 | 依赖注入 |
| Retrofit | 2.9.0 | HTTP 客户端 |
| OkHttp | 4.12.0 | 网络层 |
| DataStore | 1.0.0 | Token 本地存储 |
| CameraX | 1.3.1 | 拍照 |
| ML Kit Barcode | 17.2.0 | 二维码扫描 |
| ZXing | 3.5.2 | 二维码生成 |
| Coil | 2.5.0 | 图片加载 |

## 📁 项目结构

```
android/app/src/main/java/com/childlearning/robot/
├── App.kt                          # @HiltAndroidApp 入口
├── MainActivity.kt                 # 主 Activity
├── core/
│   ├── audio/                      # 录音、播放、TTS
│   ├── camera/                     # 拍照、二维码
│   ├── di/                         # Hilt 模块
│   ├── network/                    # Retrofit、拦截器
│   └── storage/                    # Token、DeviceId 持久化
├── data/
│   ├── model/                      # 数据模型
│   └── repository/                 # Chat、Focus、Game、Homework、STT
├── domain/
│   ├── enums/                      # AppState、AuthState、ChatRole
│   └── usecase/                    # 业务用例
└── ui/
    ├── components/                 # 通用组件
    ├── navigation/                 # 导航（AuthState 驱动）
    ├── screens/                    # 各页面
    └── theme/                      # 主题
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1) 或更新
- JDK 17+
- Android SDK 34
- Gradle 8.5+

### 编译运行

```bash
cd android
./gradlew assembleDebug    # 开发版 APK
./gradlew assembleRelease  # 正式版 APK（需要签名）
```

APK 输出路径：`app/build/outputs/apk/debug/`

### 服务器地址配置

**文件**: `core/network/NetworkModule.kt`

```kotlin
const val HARDWARE_BASE_URL = "http://192.168.1.100:8080/"  // 硬件服务端
const val AUTH_BASE_URL = "http://192.168.1.100:8081/"      // 认证服务端
```

## 📡 后端服务依赖

| 服务 | 端口 | 说明 |
|------|------|------|
| child-learning-auth | 8081 | 认证服务（登录、设备绑定） |
| child-learning-robot | 8080 | 硬件服务（AI、TTS、专注、作业） |

## 📖 详细文档

- [硬件设备对接文档](docs/HARDWARE_DEVICE_GUIDE.md) — 项目结构、认证流程、API 对接、权限要求、编译打包
- [API 接口规范](docs/API_CONTRACT.md) — 四端完整 API 定义、请求/响应格式、JWT 结构
- [四端联调审核报告](docs/FOUR_PROJECTS_REVIEW.md) — 已修复问题清单、部署检查清单

## ⚠️ 重要注意事项

### 1. 认证服务端代理问题

当前 Retrofit 只使用 `HARDWARE_BASE_URL`（8080），`AUTH_BASE_URL`（8081）仅声明未使用。如果认证端和硬件端**分开部署在不同端口**，需要在 `NetworkModule` 中创建第二个 Retrofit 实例指向 `AUTH_BASE_URL`，否则登录/绑定接口会请求到错误的服务。

### 2. JWT 密钥必须一致

硬件服务端（robot）本地验签 JWT，不调用认证端验证。因此 **两个服务的 `JWT_SECRET` 必须完全一致**，否则所有硬件接口都会返回 401。

### 3. STT 语音识别未实现

硬件服务端未实现 `POST /api/hardware/stt/recognize` 接口，语音对话功能不可用。`SttRepository.kt` 和 `VoiceUseCase.kt` 已预留接口，待服务端实现后即可对接。

### 4. 设备心跳未自动发送

`POST /api/hardware/heartbeat` 接口已定义，但 App 端**未实现自动定时发送**。需要在 `HomeViewModel` 或后台 Service 中添加每 60 秒的心跳逻辑，否则服务端无法检测设备在线状态。

### 5. HTTP 明文请求

`AndroidManifest.xml` 中 `android:usesCleartextTraffic="true"` 允许 HTTP 明文通信（开发环境需要）。**生产环境应改为 HTTPS** 并关闭此选项。

### 6. 权限说明

| 权限 | 用途 | 类型 |
|------|------|------|
| `INTERNET` | 网络通信 | 安装时授予 |
| `RECORD_AUDIO` | 语音录音 | **运行时请求** |
| `CAMERA` | 拍照（作业帮） | **运行时请求** |
| `ACCESS_FINE_LOCATION` | BLE 扫描（Android < 12） | 运行时请求 |

`RECORD_AUDIO` 和 `CAMERA` 需要在功能使用时动态请求，未授予时对应功能不可用。

### 7. 编译注意事项

- 命令行编译可能遇到 Hilt/KSP 注解处理问题，**建议使用 Android Studio 打开项目编译**
- 首次编译需下载依赖，确保网络通畅
- `local.properties` 中需配置 `sdk.dir` 指向 Android SDK 路径

### 8. BLE 蓝牙模块

设备绑定使用 BLE 通信，UUID 为标准 HM-10/HC-08 模块：

- Service: `0000FFE0-0000-1000-8000-00805F9B34FB`
- Characteristic: `0000FFE1-0000-1000-8000-00805F9B34FB`

当前硬件端 BLE 代码在 App 端（`child-learning-app/src/utils/ble.js`），不在本项目中。

### 9. 多孩支持

设备绑定时可指定 `childUserId`（可选），未指定则使用家长的默认孩子。每个孩子的学习数据独立统计。`userId` 在 JWT 中始终为家长账号 ID，`childUserId` 为当前使用设备的孩子 ID。

## 🔧 故障排查

| 问题 | 可能原因 | 解决方法 |
|------|----------|----------|
| 登录失败 | 服务地址不对 | 检查 `NetworkModule.kt` 中 `HARDWARE_BASE_URL` |
| 绑定超时 | 手机 App 未操作 | 确保手机 App 已登录并在 5 分钟内完成绑定 |
| 401 错误 | JWT 密钥不一致 | 确认 auth 和 robot 的 `JWT_SECRET` 相同 |
| 聊天无响应 | 硬件服务端不通 | 检查 8080 端口是否可访问 |
| TTS 无声音 | TTS 配置错误 | 检查服务端 `app.tts.provider` 配置 |
| 无法编译 | Hilt/KSP 缓存 | Android Studio → File → Sync + Build |

## 📄 License

MIT
