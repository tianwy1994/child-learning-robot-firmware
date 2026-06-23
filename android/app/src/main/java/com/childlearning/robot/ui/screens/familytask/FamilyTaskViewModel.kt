package com.childlearning.robot.ui.screens.familytask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.childlearning.robot.core.network.ApiService
import com.childlearning.robot.core.network.FamilyTaskCompleteRequest
import com.childlearning.robot.core.network.FamilyTaskResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyTaskUiState(
    val tasks: List<FamilyTaskResponse> = emptyList(),
    val isLoading: Boolean = false,
    val completingTask: FamilyTaskResponse? = null,
    val completeText: String = "",
    val isSubmitting: Boolean = false
)

@HiltViewModel
class FamilyTaskViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyTaskUiState())
    val uiState: StateFlow<FamilyTaskUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = apiService.getFamilyTasks()
                _uiState.update { it.copy(tasks = result.data ?: emptyList(), isLoading = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun showCompleteDialog(task: FamilyTaskResponse) =
        _uiState.update { it.copy(completingTask = task, completeText = "") }

    fun dismissCompleteDialog() =
        _uiState.update { it.copy(completingTask = null, completeText = "") }

    fun onCompleteTextChange(text: String) = _uiState.update { it.copy(completeText = text) }

    fun submitComplete() {
        val task = _uiState.value.completingTask ?: return
        val text = _uiState.value.completeText.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            try {
                apiService.completeFamilyTask(task.id, FamilyTaskCompleteRequest(text = text))
                _uiState.update { it.copy(isSubmitting = false, completingTask = null, completeText = "") }
                loadTasks()
            } catch (_: Exception) {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
