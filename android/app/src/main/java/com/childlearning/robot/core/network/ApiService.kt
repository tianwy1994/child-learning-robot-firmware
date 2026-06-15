package com.childlearning.robot.core.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 硬件端 API 接口定义
 *
 * 两个服务端：
 * - 认证服务端 (port 8081): /api/auth/*  - 通过 NetworkModule 代理
 * - 硬件服务端 (port 8080): /api/hardware/* - 直接调用
 */
interface ApiService {

    // ========================================================================
    // 认证服务端 API - /api/auth/*
    // ========================================================================

    // ---------- 用户登录 ----------

    @POST("api/auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): ApiResult<LoginResponse>

    // ---------- 设备绑定 ----------

    @GET("api/auth/device/status")
    suspend fun getDeviceStatus(
        @Query("deviceId") deviceId: String
    ): ApiResult<DeviceStatusResponse>

    // ========================================================================
    // 硬件服务端 API - /api/hardware/*
    // ========================================================================

    // ---------- AI 聊天 ----------

    @POST("api/hardware/chat/send")
    suspend fun sendChatMessage(
        @Body body: ChatRequest
    ): ApiResult<ChatResponse>

    // ---------- 语音合成 ----------

    @POST("api/hardware/tts/speak")
    @Headers("Accept: audio/pcm")
    suspend fun textToSpeech(
        @Body body: TtsRequest
    ): Response<ResponseBody>

    @GET("api/hardware/tts/preset/{name}")
    @Headers("Accept: audio/pcm")
    suspend fun getPresetVoice(
        @Path("name") name: String
    ): Response<ResponseBody>

    @GET("api/hardware/tts/presets")
    suspend fun getPresetsList(): ApiResult<List<PresetInfo>>

    // ---------- 专注模式 ----------

    @POST("api/hardware/focus/start")
    suspend fun startFocus(
        @Body body: FocusStartRequest
    ): ApiResult<FocusSessionResponse>

    @POST("api/hardware/focus/end")
    suspend fun endFocus(): ApiResult<FocusSessionResponse>

    @GET("api/hardware/focus/status")
    suspend fun getFocusStatus(): ApiResult<FocusSessionResponse?>

    @POST("api/hardware/focus/reminder/ack")
    suspend fun ackReminder(
        @Body body: ReminderAckRequest
    ): ApiResult<EmptyData>

    // ---------- 签到/积分 ----------

    @POST("api/hardware/game/checkin")
    suspend fun dailyCheckin(): ApiResult<CheckinResponse>

    @GET("api/hardware/game/profile")
    suspend fun getGameProfile(): ApiResult<GameProfileResponse>

    // ---------- 作业 ----------

    @Multipart
    @POST("api/hardware/homework/ocr")
    suspend fun homeworkOcr(
        @Part file: MultipartBody.Part
    ): ApiResult<OcrResponse>

    @Multipart
    @POST("api/hardware/homework/submit")
    suspend fun submitHomework(
        @Part file: MultipartBody.Part,
        @Part("subject") subject: RequestBody
    ): ApiResult<HomeworkSubmitResponse>
}

// ============================================================================
// 请求/响应数据类
// ============================================================================

// ---------- 认证 ----------

data class LoginRequest(
    val phone: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val userAccountId: Long? = null,
    val userId: Long? = null,
    val phone: String? = null,
    val nickname: String? = null
)

data class DeviceStatusResponse(
    val deviceId: String,
    val bound: Boolean,
    val userAccountId: Long? = null,
    val tokenExpiresAt: Long? = null
)

// ---------- 聊天 ----------

data class ChatRequest(
    val message: String,
    val role: String = "COMPANION",
    val sessionId: String? = null
)

data class ChatResponse(
    val reply: String,
    val sessionId: String
)

// ---------- TTS ----------

data class TtsRequest(
    val text: String,
    val speed: String? = null
)

data class PresetInfo(
    val name: String,
    val description: String? = null
)

// ---------- 专注 ----------

data class FocusStartRequest(
    val taskDescription: String
)

data class FocusSessionResponse(
    val id: Long? = null,
    val userId: Long? = null,
    val taskDescription: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val durationMinutes: Int? = null,
    val status: String? = null,
    val reminder: ReminderData? = null
)

data class ReminderData(
    val type: String,
    val preset: String
)

data class ReminderAckRequest(
    val type: String
)

// ---------- 签到 ----------

data class CheckinResponse(
    val firstCheckin: Boolean,
    val streakDays: Int
)

data class GameProfileResponse(
    val level: Int,
    val experience: Int,
    val streakDays: Int
)

// ---------- 作业 ----------

data class OcrResponse(
    val text: String?,
    val results: List<Any>?
)

data class HomeworkSubmitResponse(
    val id: Long? = null,
    val userId: Long? = null,
    val ocrText: String? = null,
    val subject: String? = null,
    val score: Int? = null,
    val feedback: String? = null,
    val status: String? = null
)