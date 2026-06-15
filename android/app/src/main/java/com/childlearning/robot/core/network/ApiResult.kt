package com.childlearning.robot.core.network

import com.google.gson.annotations.SerializedName

/**
 * 统一 API 响应封装，对应服务端的 {code, data} 结构
 */
data class ApiResult<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: T?
) {
    val isSuccess: Boolean get() = code == 200
}

/**
 * 空数据响应
 */
class EmptyData
