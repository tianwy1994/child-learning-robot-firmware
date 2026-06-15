package com.childlearning.robot.ui.screens.homework

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.domain.usecase.HomeworkUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeworkViewModel @Inject constructor(
    private val homeworkUseCase: HomeworkUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeworkUiState())
    val uiState: StateFlow<HomeworkUiState> = _uiState

    fun submitPhoto(imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                selectedImageUri = imageUri
            )

            val result = homeworkUseCase.submitPhoto(imageUri)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(
                    isLoading = false,
                    ocrResult = result.getOrNull()
                )
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun submitHomework(subject: String) {
        val imageUri = _uiState.value.selectedImageUri ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = homeworkUseCase.submitHomework(imageUri, subject)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                submitSuccess = result.isSuccess,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(
            ocrResult = null,
            selectedImageUri = null,
            submitSuccess = false,
            subject = ""
        )
    }

    fun setSubject(subject: String) {
        _uiState.value = _uiState.value.copy(subject = subject)
    }
}

data class HomeworkUiState(
    val isLoading: Boolean = false,
    val selectedImageUri: Uri? = null,
    val ocrResult: String? = null,
    val submitSuccess: Boolean = false,
    val subject: String = "数学",
    val error: String? = null
)