package com.childlearning.robot.domain.usecase

import com.childlearning.robot.core.audio.TtsPlayer
import com.childlearning.robot.core.network.CheckinResponse
import com.childlearning.robot.core.network.GameProfileResponse
import com.childlearning.robot.data.repository.GameRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 签到/积分用例
 * 对应固件 GameManager — 每日签到和积分查询
 *
 * 固件逻辑：
 * - 每次 WiFi 连接后自动签到 (20 小时防重复)
 * - firstCheckin=true 时播放 encourage 预设语音
 * - announceProfile() 通过 TTS 播报等级信息
 */
@Singleton
class GameUseCase @Inject constructor(
    private val gameRepository: GameRepository,
    private val ttsPlayer: TtsPlayer
) {
    private var lastCheckinTime = 0L
    private val checkinIntervalMs = 20 * 60 * 60 * 1000L  // 20 小时

    /**
     * 自动签到（带 20 小时防重复）
     * 对应固件 GameManager::autoCheckin() — WiFi 连接后调用
     */
    suspend fun autoCheckin(): Result<CheckinResponse?> {
        val now = System.currentTimeMillis()
        if (now - lastCheckinTime < checkinIntervalMs) {
            return Result.success(null) // 跳过
        }

        val result = gameRepository.dailyCheckin()
        if (result.isSuccess) {
            lastCheckinTime = now
            val data = result.getOrThrow()
            // 首次签到播放鼓励语音，对应固件 audioPlayer.playPreset("encourage")
            if (data.firstCheckin) {
                ttsPlayer.playPreset("encourage")
            }
        }
        return result
    }

    /**
     * 获取积分档案
     * 对应固件 GameManager::getProfile()
     */
    suspend fun getProfile(): Result<GameProfileResponse> {
        return gameRepository.getProfile()
    }

    /**
     * 语音播报积分
     * 对应固件 GameManager::announceProfile()
     */
    suspend fun announceProfile(): Result<Unit> {
        val result = gameRepository.getProfile()
        return result.map { profile ->
            val text = "你现在是${profile.level}级，经验${profile.experience}，" +
                    "连续签到${profile.streakDays}天"
            ttsPlayer.speak(text)
        }
    }
}
