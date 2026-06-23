package com.childlearning.robot.ui.screens.gratitude

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.core.network.ApiService
import com.childlearning.robot.core.network.GratitudeCreateRequest
import com.childlearning.robot.core.network.GratitudeEntry
import com.childlearning.robot.core.network.GratitudeTreeStats
import com.childlearning.robot.data.repository.SttRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GratitudeUiState(
    val entries: List<GratitudeEntry> = emptyList(),
    val treeStats: GratitudeTreeStats? = null,
    val inputContent: String = "",
    val inputTarget: String = "",
    val isRecording: Boolean = false,
    val isSubmitting: Boolean = false,
    val isLoading: Boolean = false,
    val submitSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GratitudeViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sttRepository: SttRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GratitudeUiState())
    val uiState: StateFlow<GratitudeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val listResult = apiService.getGratitudeList()
                val treeResult = apiService.getGratitudeTree()
                _uiState.update {
                    it.copy(
                        entries = listResult.data?.content ?: emptyList(),
                        treeStats = treeResult.data,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onContentChange(value: String) = _uiState.update { it.copy(inputContent = value) }
    fun onTargetChange(value: String) = _uiState.update { it.copy(inputTarget = value) }

    fun startRecording() {
        _uiState.update { it.copy(isRecording = true) }
    }

    fun stopRecordingAndRecognize() {
        if (!_uiState.value.isRecording) return
        _uiState.update { it.copy(isRecording = false) }
        viewModelScope.launch {
            val result = sttRepository.recordAndRecognize()
            result.onSuccess { text ->
                val current = _uiState.value.inputContent
                val appended = if (current.isBlank()) text else "$current $text"
                _uiState.update { it.copy(inputContent = appended) }
            }
        }
    }

    fun submitGratitude() {
        val content = _uiState.value.inputContent.trim()
        if (content.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                val request = GratitudeCreateRequest(
                    content = content,
                    gratitudeTarget = _uiState.value.inputTarget.takeIf { it.isNotBlank() }
                )
                apiService.createGratitude(request)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        inputContent = "",
                        inputTarget = "",
                        submitSuccess = true
                    )
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, error = e.message) }
            }
        }
    }

    fun clearSubmitSuccess() = _uiState.update { it.copy(submitSuccess = false) }
}
