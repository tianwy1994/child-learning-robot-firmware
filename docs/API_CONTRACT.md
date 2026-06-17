# 四端架构 API 接口规范

## 架构总览

```
┌──────────────────────┐     登录/绑定     ┌──────────────────────┐
│  用户手机 App         │ ───────────────→  │  认证服务端           │
│  child-learning-app  │                   │  child-learning-auth │
│  (user token)        │                   │  (port 8081)         │
└────────┬─────────────┘                   └──────────┬───────────┘
         │ 扫码获取设备ID                              │ 签发 token
         │ 调用绑定接口                                │
         ↓                                            ↓
┌──────────────────────┐     调用API       ┌──────────────────────┐
│  硬件设备 (学习机)     │ ───────────────→  │  硬件服务端           │
│  child-learning-      │                   │  child-learning-     │
│  robot-firmware       │                   │  robot (port 8080)   │
│  (device token)      │                   │                      │
└──────────────────────┘                   └──────────────────────┘
```

## 认证流程（直接绑定模式）

### 流程 1: 用户注册 (手机App)
```
手机App → POST /api/auth/register {phone, password, nickname} → 认证服务端(8081)
认证服务端 → 返回 {token, userAccountId, userId, phone, nickname}
手机App 保存 user token
```

### 流程 2: 设备激活 (完整流程)
```
1. 用户在学习机上输入手机号+密码 → POST /api/auth/login → 认证服务端(8081)
   返回 {token, userAccountId, userId, phone, nickname}
   学习机保存 user token，展示设备ID

2. 学习机开始轮询绑定状态 → GET /api/auth/device/status?deviceId=xxx

3. 用户在手机App上打开「绑定设备」页面
   扫描学习机上的设备ID（或手动输入）

4. 手机App → POST /api/auth/device/bind {deviceId, childUserId}
   Header: Authorization: Bearer <user_token>
   认证服务端验证 user token，创建绑定关系，签发 device token

5. 学习机轮询 → GET /api/auth/device/status?deviceId=xxx
   返回 {bound: true, userAccountId: xxx, tokenExpiresAt: xxx}

6. 学习机获得绑定确认，激活成功
   后续使用 user token 调用硬件服务端 API
```

### 流程 3: 日常使用
```
1. 学习机开机 → 从本地存储读取 token
2. 如果 token 有效 → 直接进入主页，可使用硬件功能
3. 如果 token 过期 → 显示登录页面，需要重新登录
```

---

## 一、认证服务端 API (port 8081)

所有接口统一响应格式：
```json
{ "code": 200, "data": {} }
```

### 1.1 用户注册
```
POST /api/auth/register
Content-Type: application/json

Request:
{
  "phone": "13800138000",
  "password": "secure_password_123",
  "nickname": "张爸爸"
}

Response (200):
{
  "code": 200,
  "data": {
    "token": "jwt_token_string",
    "userAccountId": 1,
    "userId": 1,
    "phone": "13800138000",
    "nickname": "张爸爸"
  }
}

Error (409): 手机号已注册
```

### 1.2 用户登录
```
POST /api/auth/login
Content-Type: application/json

Request:
{
  "phone": "13800138000",
  "password": "secure_password_123"
}

Response (200):
{
  "code": 200,
  "data": {
    "token": "jwt_token_string",
    "userAccountId": 1,
    "userId": 1,
    "phone": "13800138000",
    "nickname": "张爸爸"
  }
}

Error (401): 手机号或密码错误
Error (403): 账号已被禁用
```

### 1.3 刷新 Token
```
POST /api/auth/refresh
Authorization: Bearer <token>

Response (200):
{
  "code": 200,
  "data": {
    "token": "new_jwt_token",
    "userAccountId": 1,
    "userId": 1,
    "phone": "13800138000",
    "nickname": "张爸爸"
  }
}
```

### 1.4 绑定设备 (手机App调用)
```
POST /api/auth/device/bind
Authorization: Bearer <user_token>
Content-Type: application/json

Request:
{
  "deviceId": "ABC12345-EF678901",
  "childUserId": 2        // 可选，默认使用家长的默认子女
}

Response (200):
{
  "code": 200,
  "data": {
    "token": "device_jwt_token",
    "deviceId": "ABC12345-EF678901",
    "expiresAt": 1718086400000
  }
}
```
服务端逻辑：
1. 验证 user token 有效
2. 如果设备已绑定其他用户，自动解除旧绑定
3. 签发 device token（有效期30天）
4. 创建绑定记录

