package org.example.project.auth.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.model.profile.Profile
import org.example.project.core.navigation.Route
import org.example.project.core.utils.DataState

sealed class NameCaptureEffect {
    data object NavigateToNextScreen : NameCaptureEffect()
    data class ShowSnackbar(val message: String) : NameCaptureEffect()
}

data class NameCaptureUiState(
    val name: String = "",
    val dialogState: DialogState? = null
) {
    sealed interface DialogState {
        data object Loading : DialogState
    }
}
sealed class NameCaptureIntent{
    data class NameChanged(val name: String) : NameCaptureIntent()
    data object SubmitClicked : NameCaptureIntent()
    data object DismissDialog : NameCaptureIntent()
}

class NameCaptureViewModel(
    private val email : String,
    private val prefRepository: UserPreferencesRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {


    private val _uiState = MutableStateFlow(NameCaptureUiState())
    val uiState: StateFlow<NameCaptureUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<NameCaptureEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val effect: SharedFlow<NameCaptureEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: NameCaptureIntent) {
        when (intent) {
            is NameCaptureIntent.NameChanged -> updateName(intent.name)
            is NameCaptureIntent.SubmitClicked -> submitProfile()
            is NameCaptureIntent.DismissDialog -> clearDialog()
        }
    }

    private fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }


    private fun submitProfile() {
        val currentName = _uiState.value.name.trim()

        if (currentName.isEmpty()) {
            showError("Please enter your name")
            return
        }

        viewModelScope.launch {
            showLoading()
            
            val profile = Profile(
                name = currentName,
                email = email.trim(),
                imageUrl = "",  // to be updated
                totalPosts = 0,
                acks = 0,
                postByArea = listOf(0, 0, 0, 0),
                myPosts = emptyList(),
                ackPosts = emptyList()
            )
            
            when (val result = profileRepository.updateProfile(profile)) {
                is DataState.Success -> {
                    _effect.emit(NameCaptureEffect.NavigateToNextScreen)
                    prefRepository.setLoggedIn(true)

                }
                is DataState.Error -> {
                    hideLoading()
                    showError(result.exception.message ?: "Failed to update profile")
                }
                DataState.Loading -> {}
            }
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
        viewModelScope.launch {
            _effect.emit(NameCaptureEffect.ShowSnackbar(message))
        }
    }

    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\\\.[a-zA-z]{2,}\$".toRegex()
}
