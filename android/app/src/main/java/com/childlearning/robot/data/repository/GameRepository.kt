package com.childlearning.robot.data.repository

import com.childlearning.robot.core.network.ApiService
import com.childlearning.robot.core.network.CheckinResponse
import com.childlearning.robot.core.network.GameProfileResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 签到/积分仓库
 * 对应固件 GameManager — 管理每日签到和积分查询
 *
 * 固件逻辑：
 * - 每次 WiFi 连接后自动签到一次 (20 小时内不重复)
 * - 签到成功且 firstCheckin=true 时播放 encourage 预设语音
 * - getProfile 返回 level, experience, streakDays
 */
@Singleton
class GameRepository @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * 每日签到
     * 对应固件 GameManager::checkin()
     */
    suspend fun dailyCheckin(): Result<CheckinResponse> {
        return try {
            val response = apiService.dailyCheckin()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("签到失败: code=${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取积分档案
     * 对应固件 GameManager::getProfile()
     */
    suspend fun getProfile(): Result<GameProfileResponse> {
        return try {
            val response = apiService.getGameProfile()
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("获取档案失败: code=${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
