package com.childlearning.robot.ui.screens.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.core.network.CheckinResponse
import com.childlearning.robot.core.network.GameProfileResponse
import com.childlearning.robot.domain.usecase.GameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameUseCase: GameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = gameUseCase.getProfile()
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(
                    isLoading = false,
                    profile = result.getOrNull()
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun checkin() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = gameUseCase.autoCheckin()
            _uiState.value = if (result.isSuccess) {
                val data = result.getOrNull()
                _uiState.value.copy(
                    isLoading = false,
                    checkinResult = data,
                    checkinMessage = data?.let {
                        if (it.firstCheckin) "签到成功！连续签到${it.streakDays}天"
                        else "今日已签到"
                    }
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
            // 刷新档案
            loadProfile()
        }
    }
}

data class GameUiState(
    val isLoading: Boolean = false,
    val profile: GameProfileResponse? = null,
    val checkinResult: CheckinResponse? = null,
    val checkinMessage: String? = null,
    val error: String? = null
)
