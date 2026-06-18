package com.childlearning.robot.core.di

import android.content.Context
import com.childlearning.robot.core.audio.AudioRecorder
import com.childlearning.robot.core.audio.PcmAudioPlayer
import com.childlearning.robot.core.network.ApiService
import com.childlearning.robot.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Hilt 依赖注入模块
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideChatRepository(apiService: ApiService): ChatRepository {
        return ChatRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideSttRepository(
        apiService: ApiService,
        audioRecorder: AudioRecorder
    ): SttRepository {
        return SttRepository(apiService, audioRecorder)
    }

    @Provides
    @Singleton
    fun provideFocusRepository(apiService: ApiService): FocusRepository {
        return FocusRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideGameRepository(apiService: ApiService): GameRepository {
        return GameRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideHomeworkRepository(
        apiService: ApiService,
        @ApplicationContext context: Context
    ): HomeworkRepository {
        return HomeworkRepository(apiService, context)
    }

    @Provides
    @Singleton
    fun provideChallengeRepository(apiService: ApiService): ChallengeRepository {
        return ChallengeRepository(apiService)
    }

    @Provides
    @Singleton
    fun providePcmAudioPlayer(): PcmAudioPlayer {
        return PcmAudioPlayer()
    }
}