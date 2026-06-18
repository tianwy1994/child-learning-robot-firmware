package com.childlearning.robot.domain.usecase

import android.net.Uri
import com.childlearning.robot.core.network.HomeworkStatusResponse
import com.childlearning.robot.data.repository.HomeworkRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeworkUseCase @Inject constructor(
    private val homeworkRepository: HomeworkRepository
) {
    suspend fun submitHomework(imageUri: Uri, subject: String): Result<Long> =
        homeworkRepository.submitHomework(imageUri, subject)

    suspend fun getHomeworkStatus(recordId: Long): Result<HomeworkStatusResponse> =
        homeworkRepository.getHomeworkStatus(recordId)
}