### 1.5 解绑设备 (手机App调用)
```
POST /api/auth/device/unbind
Authorization: Bearer <user_token>
Content-Type: application/json

Request:
{
  "deviceId": "ABC12345-EF678901"
}

Response (200):
{ "code": 200, "data": null }
```

### 1.6 查询设备绑定状态 (学习机轮询)
```
GET /api/auth/device/status?deviceId=ABC12345-EF678901
Authorization: Bearer <user_token>

Response (200) - 已绑定:
{
  "code": 200,
  "data": {
    "deviceId": "ABC12345-EF678901",
    "bound": true,
    "userAccountId": 1,
    "tokenExpiresAt": 1718086400000
  }
}

Response (200) - 未绑定:
{
  "code": 200,
  "data": {
    "deviceId": "ABC12345-EF678901",
    "bound": false,
    "userAccountId": null,
    "tokenExpiresAt": null
  }
}
```
说明：学习机每2秒轮询一次，绑定成功后进入主页。

### 1.7 Token 验证 (供硬件服务端调用)
```
POST /api/auth/verify
Content-Type: application/json

Request:
{
  "token": "jwt_token_to_verify"
}

Response (200) - 有效:
{
  "code": 200,
  "data": {
    "valid": true,
    "userId": 1,
    "userAccountId": 1,
    "tokenType": "DEVICE",
    "deviceId": "ABC12345-EF678901"
  }
}

Response (200) - 无效:
{
  "code": 200,
  "data": { "valid": false }
}
```

---

## 二、硬件服务端 API (port 8080)

### 硬件设备端接口 /api/hardware/*
所有接口需要 `Authorization: Bearer <device_token>`。
服务端使用与认证端相同的 JWT 密钥本地验签，无需网络调用。

### 2.1 AI 聊天
```
POST /api/hardware/chat/send
Authorization: Bearer <device_token>
Content-Type: application/json

Request:
{
  "message": "小明有5个苹果，给了小红2个，还剩几个？",
  "role": "COMPANION",        // COMPANION | GRADER | EXPLAINER
  "sessionId": "session-uuid"  // 首次可不传，用于多轮对话
}

Response (200):
{
  "code": 200,
  "data": {
    "reply": "还剩3个苹果哦！5-2=3，你真棒！",
    "sessionId": "session-uuid"
  }
}
```
说明：userId 从 JWT 中自动提取，不接受客户端传入。

### 2.2 语音合成 (TTS)
```
POST /api/hardware/tts/speak
Authorization: Bearer <device_token>
Content-Type: application/json
Accept: audio/pcm

Request:
{
  "text": "你真棒！",
  "speed": "normal"    // slow | normal | fast
}

Response (200): 原始 PCM 音频二进制数据
Headers:
  Content-Type: audio/pcm
  X-Audio-Sample-Rate: 16000
  X-Audio-Bits: 16
  X-Audio-Channels: 1
```

### 2.3 获取预设语音
```
GET /api/hardware/tts/preset/{name}
Authorization: Bearer <device_token>
Accept: audio/pcm

Path: name = greeting | encourage | focus_start | focus_end
           | focus_posture | focus_break | focus_continue
           | correct_answer | wrong_answer | goodbye
           | start_learning | rest | emergency

Response (200): 原始 PCM 音频二进制数据
```

### 2.4 获取预设语音列表
```
GET /api/hardware/tts/presets
Authorization: Bearer <device_token>

Response (200):
{
  "code": 200,
  "data": {
    "greeting": "小朋友你好呀！我是你的学习小伙伴",
    "encourage": "真棒！你做得太好了！",
    "focus_start": "专注时间开始啦！",
    ...
  }
}
```

### 2.5 开始专注
```
POST /api/hardware/focus/start
Authorization: Bearer <device_token>
Content-Type: application/json

Request:
{
  "taskDescription": "完成数学作业"
}

Response (200):
{
  "code": 200,
  "data": {
    "id": 123,
    "userId": 1,
    "taskDescription": "完成数学作业",
    "startTime": "2026-06-15T10:00:00",
    "status": "ACTIVE",
    "reminder": null
  }
}
```

