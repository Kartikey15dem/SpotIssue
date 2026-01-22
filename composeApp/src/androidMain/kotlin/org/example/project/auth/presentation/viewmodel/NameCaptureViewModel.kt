package org.example.project.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NameCaptureUiState(
    val name: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class NameCaptureViewModel(
) : ViewModel() {

    private val _uiState = MutableStateFlow(NameCaptureUiState())
    val uiState: StateFlow<NameCaptureUiState> = _uiState.asStateFlow()

    fun setEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name, error = null)
    }

    fun confirmName(onSuccess: (String) -> Unit) {
        val current = _uiState.value
        val trimmed = current.name.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = current.copy(error = "Please enter your name")
            return
        }

        _uiState.value = current.copy(isLoading = true, error = null)

        viewModelScope.launch {
            // Pass the trimmed name to the callback
            _uiState.value = current.copy(isLoading = false)
            onSuccess(trimmed)
        }
    }
}

