package com.childlearning.robot.core.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PCM 音频录制器
 * 对应固件 MicManager — 录制 16kHz 16bit 单声道 PCM 音频
 *
 * 固件参数：
 * - 采样率: 16000 Hz
 * - 位深: 16 bit
 * - 声道: 1 (单声道)
 * - 最大录制时长: 15 秒
 */
@Singleton
class AudioRecorder @Inject constructor() {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val MAX_DURATION_MS = 15_000L  // 最大 15 秒，对应固件 MAX_RECORD_SECONDS
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    /**
     * 开始录音，返回 PCM 音频数据
     * 对应固件 MicManager::startRecording() + update() 的组合
     */
    suspend fun startRecording(): ByteArray = withContext(Dispatchers.IO) {
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw IllegalStateException("无法初始化音频录制缓冲区")
        }

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 2
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("AudioRecord 初始化失败")
        }

        audioRecord = record
        isRecording = true
        record.startRecording()

        val outputStream = ByteArrayOutputStream()
        val buffer = ByteArray(bufferSize)
        val maxBytes = (MAX_DURATION_MS * SAMPLE_RATE * 2 / 1000).toInt() // 16bit = 2 bytes/sample
        var totalBytes = 0

        while (isRecording && isActive && totalBytes < maxBytes) {
            val bytesRead = record.read(buffer, 0, buffer.size)
            if (bytesRead > 0) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytes += bytesRead
            }
        }

        record.stop()
        record.release()
        audioRecord = null
        isRecording = false

        outputStream.toByteArray()
    }

    /**
     * 停止录音
     * 对应固件 button release 事件
     */
    fun stopRecording() {
        isRecording = false
    }

    // KDoc removed
    fun isActive(): Boolean = isRecording
}