### 2.6 结束专注
```
POST /api/hardware/focus/end
Authorization: Bearer <device_token>

Response (200):
{
  "code": 200,
  "data": {
    "id": 123,
    "userId": 1,
    "taskDescription": "完成数学作业",
    "startTime": "2026-06-15T10:00:00",
    "endTime": "2026-06-15T10:30:00",
    "durationMinutes": 30,
    "status": "COMPLETED",
    "reminder": null
  }
}
```

### 2.7 轮询专注状态
```
GET /api/hardware/focus/status
Authorization: Bearer <device_token>

Response (200) - 无活跃会话:
{ "code": 200, "data": null }

Response (200) - 有会话无提醒:
{
  "code": 200,
  "data": {
    "id": 123,
    "status": "ACTIVE",
    "reminder": null
  }
}

Response (200) - 有提醒:
{
  "code": 200,
  "data": {
    "id": 123,
    "status": "ACTIVE",
    "reminder": {
      "type": "POSTURE",       // POSTURE | BREAK | CONTINUE
      "preset": "focus_posture"
    }
  }
}
```
说明：设备每30秒轮询一次。收到 reminder 后播放对应预设语音，然后调用 ack 确认。

### 2.8 确认提醒
```
POST /api/hardware/focus/reminder/ack
Authorization: Bearer <device_token>
Content-Type: application/json

Request:
{
  "type": "POSTURE"
}

Response (200):
{ "code": 200, "data": null }
```

### 2.9 每日签到
```
POST /api/hardware/game/checkin
Authorization: Bearer <device_token>

Response (200):
{
  "code": 200,
  "data": {
    "firstCheckin": true,
    "streakDays": 5
  }
}
```

### 2.10 获取积分档案
```
GET /api/hardware/game/profile
Authorization: Bearer <device_token>

Response (200):
{
  "code": 200,
  "data": {
    "level": 3,
    "experience": 150,
    "streakDays": 7
  }
}
```

### 2.11 作业 OCR
```
POST /api/hardware/homework/ocr
Authorization: Bearer <device_token>
Content-Type: multipart/form-data

Form:
  file: <作业图片文件>

Response (200):
{
  "code": 200,
  "data": {
    "text": "1+1=2, 2+2=4",
    "results": [
      {"question": "1+1", "answer": "2", "correct": true},
      {"question": "2+2", "answer": "4", "correct": true}
    ]
  }
}
```

### 2.12 作业提交
```
POST /api/hardware/homework/submit
Authorization: Bearer <device_token>
Content-Type: multipart/form-data

Form:
  file: <作业图片文件>
  subject: "数学"

Response (200):
{
  "code": 200,
  "data": {
    "id": 456,
    "userId": 1,
    "ocrText": "1+1=2, 2+2=4",
    "subject": "数学",
    "score": 100,
    "feedback": "全部正确，太棒了！",
    "status": "GRADED"
  }
}
```

### 2.13 设备心跳
```
POST /api/hardware/heartbeat
Authorization: Bearer <device_token>
Content-Type: application/json

Request:
{
  "deviceId": "ABC12345-EF678901",
  "batteryLevel": 85,
  "networkType": "WIFI"
}

Response (200):
{
  "code": 200,
  "data": {
    "serverTime": 1718000000000
  }
}
```
说明：设备每60秒上报一次心跳，超120秒未上报标记离线。

### 2.14 设备信息上报
```
POST /api/hardware/device/info
Authorization: Bearer <device_token>
Content-Type: application/json

Request:
{
  "deviceId": "ABC12345-EF678901",
  "deviceModel": "Samsung Tablet",
  "osVersion": "Android 14",
  "appVersion": "1.0.0",
  "screenWidth": 1920,
  "screenHeight": 1080
}

Response (200):
{ "code": 200, "data": null }
```
说明：设备首次激活或版本更新时调用。

---

## 三、手机App端 API

手机App调用两类接口：
- **认证服务端 (8081)**: `/api/auth/*` — 注册/登录/设备绑定
- **硬件服务端 (8080)**: `/api/mobile/*` — 查看学习数据

### 3.1 获取专注汇总 (硬件服务端)
```
GET /api/mobile/focus/today
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": {
    "totalMinutes": 45,
    "averageScore": 92.5,
    "records": [
      {
        "id": 123,
        "startTime": "2026-06-15T08:00:00",
        "endTime": "2026-06-15T08:30:00",
        "durationMinutes": 30,
        "taskDescription": "完成数学作业"
      }
    ]
  }
}
```

