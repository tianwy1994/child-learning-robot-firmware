package com.childlearning.robot.domain.usecase

import com.childlearning.robot.core.network.AuthApiService
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
 * 认证用例 — 调用认证服务端 (port 8081)
 */
@Singleton
class AuthUseCase @Inject constructor(
    private val tokenStore: TokenStore,
    private val authApiService: AuthApiService
) {
    val authState: Flow<AuthState> = tokenStore.tokenFlow.map { token ->
        if (token.isNullOrBlank()) AuthState.Locked else AuthState.Authenticated
    }

    private val _unauthorizedEvent = MutableSharedFlow<Unit>()
    val unauthorizedEvent: SharedFlow<Unit> = _unauthorizedEvent

    /**
     * 账号密码登录 → 调用认证端 /api/auth/login
     */
    suspend fun login(phone: String, password: String): Result<LoginResponse> {
        return try {
            val response = authApiService.login(LoginRequest(phone, password))
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
     * 检查设备绑定状态
     */
    suspend fun checkDeviceBinding(deviceId: String): Result<DeviceBindingResult> {
        return try {
            val response = authApiService.getDeviceStatus(deviceId)
            if (response.isSuccess && response.data != null) {
                val data = response.data
                if (data.bound && data.tokenExpiresAt != null) {
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

    /**
     * 获取设备 Token
     */
    suspend fun getDeviceToken(deviceId: String): Result<String> {
        return try {
            val response = authApiService.getDeviceToken(deviceId)
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

sealed class DeviceBindingResult {
    data object Pending : DeviceBindingResult()
    data object Bound : DeviceBindingResult()
    data class Error(val message: String) : DeviceBindingResult()
}
