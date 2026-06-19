package com.childlearning.robot.ui.screens.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.core.audio.PcmAudioPlayer
import com.childlearning.robot.core.network.ChallengeCard
import com.childlearning.robot.core.network.ChallengeDetailResponse
import com.childlearning.robot.data.repository.ChallengeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChallengeViewModel @Inject constructor(
    private val repository: ChallengeRepository,
    private val audioPlayer: PcmAudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChallengeUiState>(ChallengeUiState.Loading)
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    private val _dailyChallenges = MutableStateFlow<List<ChallengeCard>>(emptyList())
    val dailyChallenges: StateFlow<List<ChallengeCard>> = _dailyChallenges.asStateFlow()

    // 题库题目列表（10道）
    private val _bankQuestions = MutableStateFlow<List<ChallengeDetailResponse>>(emptyList())
    val bankQuestions: StateFlow<List<ChallengeDetailResponse>> = _bankQuestions.asStateFlow()

    // 当前正在做的题（可以是 daily 或 bank）
    private val _currentChallenge = MutableStateFlow<ChallengeDetailResponse?>(null)
    val currentChallenge: StateFlow<ChallengeDetailResponse?> = _currentChallenge.asStateFlow()

    private val _evaluationResult = MutableStateFlow<EvaluationResult?>(null)
    val evaluationResult: StateFlow<EvaluationResult?> = _evaluationResult.asStateFlow()

    // ========== 每日挑战 ==========

    fun loadDailyChallenges() {
        viewModelScope.launch {
            _uiState.value = ChallengeUiState.Loading
            try {
                val result = repository.getDailyChallenges()
                if (result.isSuccess) {
                    val allChallenges = result.data!!.pending + result.data.completed
                    _dailyChallenges.value = allChallenges
                    _uiState.value = ChallengeUiState.Success
                } else {
                    _uiState.value = ChallengeUiState.Error("加载失败: code=${result.code}")
                }
            } catch (e: Exception) {
                _uiState.value = ChallengeUiState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun loadChallengeDetail(id: Long) {
        viewModelScope.launch {
            _uiState.value = ChallengeUiState.Loading
            try {
                val result = repository.getChallengeDetail(id)
                if (result.isSuccess) {
                    _currentChallenge.value = result.data!!
                    _uiState.value = ChallengeUiState.Success
                } else {
                    _uiState.value = ChallengeUiState.Error("加载失败: code=${result.code}")
                }
            } catch (e: Exception) {
                _uiState.value = ChallengeUiState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun submitAnswer(challengeId: Long, answer: String) {
        viewModelScope.launch {
            _uiState.value = ChallengeUiState.Submitting
            try {
                val result = repository.submitAnswer(challengeId, answer)
                if (result.isSuccess) {
                    val eval = result.data!!
                    _evaluationResult.value = EvaluationResult(
                        score = eval.score,
                        encourage = eval.encourage,
                        explanation = eval.explanation,
                        expEarned = eval.expEarned,
                        isCorrect = eval.correct,
                        stars = eval.stars
                    )
                    _uiState.value = ChallengeUiState.Evaluated
                } else {
                    _uiState.value = ChallengeUiState.Error("提交失败: code=${result.code}")
                }
            } catch (e: Exception) {
                _uiState.value = ChallengeUiState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun submitDragAnswer(challengeId: Long, mapping: Map<String, String>) {
        viewModelScope.launch {
            _uiState.value = ChallengeUiState.Submitting
            try {
                val result = repository.submitDragAnswer(challengeId, mapping)
                if (result.isSuccess) {
                    val eval = result.data!!
                    _evaluationResult.value = EvaluationResult(
                        score = eval.score,
                        encourage = eval.encourage,
                        explanation = eval.explanation,
                        expEarned = eval.expEarned,
                        isCorrect = eval.correct,
                        stars = eval.stars
                    )
                    _uiState.value = ChallengeUiState.Evaluated
                } else {
                    _uiState.value = ChallengeUiState.Error("提交失败: code=${result.code}")
                }
            } catch (e: Exception) {
                _uiState.value = ChallengeUiState.Error(e.message ?: "网络错误")
            }
        }
    }

    // ========== 题库挑战 ==========

    fun loadBankQuestions(domainKey: String) {
        viewModelScope.launch {
            _uiState.value = ChallengeUiState.Loading
            try {
                val result = repository.getBankQuestions(domainKey)
                if (result.isSuccess) {
                    _bankQuestions.value = result.data!!
                    _uiState.value = ChallengeUiState.Success
                } else {
                    _uiState.value = ChallengeUiState.Error("加载失败: code=${result.code}")
                }
            } catch (e: Exception) {
                _uiState.value = ChallengeUiState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun setCurrentChallenge(challenge: ChallengeDetailResponse) {
        _currentChallenge.value = challenge
        _evaluationResult.value = null
        _uiState.value = ChallengeUiState.Success
    }

    fun submitBankAnswer(bankId: Long, answer: String) {
        viewModelScope.launch {
            _uiState.value = ChallengeUiState.Submitting
            try {
                val result = repository.submitBankAnswer(bankId, answer)
                if (result.isSuccess) {
                    val eval = result.data!!
                    _evaluationResult.value = EvaluationResult(
                        score = eval.score,
                        encourage = eval.encourage,
                        explanation = eval.explanation,
                        expEarned = eval.expEarned,
                        isCorrect = eval.correct,
                        stars = eval.stars
                    )
                    _uiState.value = ChallengeUiState.Evaluated
                } else {
                    _uiState.value = ChallengeUiState.Error("提交失败: code=${result.code}")
                }
            } catch (e: Exception) {
                _uiState.value = ChallengeUiState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun submitBankDragAnswer(bankId: Long, mapping: Map<String, String>) {
        viewModelScope.launch {
            _uiState.value = ChallengeUiState.Submitting
            try {
                val result = repository.submitBankDragAnswer(bankId, mapping)
                if (result.isSuccess) {
                    val eval = result.data!!
                    _evaluationResult.value = EvaluationResult(
                        score = eval.score,
                        encourage = eval.encourage,
                        explanation = eval.explanation,
                        expEarned = eval.expEarned,
                        isCorrect = eval.correct,
                        stars = eval.stars
                    )
                    _uiState.value = ChallengeUiState.Evaluated
                } else {
                    _uiState.value = ChallengeUiState.Error("提交失败: code=${result.code}")
                }
            } catch (e: Exception) {
                _uiState.value = ChallengeUiState.Error(e.message ?: "网络错误")
            }
        }
    }

    /**
     * 下一题：从题库随机取1道新题，直接替换当前题目（无需返回列表）。
     */
    fun nextBankQuestion(currentId: Long, domainKey: String) {
        viewModelScope.launch {
            try {
                val result = repository.getBankQuestions(domainKey)
                if (result.isSuccess && result.data!!.isNotEmpty()) {
                    val newQ = result.data.first()
                    _bankQuestions.value = _bankQuestions.value.map {
                        if (it.id == currentId) newQ else it
                    }
                    // 直接切换到新题，无需返回列表
                    _currentChallenge.value = newQ
                    _evaluationResult.value = null
                    _uiState.value = ChallengeUiState.Success
                }
            } catch (_: Exception) {}
        }
    }

    fun speakQuestion(challengeId: Long) {
        viewModelScope.launch {
            try {
                val response = repository.speakQuestion(challengeId)
                if (response.isSuccessful) {
                    val audioBytes = response.body()?.bytes()
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        audioPlayer.playPcm(audioBytes)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun speakFeedback(text: String) {
        viewModelScope.launch {
            try {
                val response = repository.speakFeedback(text)
                if (response.isSuccessful) {
                    val audioBytes = response.body()?.bytes()
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        audioPlayer.playPcm(audioBytes)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetEvaluation() {
        _evaluationResult.value = null
    }
}

sealed class ChallengeUiState {
    object Loading : ChallengeUiState()
    object Success : ChallengeUiState()
    object Submitting : ChallengeUiState()
    object Evaluated : ChallengeUiState()
    data class Error(val message: String) : ChallengeUiState()
}

data class EvaluationResult(
    val score: Int,
    val encourage: String,
    val explanation: String?,
    val expEarned: Int,
    val isCorrect: Boolean,
    val stars: Int
)
