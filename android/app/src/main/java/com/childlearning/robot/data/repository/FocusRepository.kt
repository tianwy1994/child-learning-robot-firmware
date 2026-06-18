package com.childlearning.robot.data.repository

import com.childlearning.robot.core.network.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 专注模式仓库
 * 对应固件 FocusManager — 管理专注会话和提醒轮询
 *
 * 固件逻辑：
 * - startFocus: POST /focus/start，传 taskDescription
 * - endFocus: POST /focus/end
 * - getStatus: GET /focus/status，每 30 秒轮询
 * - ackReminder: POST /focus/reminder/ack，确认提醒已播放
 * - 提醒类型: POSTURE (坐姿), BREAK (休息), CONTINUE (继续)
 */
@Singleton
class FocusRepository @Inject constructor(
    private val apiService: ApiService
) {
    /**
     * 开始专注
     * 对应固件 FocusManager::startFocus(taskDescription)
     */
    suspend fun startFocus(taskDescription: String): Result<Unit> {
        return try {
            val response = apiService.startFocus(FocusStartRequest(taskDescription))
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("开始专注失败: code=${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 结束专注
     * 对应固件 FocusManager::endFocus()
     */
    suspend fun endFocus(): Result<Unit> {
        return try {
            val response = apiService.endFocus()
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("结束专注失败: code=${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取专注状态
     * 对应固件 FocusManager::getStatus() — 每 30 秒轮询
     * 返回 null 表示无活跃会话，返回 FocusStatusData 包含可能的提醒
     */
    suspend fun getStatus(): Result<FocusSessionResponse?> {
        return try {
            val response = apiService.getFocusStatus()
            if (response.isSuccess) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("获取专注状态失败: code=${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 确认提醒
     * 对应固件 FocusManager::ackReminder(type)
     */
    suspend fun ackReminder(type: String): Result<Unit> {
        return try {
            val response = apiService.ackReminder(ReminderAckRequest(type))
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("确认提醒失败: code=${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
