package com.childlearning.robot.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.core.network.GameProfileResponse
import com.childlearning.robot.domain.usecase.AuthUseCase
import com.childlearning.robot.domain.usecase.GameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authUseCase: AuthUseCase,
    private val gameUseCase: GameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadProfile()
        autoCheckin()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val result = gameUseCase.getProfile()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(profile = result.getOrNull())
            }
        }
    }

    private fun autoCheckin() {
        viewModelScope.launch {
            val result = gameUseCase.autoCheckin()
            if (result.isSuccess) {
                val checkinData = result.getOrNull()
                if (checkinData != null) {
                    _uiState.value = _uiState.value.copy(
                        checkinMessage = if (checkinData.firstCheckin) {
                            "签到成功！连续签到${checkinData.streakDays}天"
                        } else {
                            "今日已签到，连续${checkinData.streakDays}天"
                        }
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authUseCase.logout()
        }
    }
}

data class HomeUiState(
    val profile: GameProfileResponse? = null,
    val checkinMessage: String? = null
)
