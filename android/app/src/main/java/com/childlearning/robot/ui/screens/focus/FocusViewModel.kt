package com.childlearning.robot.ui.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.domain.usecase.FocusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val focusUseCase: FocusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState

    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        // 监听专注状态
        viewModelScope.launch {
            focusUseCase.isFocusing.collect { focusing ->
                _uiState.value = _uiState.value.copy(isFocusing = focusing)
                if (focusing) startTimer() else stopTimer()
            }
        }
        viewModelScope.launch {
            focusUseCase.isBreak.collect { isBreak ->
                _uiState.value = _uiState.value.copy(isBreak = isBreak)
            }
        }

        // 设置提醒回调
        focusUseCase.onReminder = { type, preset ->
            _uiState.value = _uiState.value.copy(
                lastReminder = "提醒: ${reminderTypeText(type)}"
            )
        }
        focusUseCase.onSessionEnd = {
            _uiState.value = _uiState.value.copy(
                isFocusing = false,
                isBreak = false,
                elapsedTimeMs = 0,
                lastReminder = null
            )
        }
    }

    fun startFocus(taskDescription: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = focusUseCase.startFocus(taskDescription)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun endFocus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            focusUseCase.endFocus()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isFocusing = false,
                isBreak = false,
                elapsedTimeMs = 0
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        val startTime = System.currentTimeMillis()
        timerJob = viewModelScope.launch {
            while (isActive) {
                _uiState.value = _uiState.value.copy(
                    elapsedTimeMs = System.currentTimeMillis() - startTime
                )
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}

data class FocusUiState(
    val isFocusing: Boolean = false,
    val isBreak: Boolean = false,
    val isLoading: Boolean = false,
    val elapsedTimeMs: Long = 0,
    val lastReminder: String? = null,
    val error: String? = null
)

private fun reminderTypeText(type: String): String = when (type) {
    "POSTURE" -> "坐姿提醒：请坐直！"
    "BREAK" -> "休息提醒：学习30分钟了，活动一下！"
    "CONTINUE" -> "继续提醒：休息好了吗？继续学习！"
    else -> type
}
