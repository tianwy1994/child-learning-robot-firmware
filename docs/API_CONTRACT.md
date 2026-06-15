# 四端架构 API 接口规范

## 架构总览

```
┌─────────────────┐         ┌─────────────────┐
│   用户手机 App   │ ──────→ │   认证服务端      │
│  (user token)   │  登录    │  (port 8081)     │
└────────┬────────┘         └────────┬────────┘
         │ 扫码确认                    │ 签发 token
         ↓                           ↓
┌─────────────────┐         ┌─────────────────┐
│   硬件设备       │ ──────→ │   硬件服务端      │
│  (device token) │  调用API │  (port 8080)     │
└─────────────────┘         └─────────────────┘
```

## 认证流程

### 流程 1: 用户登录 (手机App)
```
手机App → POST /api/auth/login {username, password} → 认证服务端
认证服务端 → 返回 {token, userId, username} → 手机App
手机App 保存 user token
```

### 流程 2: 设备扫码激活 (完整流程)
```
1. 学习机 → POST /api/auth/qrcode/generate → 认证服务端
   返回 {sessionId, qrContent:"https://device.login?sid=xxx"}

2. 学习机展示二维码，开始轮询

3. 手机App(已登录) → 扫描二维码 → 提取 sessionId

4. 手机App → POST /api/auth/qrcode/confirm {sessionId}
   Header: Authorization: Bearer <user_token>
   认证服务端验证 user token，标记 sessionId 为 CONFIRMED

5. 学习机 → POST /api/auth/qrcode/poll {sessionId}
   返回 {status:"CONFIRMED", token:"device_token_xxx"}

6. 学习机保存 device token，激活成功
```

---

## 一、认证服务端 API (port 8081)

### 1.1 用户登录
```
POST /api/auth/login
Content-Type: application/json

Request:
{
  "username": "string",
  "password": "string"
}

Response (200):
{
  "code": 200,
  "data": {
    "token": "jwt_token_string",
    "userId": "user_001",
    "username": "parent_zhang"
  }
}

Error (401):
{
  "code": 401,
  "data": null
}
```

### 1.1b 用户注册 (新增)
```
POST /api/auth/register
Content-Type: application/json

Request:
{
  "username": "parent_zhang",
  "password": "secure_password_123",
  "nickname": "张爸爸"
}

Response (200):
{
  "code": 200,
  "data": {
    "token": "jwt_token_string",
    "userId": "user_001",
    "username": "parent_zhang"
  }
}

Error (409):
{
  "code": 409,
  "data": null
}
```
说明：用户名已存在时返回 409。

### 1.2 用户登出
```
POST /api/auth/logout
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": null
}
```
服务端应将 token 加入黑名单。

### 1.3 获取当前用户信息
```
GET /api/auth/me
Authorization: Bearer <user_token | device_token>

Response (200):
{
  "code": 200,
  "data": {
    "userId": "user_001",
    "username": "parent_zhang",
    "nickname": "张爸爸",
    "role": "PARENT"
  }
}
```

### 1.4 刷新 Token
```
POST /api/auth/refresh
Authorization: Bearer <token>

Response (200):
{
  "code": 200,
  "data": {
    "token": "new_jwt_token",
    "userId": "user_001",
    "username": "parent_zhang"
  }
}
```

### 1.5 生成设备登录二维码
```
POST /api/auth/qrcode/generate
（无需认证，设备调用）

Response (200):
{
  "code": 200,
  "data": {
    "sessionId": "uuid-session-12345",
    "qrContent": "https://device.login?sid=uuid-session-12345"
  }
}
```
服务端创建 sessionId，状态设为 PENDING，设置 5 分钟过期。

### 1.6 轮询扫码状态
```
POST /api/auth/qrcode/poll
（无需认证，设备调用）

Request:
{
  "sessionId": "uuid-session-12345"
}

Response (200) — 等待中:
{
  "code": 200,
  "data": {
    "status": "PENDING",
    "token": null
  }
}

Response (200) — 已确认:
{
  "code": 200,
  "data": {
    "status": "CONFIRMED",
    "token": "device_jwt_token_xxx"
  }
}

Response (200) — 已过期:
{
  "code": 200,
  "data": {
    "status": "EXPIRED",
    "token": null
  }
}
```

### 1.7 用户确认扫码
```
POST /api/auth/qrcode/confirm
Authorization: Bearer <user_token>
Content-Type: application/json

Request:
{
  "sessionId": "uuid-session-12345"
}

Response (200):
{
  "code": 200,
  "data": null
}
```
服务端逻辑：
1. 验证 user token 有效
2. 将 sessionId 状态从 PENDING 改为 CONFIRMED
3. 为该 session 生成 device token（关联到该用户）
4. 设备下次 poll 时会拿到 device token

