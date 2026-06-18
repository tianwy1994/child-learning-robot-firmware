package com.childlearning.robot.core.network

import com.childlearning.robot.core.storage.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicReference

/**
 * OkHttp 拦截器 — 自动添加 Bearer token，拦截 401。
 *
 * Token 缓存到内存 AtomicReference，由协程监听 DataStore 变更实时更新，
 * 避免在 OkHttp IO 线程上 runBlocking 阻塞。
 */
class AuthInterceptor(
    tokenStore: TokenStore
) : Interceptor {

    var onUnauthorized: (() -> Unit)? = null

    private val cachedToken = AtomicReference<String?>(null)

    init {
        // 在后台协程订阅 DataStore，token 变化时更新缓存
        tokenStore.tokenFlow
            .onEach { cachedToken.set(it) }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = cachedToken.get()
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        if (response.code == 401) {
            cachedToken.set(null)
            onUnauthorized?.invoke()
        }

        return response
    }
}
