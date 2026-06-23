package com.childlearning.robot.ui.screens.emotion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.core.network.ApiService
import com.childlearning.robot.core.network.EmotionCheckinRequest
import com.childlearning.robot.core.network.EmotionDayResponse
import com.childlearning.robot.core.network.TodayEmotionResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EmotionUiState(
    val todayEmotion: TodayEmotionResponse? = null,
    val weeklyEmotions: List<EmotionDayResponse> = emptyList(),
    val selectedEmotion: String? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class EmotionViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmotionUiState())
    val uiState: StateFlow<EmotionUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val today = apiService.getTodayEmotion()
                val weekly = apiService.getWeeklyEmotion()
                _uiState.update {
                    it.copy(
                        todayEmotion = today.data,
                        weeklyEmotions = weekly.data ?: emptyList(),
                        selectedEmotion = today.data?.emoji
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun selectEmotion(emoji: String) = _uiState.update { it.copy(selectedEmotion = emoji) }

    fun saveEmotion(onSuccess: () -> Unit) {
        val emotion = _uiState.value.selectedEmotion ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                apiService.checkinEmotion(EmotionCheckinRequest(emoji = emotion))
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()
            } catch (_: Exception) {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