### 1.8 Token 验证 (供硬件服务端调用)
```
POST /api/auth/verify
Content-Type: application/json

Request:
{
  "token": "jwt_token_to_verify"
}

Response (200) — 有效:
{
  "code": 200,
  "data": {
    "valid": true,
    "userId": "user_001",
    "tokenType": "DEVICE",
    "expiresAt": 1718000000
  }
}

Response (200) — 无效:
{
  "code": 200,
  "data": {
    "valid": false
  }
}
```

---

## 二、硬件服务端 API (port 8080)

所有接口需要 `Authorization: Bearer <device_token>`。
服务端通过调用认证服务端的 `/api/auth/verify` 验证 token。

### 2.1 AI 聊天
```
POST /api/hardware/chat/send
Authorization: Bearer <device_token>
Content-Type: application/json

Request:
{
  "message": "小明有5个苹果，给了小红2个，还剩几个？",
  "role": "COMPANION",    // COMPANION | GRADER | EXPLAINER
  "sessionId": "session-uuid"  // 首次可不传
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

### 2.2 语音识别
```
POST /api/hardware/stt/recognize
Authorization: Bearer <device_token>
Content-Type: audio/pcm
X-Audio-Sample-Rate: 16000
X-Audio-Bits: 16
X-Audio-Channels: 1

Body: 原始 PCM 音频数据 (16kHz 16bit 单声道，最长15秒)

Response (200):
{
  "code": 200,
  "data": {
    "text": "小明有五个苹果"
  }
}
```

### 2.3 语音合成
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
```

### 2.4 获取预设语音
```
GET /api/hardware/tts/preset/{name}
Authorization: Bearer <device_token>
Accept: audio/pcm

Path: name = greeting | encourage | focus_start | focus_end | ...

Response (200): 原始 PCM 音频二进制数据
```

### 2.5 获取预设语音列表
```
GET /api/hardware/tts/presets
Authorization: Bearer <device_token>

Response (200):
{
  "code": 200,
  "data": [
    {"name": "greeting", "description": "问候语"},
    {"name": "encourage", "description": "鼓励语"},
    {"name": "focus_start", "description": "专注开始"},
    {"name": "focus_end", "description": "专注结束"},
    {"name": "focus_posture", "description": "坐姿提醒"},
    {"name": "focus_break", "description": "休息提醒"},
    {"name": "focus_continue", "description": "继续学习"},
    {"name": "correct_answer", "description": "回答正确"},
    {"name": "wrong_answer", "description": "回答错误"}
  ]
}
```

### 2.6 开始专注
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
  "data": null
}
```

### 2.7 结束专注
```
POST /api/hardware/focus/end
Authorization: Bearer <device_token>

Response (200):
{
  "code": 200,
  "data": null
}
```

### 2.8 轮询专注状态
```
GET /api/hardware/focus/status
Authorization: Bearer <device_token>

Response (200) — 无活跃会话:
{
  "code": 200,
  "data": null
}

Response (200) — 有会话无提醒:
{
  "code": 200,
  "data": {
    "reminder": null
  }
}

Response (200) — 有提醒:
{
  "code": 200,
  "data": {
    "reminder": {
      "type": "POSTURE",    // POSTURE | BREAK | CONTINUE
      "preset": "focus_posture"
    }
  }
}
```

### 2.9 确认提醒
```
POST /api/hardware/focus/reminder/ack
Authorization: Bearer <device_token>
Content-Type: application/json

Request:
{
  "type": "POSTURE"
}

Response (200):
{
  "code": 200,
  "data": null
}
```

### 2.10 每日签到
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

### 2.11 获取积分档案
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

### 2.12 作业提交
```
POST /api/hardware/homework/submit
Authorization: Bearer <device_token>
Content-Type: application/json

Request:
{
  "ocrText": "识别出的文字",
  "imageBase64": "base64_encoded_image..."
}

Response (200):
{
  "code": 200,
  "data": null
}
```

### 2.13 作业 OCR
```
POST /api/hardware/homework/ocr
Authorization: Bearer <device_token>
Content-Type: multipart/form-data

Form: image=<file>

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

### 2.14 设备心跳 (新增)
```
POST /api/hardware/heartbeat
Authorization: Bearer <device_token>
Content-Type: application/json

Request:
{
  "deviceId": "AA:BB:CC:DD:EE:FF",
  "batteryLevel": 85,
  "networkType": "WIFI"
}

Response (200):
{
  "code": 200,
  "data": {
    "serverTime": 1718000000
  }
}
```
说明：设备每 60 秒上报一次心跳，服务端更新设备在线状态。
超过 120 秒未收到心跳则标记设备离线。

### 2.15 设备信息上报 (新增)
```
POST /api/hardware/device/info
Authorization: Bearer <device_token>
Content-Type: application/json

Request:
{
  "deviceId": "AA:BB:CC:DD:EE:FF",
  "deviceModel": "Android Tablet",
  "osVersion": "Android 14",
  "appVersion": "1.0.0",
  "screenWidth": 1920,
  "screenHeight": 1080
}

Response (200):
{
  "code": 200,
  "data": null
}
```
说明：设备首次激活或版本更新时调用，上报设备信息。

