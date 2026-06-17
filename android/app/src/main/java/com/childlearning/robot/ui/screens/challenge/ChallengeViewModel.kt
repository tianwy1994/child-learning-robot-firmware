package com.childlearning.robot.ui.screens.challenge

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.core.audio.AudioPlayer
import com.childlearning.robot.core.network.ChallengeCard
import com.childlearning.robot.core.network.ChallengeDetailResponse
import com.childlearning.robot.data.repository.ChallengeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ChallengeViewModel @Inject constructor(
    private val repository: ChallengeRepository,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChallengeUiState>(ChallengeUiState.Loading)
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    private val _dailyChallenges = MutableStateFlow<List<ChallengeCard>>(emptyList())
    val dailyChallenges: StateFlow<List<ChallengeCard>> = _dailyChallenges.asStateFlow()

    private val _currentChallenge = MutableStateFlow<ChallengeDetailResponse?>(null)
    val currentChallenge: StateFlow<ChallengeDetailResponse?> = _currentChallenge.asStateFlow()

    private val _evaluationResult = MutableStateFlow<EvaluationResult?>(null)
    val evaluationResult: StateFlow<EvaluationResult?> = _evaluationResult.asStateFlow()

    fun loadDailyChallenges() {
        viewModelScope.launch {
            _uiState.value = ChallengeUiState.Loading
            try {
                val result = repository.getDailyChallenges()
                if (result.isSuccess()) {
                    val allChallenges = result.data!!.pending + result.data.completed
                    _dailyChallenges.value = allChallenges
                    _uiState.value = ChallengeUiState.Success
                } else {
                    _uiState.value = ChallengeUiState.Error(result.message ?: "加载失败")
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
                if (result.isSuccess()) {
                    _currentChallenge.value = result.data!!
                    _uiState.value = ChallengeUiState.Success
                } else {
                    _uiState.value = ChallengeUiState.Error(result.message ?: "加载失败")
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
                if (result.isSuccess()) {
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

                    // 播放音效
                    if (eval.score >= 80) {
                        playSound("win")
                    } else if (eval.score >= 60) {
                        playSound("correct")
                    } else {
                        playSound("wrong")
                    }

                    // 朗读反馈
                    val speakText = "${eval.encourage} ${eval.explanation ?: ""}"
                    speakText(speakText)
                } else {
                    _uiState.value = ChallengeUiState.Error(result.message ?: "提交失败")
                }
            } catch (e: Exception) {
                _uiState.value = ChallengeUiState.Error(e.message ?: "网络错误")
            }
        }
    }

    fun speakQuestion(challengeId: Long) {
        viewModelScope.launch {
            try {
                val response = repository.speakQuestion(challengeId)
                if (response.isSuccessful) {
                    response.body()?.byteStream()?.use { inputStream ->
                        audioPlayer.playAudioStream(inputStream)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun speakText(text: String) {
        viewModelScope.launch {
            try {
                val response = repository.speakFeedback(text)
                if (response.isSuccessful) {
                    response.body()?.byteStream()?.use { inputStream ->
                        audioPlayer.playAudioStream(inputStream)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun playSound(type: String) {
        // 播放对应音效
        viewModelScope.launch {
            try {
                val resId = when (type) {
                    "win" -> android.R.raw.sound_win
                    "correct" -> android.R.raw.sound_correct
                    "wrong" -> android.R.raw.sound_wrong
                    else -> android.R.raw.sound_correct
                }
                // 实际项目中替换为自己的音效资源
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
