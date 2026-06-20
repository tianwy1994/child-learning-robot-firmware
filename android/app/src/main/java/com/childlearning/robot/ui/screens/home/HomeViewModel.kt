package com.childlearning.robot.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.core.network.*
import com.childlearning.robot.core.audio.TtsPlayer
import com.childlearning.robot.domain.usecase.AuthUseCase
import com.childlearning.robot.domain.usecase.GameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "HomeVM"

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authUseCase: AuthUseCase,
    private val gameUseCase: GameUseCase,
    private val apiService: ApiService,
    private val ttsPlayer: TtsPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadProfile()
        autoCheckin()
        loadTodayQuests()
        loadDailySurprise()
        startProactivePolling()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val result = gameUseCase.getProfile()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(profile = result.getOrNull())
            }
        }
    }

    private fun autoCheckin() {
        viewModelScope.launch {
            val result = gameUseCase.autoCheckin()
            if (result.isSuccess) {
                val checkinData = result.getOrNull()
                if (checkinData != null) {
                    _uiState.value = _uiState.value.copy(
                        checkinMessage = if (checkinData.firstCheckin) {
                            "签到成功！连续签到${checkinData.streakDays}天"
                        } else {
                            "今日已签到，连续${checkinData.streakDays}天"
                        }
                    )
                }
            }
        }
    }

    private fun loadTodayQuests() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "正在加载每日任务...")
                val response = apiService.getTodayQuests()
                Log.d(TAG, "每日任务响应: code=${response.code}, data=${response.data?.size ?: "null"}")
                if (response.isSuccess && response.data != null) {
                    _uiState.value = _uiState.value.copy(quests = response.data)
                    Log.d(TAG, "每日任务加载成功: ${response.data.size} 个任务")
                } else {
                    Log.w(TAG, "每日任务加载失败: code=${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "每日任务请求异常: ${e.message}", e)
            }
        }
    }

    private fun loadDailySurprise() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "正在加载每日惊喜...")
                val response = apiService.getDailySurprise()
                Log.d(TAG, "每日惊喜响应: code=${response.code}, data=${response.data}")
                if (response.isSuccess && response.data != null) {
                    val surprise = response.data
                    Log.d(TAG, "每日惊喜: type=${surprise.type}, alreadyClaimed=${surprise.alreadyClaimed}")
                    if (!surprise.alreadyClaimed && surprise.type != "NOTHING") {
                        _uiState.value = _uiState.value.copy(surprise = surprise)
                        Log.d(TAG, "显示每日惊喜弹窗")
                    }
                } else {
                    Log.w(TAG, "每日惊喜加载失败: code=${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "每日惊喜请求异常: ${e.message}", e)
            }
        }
    }

    private fun startProactivePolling() {
        viewModelScope.launch {
            // 首次启动等 10 秒再轮询（避免和页面加载竞争）
            delay(10_000)
            while (isActive) {
                try {
                    Log.d(TAG, "轮询主动消息...")
                    val response = apiService.getProactiveMessages()
                    Log.d(TAG, "主动消息响应: code=${response.code}, data=${response.data?.size ?: "null"}")
                    if (response.isSuccess && response.data != null) {
                        val messages = response.data
                        for (msg in messages) {
                            Log.d(TAG, "播放主动消息: ${msg.message}")
                            try {
                                ttsPlayer.speak(msg.message)
                            } catch (e: Exception) {
                                Log.e(TAG, "TTS播放失败: ${e.message}")
                            }
                            _uiState.value = _uiState.value.copy(
                                proactiveMessage = msg.message
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "主动消息轮询异常: ${e.message}", e)
                }
                delay(60_000)
            }
        }
    }

    fun dismissSurprise() {
        _uiState.value = _uiState.value.copy(surprise = null)
    }

    fun dismissProactiveMessage() {
        _uiState.value = _uiState.value.copy(proactiveMessage = null)
    }

    fun refreshProfile() {
        loadProfile()
        loadTodayQuests()
    }

    fun logout() {
        viewModelScope.launch {
            authUseCase.logout()
        }
    }
}

data class HomeUiState(
    val profile: GameProfileResponse? = null,
    val checkinMessage: String? = null,
    val quests: List<DailyQuestResponse>? = null,
    val surprise: DailySurpriseResponse? = null,
    val proactiveMessage: String? = null
)
