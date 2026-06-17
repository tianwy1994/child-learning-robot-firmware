package com.childlearning.robot.domain.usecase

import com.childlearning.robot.core.audio.TtsPlayer
import com.childlearning.robot.data.repository.ChatRepository
import com.childlearning.robot.data.repository.SttRepository
import com.childlearning.robot.domain.enums.ChatRole
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音交互用例
 * 对应固件 main.cpp 中的 processVoiceInput() — 完整语音流水线
 *
 * 流程：录音 → STT → Chat → TTS 播放
 * 固件中这是同步阻塞的，Android 中用协程异步执行
 */
@Singleton
class VoiceUseCase @Inject constructor(
    private val sttRepository: SttRepository,
    private val chatRepository: ChatRepository,
    private val ttsPlayer: TtsPlayer
) {
    /**
     * 执行完整的语音交互流水线
     * 对应固件 processVoiceInput():
     *   1. 录音 (MicManager)
     *   2. STT (SttClient → /api/hardware/stt/recognize)
     *   3. Chat (ChatManager → /api/hardware/chat/send)
     *   4. TTS (AudioPlayer → /api/hardware/tts/speak)
     *   5. 播放
     */
    suspend fun processVoiceInput(
        role: ChatRole = ChatRole.COMPANION
    ): Result<VoiceResult> {
        // Step 1+2: 录音 + 语音识别
        val sttResult = sttRepository.recordAndRecognize()
        if (sttResult.isFailure) {
            return Result.failure(sttResult.exceptionOrNull() ?: Exception("语音识别失败"))
        }
        val recognizedText = sttResult.getOrThrow()

        // Step 3: AI 聊天
        val chatResult = chatRepository.sendMessage(recognizedText, role)
        if (chatResult.isFailure) {
            return Result.failure(chatResult.exceptionOrNull() ?: Exception("聊天请求失败"))
        }
        val reply = chatResult.getOrThrow()

        // Step 4+5: TTS 合成并播放
        val ttsSuccess = ttsPlayer.speak(reply.reply)

        return Result.success(
            VoiceResult(
                recognizedText = recognizedText,
                replyText = reply.reply,
                ttsPlayed = ttsSuccess
            )
        )
    }

    // KDoc removed
    fun stopRecording() {
        sttRepository.stopRecording()
    }

    // KDoc removed
    fun stopPlaying() {
        ttsPlayer.stop()
    }
}

/**
 * 语音交互结果
 */
data class VoiceResult(
    val recognizedText: String,
    val replyText: String,
    val ttsPlayed: Boolean
)
