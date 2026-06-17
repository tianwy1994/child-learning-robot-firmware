package com.childlearning.robot.domain.usecase

import com.childlearning.robot.core.network.ApiService
import com.childlearning.robot.core.network.LoginRequest
import com.childlearning.robot.core.network.LoginResponse
import com.childlearning.robot.core.storage.TokenStore
import com.childlearning.robot.domain.enums.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 认证用例 - 管理用户登录和设备绑定状态
 *
 * 认证流程：
 * 1. 用户在手机App端注册
 * 2. 用户在本设备上使用手机号+密码登录 → user token
 * 3. 设备展示 deviceId，等待手机App绑定
 * 4. 手机App扫描/输入 deviceId → 调用 /api/auth/device/bind
 * 5. 设备轮询 /api/auth/device/status → 绑定成功 → 获得 device token
 * 6. 设备使用 device token 调用硬件服务端 API
 */
@Singleton
class AuthUseCase @Inject constructor(
    private val tokenStore: TokenStore,
    private val apiService: ApiService
) {
    val authState: Flow<AuthState> = tokenStore.tokenFlow.map { token ->
        if (token.isNullOrBlank()) AuthState.Locked else AuthState.Authenticated
    }

    private val _unauthorizedEvent = MutableSharedFlow<Unit>()
    val unauthorizedEvent: SharedFlow<Unit> = _unauthorizedEvent

    /**
     * 用户登录（手机号+密码）
     */
    suspend fun login(phone: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(phone, password))
            if (response.isSuccess && response.data != null) {
                tokenStore.saveToken(response.data.token)
                Result.success(response.data)
            } else {
                Result.failure(Exception("登录失败: code=${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 检查设备绑定状态（轮询）
     * 手机App绑定设备后，设备通过此方法获取 device token
     */
    suspend fun checkDeviceBinding(deviceId: String): Result<DeviceBindingResult> {
        return try {
            val response = apiService.getDeviceStatus(deviceId)
            if (response.isSuccess && response.data != null) {
                val data = response.data
                if (data.bound && data.tokenExpiresAt != null) {
                    // 绑定成功，但device token需要通过其他方式获取
                    // 实际上认证端在 /api/auth/device/bind 时返回 token
                    // 设备端通过轮询 status 只能知道绑定成功，token 需要再次获取
                    Result.success(DeviceBindingResult.Bound)
                } else {
                    Result.success(DeviceBindingResult.Pending)
                }
            } else {
                Result.failure(Exception("查询绑定状态失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDeviceToken(deviceId: String): Result<String> {
        return try {
            val response = apiService.getDeviceToken(deviceId)
            if (response.isSuccess && response.data != null) {
                val token = response.data.token
                tokenStore.saveToken(token)
                Result.success(token)
            } else {
                Result.failure(Exception("获取设备Token失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenStore.clearToken()
    }

    suspend fun handleUnauthorized() {
        tokenStore.clearToken()
        _unauthorizedEvent.emit(Unit)
    }
}

/**
 * 设备绑定状态
 */
sealed class DeviceBindingResult {
    data object Pending : DeviceBindingResult()
    data object Bound : DeviceBindingResult()
    data class Error(val message: String) : DeviceBindingResult()
}