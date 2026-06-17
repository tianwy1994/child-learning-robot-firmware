# 四端项目联调审核报告 ✅

> 审核日期: 2026-06-15 | 状态: 全部修复完成

## 项目总览

| 项目 | 技术栈 | 端口 | 职责 |
|------|--------|------|------|
| child-learning-app | UniApp/Vue3/Pinia | 用户手机 | 用户App端 |
| child-learning-auth | Spring Boot 3.4/Java21/MyBatis-Plus | 8081 | 认证服务端 |
| child-learning-robot | Spring Boot 3.4/Java21/JPA | 8080 | 硬件服务端 |
| child-learning-robot-firmware | Android/Kotlin/Compose | 学习机 | 硬件端 |

---

## 已修复问题清单

### ✅ 问题1: 硬件端 ApiService.kt 注解损坏
- **文件**: `ApiService.kt`
- **修复**: 重写整个文件，所有 `@POST/@GET/@PUT/@Multipart` 注解正确放置

### ✅ 问题2: 认证流程不一致
- **修复前**: 硬件端用 QR码流程，认证端用直接绑定 → 对不上
- **修复后**: 统一为直接绑定模式
  1. 学习机展示设备ID
  2. 手机App扫描/输入设备ID → `POST /api/auth/device/bind`
  3. 学习机轮询 `GET /api/auth/device/status` → 绑定成功

### ✅ 问题3: 登录标识不一致
- **修复前**: 硬件端 `username`，认证端 `phone`
- **修复后**: 统一使用 `phone` 手机号

### ✅ 问题4: API 路径不一致
- **修复前**: 硬件端调 `/api/user/*`（不存在）
- **修复后**: 移除硬件端不需要的 `/api/user/*`，只保留 `/api/hardware/*`

### ✅ 问题5: 作业接口格式不一致
- **修复前**: 硬件端发 JSON，服务端收 multipart
- **修复后**: 统一使用 multipart/form-data（`file` + `subject`）

### ✅ 问题6: 用户App端 baseUrl 为空
- **文件**: `child-learning-app/src/utils/request.js`
- **修复**: 配置为可修改的 IP 地址
  ```js
  const AUTH_BASE_URL = 'http://192.168.1.100:8081'
  const API_BASE_URL = 'http://192.168.1.100:8080'
  ```

### ✅ 问题7: 硬件服务端缺 heartbeat/device-info 端点
- **文件**: 新增 `HardwareDeviceController.java`
- **新增接口**:
  - `POST /api/hardware/heartbeat`
  - `POST /api/hardware/device/info`

---

## 统一后的认证流程

```
手机App(注册) → 认证端(8081)
     ↓
学习机(手机号+密码登录) → 认证端(8081) → user token
     ↓
学习机展示设备ID → 开始轮询 GET /api/auth/device/status
     ↓
手机App扫描设备ID → POST /api/auth/device/bind → 认证端(8081)
     ↓
学习机轮询成功 → 进入主页
     ↓
学习机使用 device token → 硬件服务端(8080) → 所有功能
```

## 各项目修改汇总

| 项目 | 修改文件 | 说明 |
|------|----------|------|
| **硬件端** | `ApiService.kt` | 重写，修复注解，对齐接口 |
| **硬件端** | `AuthUseCase.kt` | 改为直接绑定模式 |
| **硬件端** | `LoginViewModel.kt` | 设备ID展示+轮询 |
| **硬件端** | `LoginScreen.kt` | 新UI：登录→等待绑定→成功 |
| **硬件端** | `NetworkModule.kt` | 添加 AUTH_BASE_URL |
| **硬件端** | `HomeworkRepository.kt` | 改为multipart上传 |
| **硬件端** | `HomeworkUseCase.kt` | 添加subject参数 |
| **硬件端** | `HomeworkViewModel.kt` | 添加科目选择 |
| **硬件端** | `HomeworkScreen.kt` | 添加科目选择对话框 |
| **硬件服务端** | `HardwareDeviceController.java` | 新增 heartbeat+device/info |
| **用户App端** | `request.js` | 配置服务器地址 |
| **文档** | `API_CONTRACT.md` | 完全重写，对齐实现 |

## 部署检查清单

上线前确认以下配置：

- [ ] 硬件端 `NetworkModule.kt` — IP 指向正确
- [ ] 用户App `request.js` — IP 指向正确
- [ ] 认证端 `application-prod.yml` — 数据库/Redis 配置正确
- [ ] 硬件服务端 `application-prod.yml` — 数据库/Redis 配置正确
- [ ] 认证端和硬件服务端 `jwt-secret` 一致
- [ ] MySQL 数据库 `learning_robot_auth` 和 `learning_robot` 已创建
- [ ] Redis 服务已启动
- [ ] 四端网络互通