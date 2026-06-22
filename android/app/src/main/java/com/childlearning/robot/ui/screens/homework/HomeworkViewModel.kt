package com.childlearning.robot.ui.screens.homework

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.domain.usecase.HomeworkUseCase
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
                                    feedback = parseFeedback(status.gradingResult),
                                    questionResults = parseQuestions(status.gradingResult)
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

    private fun parseGradingJson(gradingResult: String?): JsonObject? {
        if (gradingResult.isNullOrBlank()) return null
        return try {
            JsonParser.parseString(gradingResult).asJsonObject
        } catch (e: Exception) {
            null
        }
    }

    /** 从 gradingResult JSON 里提取 summary + encouragement 作为反馈文本 */
    private fun parseFeedback(gradingResult: String?): String? {
        val json = parseGradingJson(gradingResult) ?: return null
        return try {
            val summary = json.get("summary")?.takeIf { !it.isJsonNull }?.asString
            val encouragement = json.get("encouragement")?.takeIf { !it.isJsonNull }?.asString
            listOfNotNull(summary, encouragement).filter { it.isNotBlank() }
                .joinToString("\n").ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    /** 从 gradingResult JSON 里解析每道题的批改详情 */
    private fun parseQuestions(gradingResult: String?): List<QuestionResult> {
        val json = parseGradingJson(gradingResult) ?: return emptyList()
        return try {
            val questionsArray = json.getAsJsonArray("questions") ?: return emptyList()
            questionsArray.mapNotNull { element ->
                val q = element.asJsonObject
                QuestionResult(
                    question = q.get("question")?.takeIf { !it.isJsonNull }?.asString ?: return@mapNotNull null,
                    studentAnswer = q.get("studentAnswer")?.takeIf { !it.isJsonNull }?.asString ?: "",
                    correctAnswer = q.get("correctAnswer")?.takeIf { !it.isJsonNull }?.asString ?: "",
                    isCorrect = q.get("isCorrect")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
                    explanation = q.get("explanation")?.takeIf { !it.isJsonNull }?.asString?.ifBlank { null }
                )
            }
        } catch (e: Exception) {
            emptyList()
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

data class QuestionResult(
    val question: String,
    val studentAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val explanation: String?
)

data class HomeworkSubmitResult(
    val ocrText: String?,
    val score: Int?,
    val feedback: String?,
    val questionResults: List<QuestionResult> = emptyList()
)

data class HomeworkUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "批改中，请稍候...",
    val selectedImageUri: Uri? = null,
    val subject: String = "数学",
    val submitResult: HomeworkSubmitResult? = null,
    val error: String? = null
)
