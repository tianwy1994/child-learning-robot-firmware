package com.childlearning.robot.data.repository

import android.content.Context
import android.net.Uri
import com.childlearning.robot.core.network.ApiService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 作业仓库
 *
 * 对应硬件服务端接口：
 * - POST /api/hardware/homework/ocr (multipart: file)
 * - POST /api/hardware/homework/submit (multipart: file + subject)
 */
@Singleton
class HomeworkRepository @Inject constructor(
    private val apiService: ApiService,
    private val context: Context
) {
    /**
     * 提交作业图片进行 OCR 识别
     */
    suspend fun submitForOcr(imageUri: Uri): Result<String?> {
        return try {
            val file = uriToFile(imageUri)
            val requestBody = file.asRequestBody("image/jpeg".toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

            val response = apiService.homeworkOcr(filePart)
            if (response.isSuccess) {
                Result.success(response.data?.text)
            } else {
                Result.failure(Exception("OCR 识别失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 提交作业（带科目）
     */
    suspend fun submitHomework(imageUri: Uri, subject: String): Result<Unit> {
        return try {
            val file = uriToFile(imageUri)
            val requestBody = file.asRequestBody("image/jpeg".toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)
            val subjectPart = subject.toRequestBody("text/plain".toMediaType())

            val response = apiService.submitHomework(filePart, subjectPart)
            if (response.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("提交作业失败"))
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