### 3.2 每日学习报告 (硬件服务端)
```
GET /api/mobile/focus/report/daily
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": {
    "totalFocusMinutes": 45,
    "tasksCompleted": 3,
    "score": 92,
    "highlights": ["数学作业全对", "专注超过30分钟"]
  }
}
```

### 3.3 今日任务列表 (硬件服务端)
```
GET /api/mobile/focus/tasks/today
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "title": "完成数学作业",
      "description": "课本第25页",
      "subject": "数学",
      "status": "PENDING"
    }
  ]
}
```

### 3.4 游戏化档案 (硬件服务端)
```
GET /api/mobile/game/profile
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": {
    "level": 3,
    "experience": 150,
    "streakDays": 7
  }
}
```

### 3.5 作业记录列表 (硬件服务端)
```
GET /api/mobile/homework/records?page=0&size=10
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": [
    {
      "id": 456,
      "submitTime": "2026-06-15T09:00:00",
      "ocrText": "1+1=2",
      "subject": "数学",
      "score": 100,
      "status": "GRADED"
    }
  ]
}
```

### 3.6 错题本 (硬件服务端)
```
GET /api/mobile/homework/wrong-questions?page=0&size=10
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "question": "3+5=?",
      "wrongAnswer": "7",
      "correctAnswer": "8",
      "subject": "数学",
      "time": "2026-06-15T09:00:00"
    }
  ]
}
```

### 3.7 用户资料 (硬件服务端)
```
GET /api/mobile/user/profile
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": {
    "userId": 1,
    "nickname": "张爸爸",
    "phone": "13800138000"
  }
}

PUT /api/mobile/user/profile
Authorization: Bearer <user_token>

Request:
{
  "nickname": "新昵称"
}

Response (200):
{ "code": 200, "data": null }
```

---

## 四、JWT Token 结构

### 用户 Token (type: "user")
```json
{
  "sub": "user:13800138000",
  "userAccountId": 1,
  "userId": 1,
  "type": "user",
  "iss": "child-learning-auth",
  "iat": 1718000000,
  "exp": 1718604800
}
```
- 有效期：7天（168小时），可配置
- 用于手机App端和硬件端登录

### 设备 Token (type: "device")
```json
{
  "sub": "device:ABC12345-EF678901",
  "userAccountId": 1,
  "userId": 1,
  "childUserId": 2,
  "type": "device",
  "iss": "child-learning-auth",
  "iat": 1718000000,
  "exp": 1720592000
}
```
- 有效期：30天（720小时），可配置
- 用于硬件端调用硬件服务端 API
- childUserId 指向 child-learning-robot 中的用户

### JWT 密钥
认证端和硬件服务端使用相同的 JWT 密钥，硬件服务端本地验签，无需网络调用认证端。
配置项：`app.jwt.secret` / `JWT_SECRET` 环境变量

---

## 五、错误码规范

| code | 含义 | 客户端处理 |
|------|------|-----------|
| 200 | 成功 | 正常处理 |
| 400 | 请求参数错误 | 提示用户检查输入 |
| 401 | 未授权/token无效 | 清除token，跳转登录页 |
| 403 | 账号被禁用 | 提示联系客服 |
| 404 | 资源不存在 | 提示资源不存在 |
| 409 | 手机号已注册 | 提示直接登录 |
| 500 | 服务器错误 | 提示稍后重试 |

---

## 六、各项目配置速查

### 硬件端 (child-learning-robot-firmware)
修改 `NetworkModule.kt`:
```kotlin
const val HARDWARE_BASE_URL = "http://192.168.1.100:8080/"
const val AUTH_BASE_URL = "http://192.168.1.100:8081/"
```

### 用户App端 (child-learning-app)
修改 `src/utils/request.js`:
```js
const AUTH_BASE_URL = 'http://192.168.1.100:8081'
const API_BASE_URL = 'http://192.168.1.100:8080'
```

### 认证服务端 (child-learning-auth)
修改 `src/main/resources/application-prod.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://your-db-host:3306/learning_robot_auth
  data:
    redis:
      host: your-redis-host
```

### 硬件服务端 (child-learning-robot)
修改 `src/main/resources/application-prod.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://your-db-host:3306/learning_robot
  data:
    redis:
      host: your-redis-host
app:
  auth:
    jwt-secret: <与认证端相同的密钥>
```