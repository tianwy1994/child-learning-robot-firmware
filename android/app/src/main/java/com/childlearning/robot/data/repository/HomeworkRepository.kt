package com.childlearning.robot.data.repository

import android.content.Context
import android.net.Uri
import com.childlearning.robot.core.network.ApiService
import com.childlearning.robot.core.network.HomeworkStatusResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeworkRepository @Inject constructor(
    private val apiService: ApiService,
    private val context: Context
) {
    data class HomeworkResult(
        val ocrText: String?,
        val score: Int?,
        val feedback: String?
    )

    /** 提交作业，立即返回 recordId，服务端后台异步批改 */
    suspend fun submitHomework(imageUri: Uri, subject: String): Result<Long> {
        return try {
            val file = uriToFile(imageUri)
            val requestBody = file.asRequestBody("image/jpeg".toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)
            val subjectPart = subject.toRequestBody("text/plain".toMediaType())

            val response = apiService.submitHomework(filePart, subjectPart)
            if (response.isSuccess) {
                Result.success(response.data!!.recordId)
            } else {
                Result.failure(Exception("提交失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 查询批改状态，status: PENDING / PROCESSING / COMPLETED / FAILED */
    suspend fun getHomeworkStatus(recordId: Long): Result<HomeworkStatusResponse> {
        return try {
            val response = apiService.getHomeworkStatus(recordId)
            if (response.isSuccess && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("查询状态失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("无法读取图片")
        val tempFile = File.createTempFile("homework_", ".jpg", context.cacheDir)
        tempFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()
        return tempFile
    }
}