---

## 三、用户 App 端 API (port 8080)

所有接口需要 `Authorization: Bearer <user_token>`。

### 3.1 获取用户绑定的设备列表
```
GET /api/user/devices
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": [
    {
      "deviceId": "AA:BB:CC:DD:EE:FF",
      "deviceName": "小智学习机",
      "bindTime": "2026-06-15T10:00:00Z",
      "lastOnlineTime": "2026-06-15T12:00:00Z",
      "isOnline": true
    }
  ]
}
```

### 3.2 解绑设备
```
POST /api/user/devices/{deviceId}/unbind
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": null
}
```

### 3.3 获取专注历史
```
GET /api/user/focus/history?page=0&size=20
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": {
    "total": 50,
    "page": 0,
    "records": [
      {
        "id": "focus-001",
        "startTime": "2026-06-15T08:00:00Z",
        "endTime": "2026-06-15T08:30:00Z",
        "durationMinutes": 30,
        "taskDescription": "完成数学作业"
      }
    ]
  }
}
```

### 3.4 获取作业历史
```
GET /api/user/homework/history?page=0&size=20
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": [
    {
      "id": "hw-001",
      "submitTime": "2026-06-15T09:00:00Z",
      "ocrText": "1+1=2",
      "status": "GRADED"
  }
]
}
```

### 3.5 用户注册 (新增)
```
POST /api/user/register
Content-Type: application/json

Request:
{
  "username": "parent_zhang",
  "password": "secure_password_123",
  "nickname": "张爸爸",
  "role": "PARENT"
}

Response (200):
{
  "code": 200,
  "data": {
    "token": "jwt_token_string",
    "userId": "user_001",
    "username": "parent_zhang"
  }
}

Error (409):
{
  "code": 409,
  "data": null
}
```
说明：用户注册接口，与 /api/auth/register 等效。
用户名已存在返回 409。

### 3.6 获取/更新用户资料 (新增)
```
GET /api/user/profile
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": {
    "userId": "user_001",
    "username": "parent_zhang",
    "nickname": "张爸爸",
    "avatar": "https://cdn.example.com/avatars/user_001.png",
    "role": "PARENT",
    "childName": "小明",
    "childAge": 7,
    "createdAt": "2026-06-01T10:00:00Z"
  }
}
```

```
PUT /api/user/profile
Authorization: Bearer <user_token>
Content-Type: application/json

Request:
{
  "nickname": "新昵称",
  "childName": "小明",
  "childAge": 8
}

Response (200):
{
  "code": 200,
  "data": null
}
```

### 3.7 专注统计 (新增)
```
GET /api/user/focus/stats
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": {
    "totalMinutes": 1250,
    "todayMinutes": 45,
    "streakDays": 7,
    "totalSessions": 42,
    "averageMinutesPerSession": 29
  }
}
```

### 3.8 查看游戏化档案 (新增)
```
GET /api/user/game/profile
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": {
    "level": 3,
    "experience": 150,
    "experienceToNextLevel": 200,
    "streakDays": 7,
    "totalCheckins": 35,
    "achievements": [
      {"id": "first_checkin", "name": "初次打卡", "unlocked": true},
      {"id": "streak_7", "name": "连续7天", "unlocked": true},
      {"id": "streak_30", "name": "连续30天", "unlocked": false}
    ]
  }
}
```

### 3.9 聊天历史 (新增)
```
GET /api/user/chat/history?deviceId={deviceId}&page=0&size=20
Authorization: Bearer <user_token>

Response (200):
{
  "code": 200,
  "data": {
    "total": 100,
    "page": 0,
    "records": [
      {
        "id": "chat-001",
        "deviceId": "AA:BB:CC:DD:EE:FF",
        "message": "小明有5个苹果，给了小红2个，还剩几个？",
        "reply": "还剩3个苹果哦！5-2=3，你真棒！",
        "role": "COMPANION",
        "time": "2026-06-15T10:00:00Z"
      }
    ]
  }
}
```

---

## 四、JWT Token 结构

```json
{
  "sub": "user_001",
  "type": "DEVICE",          // USER | DEVICE
  "deviceId": "AA:BB:CC:DD:EE:FF",  // 仅 DEVICE 类型
  "userId": "user_001",      // 关联的用户 ID
  "iat": 1718000000,
  "exp": 1718086400
}
```

---

## 五、错误码规范

| code | 含义 | 客户端处理 |
|------|------|-----------|
| 200 | 成功 | 正常处理 |
| 400 | 请求参数错误 | 提示用户检查输入 |
| 401 | 未授权/token无效 | 清除token，跳转登录页 |
| 403 | 无权限 | 提示无权限 |
| 404 | 资源不存在 | 提示资源不存在 |
| 500 | 服务器错误 | 提示稍后重试 |
