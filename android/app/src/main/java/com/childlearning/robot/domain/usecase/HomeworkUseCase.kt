package com.childlearning.robot.domain.usecase

import android.net.Uri
import com.childlearning.robot.data.repository.HomeworkRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 作业用例
 */
@Singleton
class HomeworkUseCase @Inject constructor(
    private val homeworkRepository: HomeworkRepository
) {
    /**
     * 提交作业图片进行 OCR 识别
     */
    suspend fun submitPhoto(imageUri: Uri): Result<String?> {
        return homeworkRepository.submitForOcr(imageUri)
    }

    /**
     * 提交作业（带科目）
     */
    suspend fun submitHomework(imageUri: Uri, subject: String): Result<Unit> {
        return homeworkRepository.submitHomework(imageUri, subject)
    }
}