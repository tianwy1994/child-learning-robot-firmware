package com.childlearning.robot.core.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

// 硬件端 API 接口定义
// 两个服务端：
// - 认证服务端 (port 8081): /api/auth/  (通过 NetworkModule 代理)
// - 硬件服务端 (port 8080): /api/hardware/ (直接调用)
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

    @POST("api/auth/device/bind")
    suspend fun bindDevice(
        @Body body: DeviceBindRequest
    ): ApiResult<DeviceBindResponse>

    @GET("api/auth/device/token")
    suspend fun getDeviceToken(
        @Query("deviceId") deviceId: String
    ): ApiResult<DeviceTokenResponse>

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

    // ---------- 挑战系统 ----------
    @GET("api/hardware/challenge/daily")
    suspend fun getDailyChallenges(): ApiResult<DailyChallengesResponse>

    @GET("api/hardware/challenge/{id}")
    suspend fun getChallengeDetail(@Path("id") id: Long): ApiResult<ChallengeDetailResponse>

    @POST("api/hardware/challenge/{id}/submit")
    suspend fun submitAnswer(
        @Path("id") id: Long,
        @Body body: ChallengeSubmitRequest
    ): ApiResult<ChallengeEvaluationResponse>

    @POST("api/hardware/challenge/{id}/submit-drag")
    suspend fun submitDragAnswer(
        @Path("id") id: Long,
        @Body body: ChallengeDragSubmitRequest
    ): ApiResult<ChallengeEvaluationResponse>

    @GET("api/hardware/challenge/progress")
    suspend fun getChallengeProgress(): ApiResult<List<SkillProgressResponse>>

    @GET("api/hardware/challenge/{id}/speak-question")
    @Headers("Accept: audio/mpeg")
    suspend fun speakChallengeQuestion(@Path("id") id: Long): Response<ResponseBody>

    @GET("api/hardware/challenge/speak-feedback")
    @Headers("Accept: audio/mpeg")
    suspend fun speakFeedback(@Query("text") text: String): Response<ResponseBody>
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

data class DeviceBindRequest(
    val deviceId: String,
    val code: String? = null
)

data class DeviceBindResponse(
    val success: Boolean,
    val message: String,
    val deviceToken: String? = null
)

data class DeviceTokenResponse(
    val token: String,
    val expiresAt: Long
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

// ---------- 挑战系统 ----------
data class DailyChallengesResponse(
    val pending: List<ChallengeCard>,
    val completed: List<ChallengeCard>,
    val totalPending: Int,
    val totalCompleted: Int
)

data class ChallengeCard(
    val id: Long,
    val title: String,
    val description: String,
    val type: String,
    val difficulty: Int,
    val expReward: Int,
    val domainKey: String,
    val domainName: String,
    val domainIcon: String,
    val domainLevel: Int,
    val completed: Boolean = false
)

data class ChallengeDetailResponse(
    val id: Long,
    val title: String,
    val description: String,
    val type: String,
    val difficulty: Int,
    val content: String,
    val answer: String? = null,
    val explanation: String? = null,
    val expReward: Int,
    val domainKey: String,
    val domainName: String,
    val domainIcon: String,
    val pageUiSchema: PageUiSchema? = null,
    val options: List<String> = emptyList(),
    val completed: Boolean = false,
    val lastScore: Int? = null
)

data class PageUiSchema(
    val pageLayout: String = "drag_to_slots",
    val pageBgGradient: String? = null,
    val pageBgPattern: String? = null,
    val pageTitle: String? = null,
    val tipText: String? = null,
    val materialArea: AreaConfig? = null,
    val canvasArea: AreaConfig? = null,
    val dragCards: List<DragCard> = emptyList(),
    val targetSlots: List<TargetSlot> = emptyList(),
    val correctMapping: Map<String, String> = emptyMap(),
    val animationConfig: AnimationConfig? = null,
    val particleEffects: List<String> = emptyList(),
    val theme: String? = null
)

data class AreaConfig(
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val bgColor: String? = null,
    val borderStyle: String? = null
)

data class DragCard(
    val cardId: Int,
    val text: String,
    val imageUrl: String? = null,
    val color: String,
    val gradient: String? = null,
    val emoji: String,
    val w: Int,
    val h: Int,
    val shadow: String? = null,
    val borderRadius: String? = null,
    val hoverEffect: String? = null
)

data class TargetSlot(
    val slotId: Int,
    val label: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val bgColor: String? = null,
    val borderColor: String? = null,
    val borderStyle: String? = null,
    val placeholder: String? = null,
    val glowEffect: Boolean = false,
    val acceptCardIds: List<Int> = emptyList()
)

data class AnimationConfig(
    val dragScale: Float = 1.1f,
    val dragRotate: Float = 5f,
    val correctAnim: String = "bounce",
    val errorAnim: String = "shake",
    val passEffect: String = "confetti",
    val correctSound: String = "win",
    val errorSound: String = "oops"
)

data class ChallengeSubmitRequest(
    val response: String
)

data class ChallengeDragSubmitRequest(
    val mapping: Map<String, String>
)

data class ChallengeEvaluationResponse(
    val score: Int,
    val correct: Boolean = false,
    val correctCount: Int? = null,
    val totalSlots: Int? = null,
    val encourage: String,
    val explanation: String? = null,
    val knowledge: String? = null,
    val stars: Int = 1,
    val expEarned: Int = 0,
    val newLevel: Int = 1
)

data class SkillProgressResponse(
    val domainKey: String,
    val domainName: String,
    val domainIcon: String,
    val level: Int,
    val totalAttempts: Int,
    val averageScore: Int,
    val totalChallenges: Int
)