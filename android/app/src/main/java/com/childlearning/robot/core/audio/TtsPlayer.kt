package com.childlearning.robot.core.audio

import com.childlearning.robot.core.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TTS 语音播放器
 * 对应固件 AudioPlayer::playTts(text) — 调用服务端 TTS 接口获取 PCM 并播放
 *
 * 流程：POST /api/hardware/tts/speak → 返回 PCM 二进制 → 播放
 */
@Singleton
class TtsPlayer @Inject constructor(
    private val apiService: ApiService,
    private val pcmPlayer: PcmAudioPlayer
) {
    /**
     * 合成并播放语音
     * 对应固件 main.cpp 中的 processVoiceInput() 第 4 步
     */
    suspend fun speak(text: String, speed: String? = null): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.textToSpeech(
                    com.childlearning.robot.core.network.TtsRequest(text, speed)
                )

                if (response.isSuccessful) {
                    val audioBytes = response.body()?.bytes()
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        pcmPlayer.playPcm(audioBytes)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 播放预设语音
     * 对应固件 AudioPlayer::playPreset(name)
     * 预设名: greeting, encourage, focus_start, focus_end, focus_posture, focus_break 等
     */
    suspend fun playPreset(name: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPresetVoice(name)
                if (response.isSuccessful) {
                    val audioBytes = response.body()?.bytes()
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        pcmPlayer.playPcm(audioBytes)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    fun stop() {
        pcmPlayer.stop()
    }
}
