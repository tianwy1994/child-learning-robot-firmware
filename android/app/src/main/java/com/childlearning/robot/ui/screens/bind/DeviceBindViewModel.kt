package com.childlearning.robot.ui.screens.bind

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.core.storage.DeviceIdStore
import com.childlearning.robot.domain.usecase.AuthUseCase
import com.childlearning.robot.domain.usecase.DeviceBindingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceBindViewModel @Inject constructor(
    private val authUseCase: AuthUseCase,
    private val deviceIdStore: DeviceIdStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<BindUiState>(BindUiState.Loading)
    val uiState: StateFlow<BindUiState> = _uiState.asStateFlow()

    // Device ID is initialized asynchronously; show placeholder until ready
    var deviceId: String = ""
        private set

    init {
        // Generate/retrieve device ID then start polling
        viewModelScope.launch {
            deviceId = deviceIdStore.getDeviceId()
            startPollingBindingStatus()
        }
    }

    private fun startPollingBindingStatus() {
        viewModelScope.launch {
            _uiState.value = BindUiState.WaitingForBind(deviceId)

            // 每3秒轮询一次绑定状态，最多轮询30分钟
            repeat(600) { // 3s * 600 = 30分钟
                try {
                    val result = authUseCase.checkDeviceBinding(deviceId)
                    result.onSuccess { bindingResult ->
                        if (bindingResult == DeviceBindingResult.Bound) {
                            // 绑定成功，获取设备token
                            val tokenResult = authUseCase.getDeviceToken(deviceId)
                            tokenResult.onSuccess { token ->
                                deviceIdStore.saveDeviceToken(token)
                                _uiState.value = BindUiState.Bound
                                return@launch
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(3000)
            }

            // 超时
            _uiState.value = BindUiState.Error("绑定超时，请重试")
        }
    }
}
