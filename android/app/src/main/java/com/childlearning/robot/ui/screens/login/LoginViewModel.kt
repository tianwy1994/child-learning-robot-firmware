package com.childlearning.robot.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.core.storage.DeviceIdStore
import com.childlearning.robot.core.storage.TokenStore
import com.childlearning.robot.domain.usecase.AuthUseCase
import com.childlearning.robot.domain.usecase.DeviceBindingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录 ViewModel
 *
 * 认证流程：
 * 1. 用户输入手机号+密码 → 登录 → 获得 user token
 * 2. 登录成功后 → 展示设备ID → 等待手机App绑定
 * 3. 设备轮询绑定状态 → 绑定成功 → 获得 device token → 进入主页
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authUseCase: AuthUseCase,
    private val deviceIdStore: DeviceIdStore,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private var pollJob: Job? = null

    init {
        // 加载设备ID
        viewModelScope.launch {
            val deviceId = deviceIdStore.getDeviceId()
            _uiState.value = _uiState.value.copy(deviceId = deviceId)
        }
    }

    // ==================== 用户登录 ====================

    fun login(phone: String, password: String) {
        if (phone.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入手机号和密码")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authUseCase.login(phone, password)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(
                    isLoading = false,
                    loginSuccess = true,
                    activationStep = ActivationStep.WAITING_FOR_BIND
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "登录失败"
                )
            }
        }
    }

    // ==================== 设备绑定 ====================

    /**
     * 开始轮询设备绑定状态
     * 登录成功后自动调用
     */
    fun startPollingBinding() {
        val deviceId = _uiState.value.deviceId ?: return
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val timeoutMs = 5 * 60 * 1000L // 5 分钟超时

            while (_uiState.value.activationStep == ActivationStep.WAITING_FOR_BIND) {
                delay(2000)

                if (System.currentTimeMillis() - startTime > timeoutMs) {
                    _uiState.value = _uiState.value.copy(
                        activationStep = ActivationStep.TIMEOUT,
                        error = "绑定超时，请重新操作"
                    )
                    return@launch
                }

                val result = authUseCase.checkDeviceBinding(deviceId)
                if (result.isSuccess) {
                    when (result.getOrThrow()) {
                        is DeviceBindingResult.Bound -> {
                            _uiState.value = _uiState.value.copy(
                                activationStep = ActivationStep.BOUND,
                                loginSuccess = true
                            )
                            return@launch
                        }
                        is DeviceBindingResult.Pending -> {
                            // 继续轮询
                        }
                        is DeviceBindingResult.Error -> {
                            // 继续轮询
                        }
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshBinding() {
        _uiState.value = _uiState.value.copy(
            activationStep = ActivationStep.WAITING_FOR_BIND,
            error = null
        )
        startPollingBinding()
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}

enum class ActivationStep {
    LOGIN,           // 登录阶段
    WAITING_FOR_BIND, // 等待绑定
    BOUND,           // 已绑定
    TIMEOUT          // 超时
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val error: String? = null,
    val deviceId: String? = null,
    val activationStep: ActivationStep = ActivationStep.LOGIN
)