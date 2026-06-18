package com.childlearning.robot.domain.usecase

import com.childlearning.robot.core.audio.TtsPlayer
import com.childlearning.robot.core.network.FocusSessionResponse
import com.childlearning.robot.data.repository.FocusRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 专注模式用例
 * 对应固件 FocusManager — 管理专注会话、轮询提醒、播放预设语音
 *
 * 固件逻辑：
 * - 开始专注后每 30 秒轮询 /focus/status
 * - 收到提醒后播放对应预设语音，然后调用 /focus/reminder/ack
 * - BREAK 提醒 → 进入休息状态
 * - CONTINUE 提醒 → 恢复专注状态
 * - data=null 表示会话已结束
 */
@Singleton
class FocusUseCase @Inject constructor(
    private val focusRepository: FocusRepository,
    private val ttsPlayer: TtsPlayer
) {
    companion object {
        private const val POLL_INTERVAL_MS = 30_000L  // 对应固件 STATUS_CHECK_INTERVAL
    }

    private val _isFocusing = MutableStateFlow(false)
    val isFocusing: StateFlow<Boolean> = _isFocusing

    private val _isBreak = MutableStateFlow(false)
    val isBreak: StateFlow<Boolean> = _isBreak

    private var pollJob: Job? = null

    // KDoc removed
    var onReminder: ((String, String) -> Unit)? = null  // (type, preset)
    var onSessionEnd: (() -> Unit)? = null

    /**
     * 开始专注
     * 对应固件 FocusManager::startFocus()
     */
    suspend fun startFocus(taskDescription: String): Result<Unit> {
        val result = focusRepository.startFocus(taskDescription)
        if (result.isSuccess) {
            _isFocusing.value = true
            _isBreak.value = false
            startPolling()
        }
        return result
    }

    /**
     * 结束专注
     * 对应固件 FocusManager::endFocus()
     */
    suspend fun endFocus(): Result<Unit> {
        stopPolling()
        val result = focusRepository.endFocus()
        _isFocusing.value = false
        _isBreak.value = false
        return result
    }

    /**
     * 开始轮询
     * 对应固件 FocusManager 的 30 秒定时轮询
     */
    private fun startPolling() {
        stopPolling()
        pollJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && _isFocusing.value) {
                delay(POLL_INTERVAL_MS)
                pollStatus()
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    /**
     * 轮询状态
     * 对应固件 FocusManager::getStatus() + 处理提醒逻辑
     */
    private suspend fun pollStatus() {
        val result = focusRepository.getStatus()
        if (result.isFailure) return

        val statusData = result.getOrNull()

        if (statusData == null) {
            // data=null 表示无活跃会话，对应固件 _focusing = false
            _isFocusing.value = false
            _isBreak.value = false
            stopPolling()
            onSessionEnd?.invoke()
            return
        }

        val reminder = statusData.reminder ?: return

        // 播放提醒预设语音，对应固件 audioPlayer.playPreset(reminder.preset)
        ttsPlayer.playPreset(reminder.preset)

        // 确认提醒，对应固件 ackReminder(reminder.type)
        focusRepository.ackReminder(reminder.type)

        // 根据提醒类型更新状态
        when (reminder.type) {
            "BREAK" -> {
                _isBreak.value = true
                // 对应固件 _inBreak = true, setState(STATE_FOCUS_BREAK)
            }
            "CONTINUE" -> {
                _isBreak.value = false
                // 对应固件 _inBreak = false, setState(STATE_FOCUSING)
            }
            // POSTURE 提醒不改变状态
        }

        onReminder?.invoke(reminder.type, reminder.preset)
    }
}
