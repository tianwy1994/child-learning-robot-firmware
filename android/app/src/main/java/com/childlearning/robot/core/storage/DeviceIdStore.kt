package com.childlearning.robot.core.storage

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设备 ID 持久化存储
 *
 * 对应固件: AuthManager::generateDeviceId()
 * 固件使用 WiFi MAC 地址作为设备 ID
 * Android 使用 ANDROID_ID + UUID 组合，首次生成后持久化
 *
 * 用途: 设备注册、心跳上报、设备绑定
 */
private val Context.deviceDataStore: DataStore<Preferences> by preferencesDataStore(name = "device")

@Singleton
class DeviceIdStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        private val KEY_DEVICE_TOKEN = stringPreferencesKey("device_token")
    }

    val deviceTokenFlow: Flow<String> = context.deviceDataStore.data.map { prefs ->
        prefs[KEY_DEVICE_TOKEN] ?: ""
    }

    suspend fun getDeviceToken(): String {
        return context.deviceDataStore.data.first()[KEY_DEVICE_TOKEN] ?: ""
    }

    suspend fun saveDeviceToken(token: String) {
        context.deviceDataStore.edit { prefs ->
            prefs[KEY_DEVICE_TOKEN] = token
        }
    }

    // KDoc removed
    val deviceIdFlow: Flow<String> = context.deviceDataStore.data.map { prefs ->
        prefs[KEY_DEVICE_ID] ?: ""
    }

    /**
     * 获取设备 ID（首次调用时生成并持久化）
     * 对应固件: generateDeviceId() → WiFi.macAddress()
     */
    suspend fun getDeviceId(): String {
        val existing = context.deviceDataStore.data.first()[KEY_DEVICE_ID]
        if (!existing.isNullOrBlank()) return existing

        // 生成新设备 ID
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: UUID.randomUUID().toString()

        // 组合 ANDROID_ID + 随机 UUID 生成唯一设备 ID
        val deviceId = "${androidId.takeLast(8)}-${UUID.randomUUID().toString().take(8)}".uppercase()

        // 持久化
        context.deviceDataStore.edit { prefs ->
            prefs[KEY_DEVICE_ID] = deviceId
        }

        return deviceId
    }

    /**
     * 获取设备型号信息（用于上报）
     */
    fun getDeviceModel(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    /**
     * 获取系统版本
     */
    fun getOsVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }
}