package com.childlearning.robot.core.network

import retrofit2.http.*

/**
 * 认证服务端 API (port 8081)
 * 处理登录、设备绑定等认证相关接口
 */
interface AuthApiService {

    @POST("api/auth/login")
    suspend fun login(
        @Body body: LoginRequest
    ): ApiResult<LoginResponse>

    @GET("api/auth/device/status")
    suspend fun getDeviceStatus(
        @Query("deviceId") deviceId: String
    ): ApiResult<DeviceStatusResponse>

    @POST("api/auth/device/bind")
    suspend fun bindDevice(
        @Body body: DeviceBindRequest
    ): ApiResult<DeviceBindResponse>

    @GET("api/auth/device/token")
    suspend fun getDeviceToken(
        @Query("deviceId") deviceId: String
    ): ApiResult<DeviceTokenResponse>
}
