package org.example.project.auth.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.datastore.UserPreferencesRepository

class NameCaptureViewModel(
    private val prefRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NameCaptureUiState())
    val uiState: StateFlow<NameCaptureUiState> = _uiState.asStateFlow()


    fun handleIntent(intent: NameCaptureIntent) {
        when (intent) {
            is NameCaptureIntent.NameChanged -> updateName(intent.name)
            is NameCaptureIntent.EmailChanged -> updateEmail(intent.email)
            is NameCaptureIntent.SubmitClicked -> submitProfile()
            is NameCaptureIntent.DismissDialog -> clearDialog()
        }
    }

    private fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    private fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    private fun submitProfile() {
        val currentName = _uiState.value.name.trim()
        val currentEmail = _uiState.value.email.trim()

        if (currentName.isEmpty()) {
            showError("Please enter your name")
            return
        }

        if (currentEmail.isEmpty() || !emailRegex.matches(currentEmail)) {
            showError("Please enter a valid email address")
            return
        }

        viewModelScope.launch {
            showLoading()
            prefRepository.updateName(currentName)
            prefRepository.setLoggedIn(true)
            hideLoading()
        }
    }

    private fun clearDialog() {
        _uiState.update { it.copy(dialogState = null) }
    }

    private fun showLoading() {
        _uiState.update { it.copy(dialogState = NameCaptureUiState.DialogState.Loading) }
    }

    private fun hideLoading() {
        _uiState.update { it.copy(dialogState = null) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(dialogState = NameCaptureUiState.DialogState.Error(message)) }
    }

    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-z]{2,}\$".toRegex()
}

sealed class NameCaptureIntent {
    data class NameChanged(val name: String) : NameCaptureIntent()
    data class EmailChanged(val email: String) : NameCaptureIntent()
    data object SubmitClicked : NameCaptureIntent()
    data object DismissDialog : NameCaptureIntent()
}

sealed class NameCaptureEffect {
    data object NavigateToNextScreen : NameCaptureEffect()
}

data class NameCaptureUiState(
    val name: String = "",
    val email: String = "",
    val dialogState: DialogState? = null
) {
    sealed interface DialogState {
        data class Error(val message: String) : DialogState
        data object Loading : DialogState
    }
}
