package com.childlearning.robot.ui.screens.homework

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.domain.usecase.HomeworkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import javax.inject.Inject

@HiltViewModel
class HomeworkViewModel @Inject constructor(
    private val homeworkUseCase: HomeworkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeworkUiState())
    val uiState: StateFlow<HomeworkUiState> = _uiState

    private var pollJob: Job? = null

    fun setSelectedImage(imageUri: Uri) {
        _uiState.value = HomeworkUiState(selectedImageUri = imageUri)
    }

    fun submitHomework(subject: String) {
        val imageUri = _uiState.value.selectedImageUri ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadingMessage = "提交中...",
                error = null,
                submitResult = null
            )
            try {
                val result = homeworkUseCase.submitHomework(imageUri, subject)
                if (result.isSuccess) {
                    val recordId = result.getOrThrow()
                    _uiState.value = _uiState.value.copy(
                        subject = subject,
                        loadingMessage = "正在识别和批改，请稍候..."
                    )
                    startPolling(recordId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "提交失败"
                    )
                }
            } catch (e: SocketTimeoutException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "⏱️ 网络超时，请检查网络后重试"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "❌ ${e.message ?: "请求失败"}"
                )
            }
        }
    }

    private fun startPolling(recordId: Long) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var retries = 0
            val maxRetries = 60  // 最多轮询 60 次 × 3s = 3 分钟
            while (retries < maxRetries) {
                delay(3000L)
                val result = homeworkUseCase.getHomeworkStatus(recordId)
                if (result.isSuccess) {
                    val status = result.getOrNull()
                    when (status?.status) {
                        "COMPLETED" -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                submitResult = HomeworkSubmitResult(
                                    ocrText = status.ocrText,
                                    score = status.score,
                                    feedback = parseFeedback(status.gradingResult)
                                )
                            )
                            return@launch
                        }
                        "FAILED" -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "❌ 批改失败，请重新提交"
                            )
                            return@launch
                        }
                        "PROCESSING" -> {
                            _uiState.value = _uiState.value.copy(
                                loadingMessage = "AI 正在批改中..."
                            )
                        }
                        // PENDING 继续等待
                    }
                }
                retries++
            }
            // 超时退出轮询
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "⏱️ 批改超时，请稍后在历史记录中查看结果"
            )
        }
    }

    /** 从 JSON 格式的 gradingResult 里提取 summary 字段作为反馈文本 */
    private fun parseFeedback(gradingResult: String?): String? {
        if (gradingResult.isNullOrBlank()) return null
        return try {
            val summaryMatch = Regex("\"summary\"\\s*:\\s*\"([^\"]+)\"")
                .find(gradingResult)?.groupValues?.get(1)
            summaryMatch ?: gradingResult.take(200)
        } catch (e: Exception) {
            null
        }
    }

    fun clearResult() {
        pollJob?.cancel()
        _uiState.value = HomeworkUiState()
    }

    fun setSubject(subject: String) {
        _uiState.value = _uiState.value.copy(subject = subject)
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}

data class HomeworkSubmitResult(
    val ocrText: String?,
    val score: Int?,
    val feedback: String?
)

data class HomeworkUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "批改中，请稍候...",
    val selectedImageUri: Uri? = null,
    val subject: String = "数学",
    val submitResult: HomeworkSubmitResult? = null,
    val error: String? = null
)
