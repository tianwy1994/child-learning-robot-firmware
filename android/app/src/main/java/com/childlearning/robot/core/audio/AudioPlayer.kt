package com.childlearning.robot.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PCM 音频播放器
 * 对应固件 AudioPlayer — 播放 16kHz 16bit 单声道 PCM 音频
 *
 * 固件使用 I2S_NUM_0 输出到 MAX98357A 功放
 * Android 使用 AudioTrack 直接播放 PCM 数据
 */
@Singleton
class PcmAudioPlayer @Inject constructor() {

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioTrack: AudioTrack? = null
    private var playing = false

    /**
     * 播放 PCM 音频数据
     * 对应固件 AudioPlayer::playAudio(data, length)
     */
    suspend fun playPcm(pcmData: ByteArray) = withContext(Dispatchers.IO) {
        if (pcmData.isEmpty()) return@withContext

        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .setEncoding(AUDIO_FORMAT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(bufferSize, pcmData.size))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack = track
        playing = true

        track.write(pcmData, 0, pcmData.size)
        track.play()

        // 等待播放完成
        // AudioTrack 播放完静态数据后会自动停止
        while (playing && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            Thread.sleep(100)
        }

        track.release()
        audioTrack = null
        playing = false
    }

    // KDoc removed
    fun stop() {
        playing = false
        audioTrack?.stop()
    }

    // KDoc removed
    fun isPlaying(): Boolean = playing
}
