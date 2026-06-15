package com.childlearning.robot.ui.screens.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.data.model.ChatMessage
import com.childlearning.robot.domain.enums.AppState
import com.childlearning.robot.domain.usecase.VoiceResult
import com.childlearning.robot.domain.usecase.VoiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceUseCase: VoiceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    /**
     * 开始录音
     * 对应固件 STATE_LISTENING — 长按按钮触发
     */
    fun startRecording() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                appState = AppState.Listening,
                error = null
            )
        }
    }

    /**
     * 停止录音并处理
     * 对应固件 STATE_LISTENING → STATE_PROCESSING → STATE_SPEAKING
     */
    fun stopRecordingAndProcess() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(appState = AppState.Processing)

            val result = voiceUseCase.processVoiceInput()

            if (result.isSuccess) {
                val voiceResult = result.getOrThrow()
                // 添加消息到列表
                val newMessages = _messages.value.toMutableList()
                newMessages.add(ChatMessage(voiceResult.recognizedText, true))
                newMessages.add(ChatMessage(voiceResult.replyText, false))
                _messages.value = newMessages

                _uiState.value = _uiState.value.copy(
                    appState = if (voiceResult.ttsPlayed) AppState.Speaking else AppState.Idle,
                    lastRecognizedText = voiceResult.recognizedText,
                    lastReplyText = voiceResult.replyText
                )

                // 模拟播放完成后回到 Idle
                if (voiceResult.ttsPlayed) {
                    kotlinx.coroutines.delay(2000)
                    _uiState.value = _uiState.value.copy(appState = AppState.Idle)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    appState = AppState.Error(result.exceptionOrNull()?.message ?: "处理失败"),
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun stopPlaying() {
        voiceUseCase.stopPlaying()
        _uiState.value = _uiState.value.copy(appState = AppState.Idle)
    }
}

data class VoiceUiState(
    val appState: AppState = AppState.Idle,
    val lastRecognizedText: String? = null,
    val lastReplyText: String? = null,
    val error: String? = null
)
