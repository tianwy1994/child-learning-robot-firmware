package com.childlearning.robot.data.repository

import com.childlearning.robot.core.network.ApiResult
import com.childlearning.robot.core.network.ApiService
import com.childlearning.robot.core.network.ChallengeDetailResponse
import com.childlearning.robot.core.network.ChallengeEvaluationResponse
import com.childlearning.robot.core.network.DailyChallengesResponse
import com.childlearning.robot.core.network.SkillProgressResponse
import okhttp3.ResponseBody
import retrofit2.Response
import javax.inject.Inject

class ChallengeRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getDailyChallenges(): ApiResult<DailyChallengesResponse> {
        return apiService.getDailyChallenges()
    }

    suspend fun getChallengeDetail(id: Long): ApiResult<ChallengeDetailResponse> {
        return apiService.getChallengeDetail(id)
    }

    suspend fun submitAnswer(id: Long, response: String): ApiResult<ChallengeEvaluationResponse> {
        return apiService.submitAnswer(id, ChallengeSubmitRequest(response))
    }

    suspend fun submitDragAnswer(id: Long, mapping: Map<String, String>): ApiResult<ChallengeEvaluationResponse> {
        return apiService.submitDragAnswer(id, ChallengeDragSubmitRequest(mapping))
    }

    suspend fun getProgress(): ApiResult<List<SkillProgressResponse>> {
        return apiService.getChallengeProgress()
    }

    suspend fun speakQuestion(id: Long): Response<ResponseBody> {
        return apiService.speakChallengeQuestion(id)
    }

    suspend fun speakFeedback(text: String): Response<ResponseBody> {
        return apiService.speakFeedback(text)
    }
}

data class ChallengeSubmitRequest(
    val response: String
)

data class ChallengeDragSubmitRequest(
    val mapping: Map<String, String>
)
