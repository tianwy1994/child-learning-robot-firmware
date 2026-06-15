# 四端项目联调审核报告

## 项目总览

| 项目 | 技术栈 | 端口 | 职责 |
|------|--------|------|------|
| child-learning-app | UniApp/Vue3/Pinia | 用户手机 | 用户App端 |
| child-learning-auth | Spring Boot 3.4/Java21/MyBatis-Plus | 8081 | 认证服务端 |
| child-learning-robot | Spring Boot 3.4/Java21/JPA | 8080 | 硬件服务端 |
| child-learning-robot-firmware | Android/Kotlin/Compose | 学习机 | 硬件端 |

---

## 🔴 严重问题（必须修复）

### 问题1: 硬件端 ApiService.kt 损坏
**文件**: `child-learning-robot-firmware/android/.../ApiService.kt`
**问题**: 所有 Retrofit 注解被注释掉，例如：
```kotlin
// POST /api/hardware/chat/send - AI 聊天     @POST("api/hardware/chat/send")
```
`@POST("api/hardware/chat/send")` 在 `//` 后面，Kotlin 编译器会忽略注解。
**修复**: 重写 ApiService.kt，确保所有注解在第一列。

### 问题2: 认证流程不一致（四个项目对不上）
**现状**:
- 硬件端(firmware): 期望 `/api/auth/qrcode/generate → poll → confirm` 流程
- 认证端(auth): 实现 `/api/auth/device/bind → unbind → status` 直接绑定
- 用户App端: 调用 `/api/auth/device/bind` 直接绑定
- 硬件服务端: 无 QR 相关接口

**修复方向**: 统一使用认证端的直接绑定流程。硬件端改为：
1. 显示设备ID（二维码/文本）
2. 用户App扫码获取设备ID
3. 用户App调用 `/api/auth/device/bind` 绑定
4. 硬件端轮询 `/api/auth/device/status?deviceId=xxx` 检查绑定状态
5. 绑定成功 → 获取设备 token → 正常使用

### 问题3: 登录标识不一致
**现状**:
- 硬件端 LoginRequest: `username` + `password`
- 认证端 LoginRequest: `phone` + `password`
- 用户App端: `phone` + `password`

**修复**: 硬件端改为 `phone` + `password`，与认证端一致。

### 问题4: 硬件端 API 路径与硬件服务端不一致
**现状**:
- 硬件端调用 `/api/user/*` 路径（在硬件服务端不存在这些路径）
- 硬件服务端实现的是 `/api/mobile/*` 路径

**修复**: 硬件端去掉 `/api/user/*` 接口（这些是用户App端的功能），硬件端只需要 `/api/hardware/*` 接口。

### 问题5: 用户App端 baseUrl 为空
**文件**: `child-learning-app/src/utils/request.js`
```js
const AUTH_BASE_URL = ''  // 空字符串
const API_BASE_URL = ''   // 空字符串
```
**修复**: 改为可配置的服务器地址，开发环境用 Vite 代理，生产环境直连。

---

## 🟡 中等问题

### 问题6: 硬件端缺少设备ID管理
硬件端需要生成唯一的 deviceId，并展示给用户扫描。当前 DeviceIdStore 已实现，但未被 LoginViewModel 使用。

### 问题7: 硬件端 AuthUseCase 调用不存在的端点
- `generateQrCode()` → `/api/auth/qrcode/generate` (认证端不存在)
- `pollQrStatus()` → `/api/auth/qrcode/poll` (认证端不存在)
这些需要改为调用认证端实际存在的端点。

### 问题8: 硬件服务端缺少 heartbeat 和 device/info 端点
硬件端调用 `/api/hardware/heartbeat` 和 `/api/hardware/device/info`，但硬件服务端没有对应的 Controller。

### 问题9: 硬件服务端 homework/submit 接口不一致
- 硬件端: POST JSON body `{ocrText, imageBase64}`
- 硬件服务端: POST multipart form `file` + `subject` 参数
两边格式不匹配。

---

## 🟢 优化建议

### 10: 统一配置管理
每个项目都应有统一的环境变量/配置文件来管理 IP 地址，确保"改一处即可上线"。

### 11: 硬件端 LoginScreen 简化
去掉 QR code 扫码激活 Tab，改为设备 ID 展示 + 绑定状态轮询。

### 12: 日志和错误处理
各项目统一错误码和响应格式。

---

## 修复计划

### Step 1: 修复硬件端 ApiService.kt（重写）
### Step 2: 统一认证流程（硬件端改为直接绑定）
### Step 3: 统一登录标识（phone 替代 username）
### Step 4: 修复硬件端 AuthUseCase
### Step 5: 添加硬件服务端缺少的端点
### Step 6: 修复用户App端 baseUrl
### Step 7: 统一 homework 接口格式
### Step 8: 最终联调验证