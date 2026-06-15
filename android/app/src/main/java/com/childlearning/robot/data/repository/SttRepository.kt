package com.childlearning.robot.data.repository

import com.childlearning.robot.core.audio.AudioRecorder
import com.childlearning.robot.core.network.ApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语音识别仓库
 * 对应固件 SttClient — 录制 PCM 音频并上传到 /api/hardware/stt/recognize
 *
 * 固件逻辑：
 * - 录音格式: 16kHz 16bit 单声道 PCM
 * - 最小 1 秒 (32000 bytes)，最大 15 秒 (480000 bytes)
 * - Content-Type: audio/pcm
 * - 自定义 Header: X-Audio-Sample-Rate, X-Audio-Bits, X-Audio-Channels
 */
@Singleton
class SttRepository @Inject constructor(
    private val apiService: ApiService,
    private val audioRecorder: AudioRecorder
) {
    companion object {
        private const val MIN_AUDIO_BYTES = 32000  // 1 秒 = 16000 * 2 bytes
    }

    /**
     * 录音并识别
     * 对应固件 main.cpp 中的 processVoiceInput() 第 1-2 步
     */
    suspend fun recordAndRecognize(): Result<String> {
        return try {
            // 录音
            val audioData = audioRecorder.startRecording()

            // 检查最小长度，对应固件的 MIN_RECORD_SECONDS 检查
            if (audioData.size < MIN_AUDIO_BYTES) {
                return Result.failure(Exception("录音太短"))
            }

            // 上传识别
            val requestBody = audioData.toRequestBody("audio/pcm".toMediaType())
            val response = apiService.recognizeSpeech(requestBody)

            if (response.isSuccess && response.data != null) {
                Result.success(response.data.text)
            } else {
                Result.failure(Exception("语音识别失败: code=${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 停止录音 */
    fun stopRecording() {
        audioRecorder.stopRecording()
    }
}
