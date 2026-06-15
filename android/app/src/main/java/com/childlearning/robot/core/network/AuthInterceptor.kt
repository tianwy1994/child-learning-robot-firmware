package com.childlearning.robot.core.network

import com.childlearning.robot.core.storage.TokenStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

// OkHttp 拦截器 - 自动添加 Bearer token，拦截 401 响应

class AuthInterceptor(
    private val tokenStore: TokenStore
) : Interceptor {

    // 401 回调，由 MainActivity 注册
    var onUnauthorized: (() -> Unit)? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStore.tokenFlow.first() }
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        if (response.code == 401 && !token.isNullOrBlank()) {
            runBlocking { tokenStore.clearToken() }
            onUnauthorized?.invoke()
        }

        return response
    }
}