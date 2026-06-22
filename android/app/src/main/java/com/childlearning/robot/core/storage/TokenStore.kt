package com.childlearning.robot.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_TOKEN = stringPreferencesKey("device_token")
        private val KEY_NICKNAME = stringPreferencesKey("child_nickname")
    }

    val tokenFlow: Flow<String?> = context.tokenDataStore.data.map { prefs ->
        prefs[KEY_TOKEN]
    }

    val nicknameFlow: Flow<String?> = context.tokenDataStore.data.map { prefs ->
        prefs[KEY_NICKNAME]
    }

    suspend fun saveToken(token: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
        }
    }

    suspend fun saveNickname(nickname: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[KEY_NICKNAME] = nickname
        }
    }

    suspend fun clearToken() {
        context.tokenDataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_NICKNAME)
        }
    }
}