package com.childlearning.robot.ui.screens.bind

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.core.storage.DeviceIdStore
import com.childlearning.robot.domain.usecase.AuthUseCase
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

    val deviceId: String
        get() = deviceIdStore.getDeviceId()

    init {
        // 生成唯一设备ID
        generateDeviceId()
        // 启动轮询绑定状态
        startPollingBindingStatus()
    }

    private fun generateDeviceId() {
        val existingId = deviceIdStore.getDeviceId()
        if (existingId.isBlank()) {
            // 生成唯一设备ID：使用Android ID + 时间戳哈希
            val androidId = Build.SERIAL + Build.ID + Build.MODEL + System.currentTimeMillis()
            val newDeviceId = androidId.hashCode().toUInt().toString(16).padStart(8, '0')
            deviceIdStore.saveDeviceId(newDeviceId)
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
