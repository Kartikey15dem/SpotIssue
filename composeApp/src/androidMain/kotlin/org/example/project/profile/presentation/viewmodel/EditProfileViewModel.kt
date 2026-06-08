package org.example.project.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.model.profile.Profile
import org.example.project.core.utils.DataState
import android.net.Uri

class EditProfileViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileState())
    val uiState: StateFlow<EditProfileState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<EditProfileSideEffect>()
    val sideEffects: SharedFlow<EditProfileSideEffect> = _sideEffects.asSharedFlow()

    init {
        observeProfile()
        loadProfile()
    }

    fun onIntent(intent: EditProfileIntent) {
        when (intent) {
            EditProfileIntent.LoadProfile -> loadProfile()
            is EditProfileIntent.ImageUrlChanged -> updateImageUrl(intent.url)
            is EditProfileIntent.NameChanged -> updateName(intent.name)
            EditProfileIntent.PickFromGalleryClicked -> pickFromGallery()
            EditProfileIntent.CaptureFromCameraClicked -> captureFromCamera()
            EditProfileIntent.SaveChangesClicked -> saveChanges()
            EditProfileIntent.ResetClicked -> resetToOriginal()
            EditProfileIntent.DismissImagePicker -> dismissImagePicker()
            EditProfileIntent.BackPressed -> viewModelScope.launch { _sideEffects.emit(EditProfileSideEffect.BackPreseed) }
            EditProfileIntent.ErrorShown -> clearError()
            EditProfileIntent.RequestEmailChangeClicked -> requestEmailChange()
            is EditProfileIntent.VerifyEmailChangeClicked -> verifyEmailChange(intent.otp)
            EditProfileIntent.DismissEmailChangeDialog -> dismissEmailChangeDialog()
            is EditProfileIntent.NewEmailChanged -> updateNewEmail(intent.email)
            is EditProfileIntent.EmailChanged -> {
                _uiState.update { it.copy(email = intent.email, newEmail = intent.email) }
            }
            EditProfileIntent.ShowEmailChangeDialogClicked -> _uiState.update { it.copy(showEmailChangeDialog = true, emailChangeStep = EmailChangeStep.Request) }
        }
    }

    private fun requestEmailChange() {
        viewModelScope.launch {
            val newEmail = _uiState.value.newEmail
            if (newEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                _sideEffects.emit(EditProfileSideEffect.ShowError("Please enter a valid email"))
                return@launch
            }

            _uiState.update { it.copy(isEmailUpdating = true) }
            when (val res = profileRepository.requestEmailChange(newEmail)) {
                is DataState.Success -> {
                    _uiState.update { it.copy(isEmailUpdating = false, emailChangeStep = EmailChangeStep.Verify) }
                    _sideEffects.emit(EditProfileSideEffect.ShowSnackbar("OTP sent to your new email"))
                }
                is DataState.Error -> {
                    handleError(res.exception)
                    _uiState.update { it.copy(isEmailUpdating = false) }
                }
                else -> Unit
            }
        }
    }

    private fun verifyEmailChange(otp: String) {
        viewModelScope.launch {
            if (otp.length != 6) {
                _sideEffects.emit(EditProfileSideEffect.ShowError("OTP must be 6 digits"))
                return@launch
            }

            _uiState.update { it.copy(isEmailUpdating = true) }
            when (val res = profileRepository.verifyEmailChange(_uiState.value.newEmail, otp)) {
                is DataState.Success -> {
                    _uiState.update { 
                        it.copy(
                            isEmailUpdating = false, 
                            showEmailChangeDialog = false,
                            email = _uiState.value.newEmail,
                            newEmail = ""
                        ) 
                    }
                    _sideEffects.emit(EditProfileSideEffect.ShowSnackbar("Email updated successfully"))
                    _sideEffects.emit(EditProfileSideEffect.EmailChanged)
                }
                is DataState.Error -> {
                    handleError(res.exception)
                    _uiState.update { it.copy(isEmailUpdating = false) }
                }
                else -> Unit
            }
        }
    }

    private fun updateNewEmail(email: String) {
        _uiState.update { it.copy(newEmail = email) }
    }

    private fun dismissEmailChangeDialog() {
        _uiState.update { it.copy(showEmailChangeDialog = false, newEmail = "", isEmailUpdating = false) }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val res = profileRepository.refreshProfile()) {
                is DataState.Error -> {
                    handleError(res.exception)
                    _uiState.update { it.copy(isLoading = false) }
                }
                else -> Unit
            }
        }
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileRepository.observeProfile().collect { state ->
                when (state) {
                    DataState.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is DataState.Error -> {
                        handleError(state.exception)
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is DataState.Success -> {
                        val profile = state.data
                        if (profile == null) {
                            _uiState.update { it.copy(isLoading = false) }
                            return@collect
                        }
                        _uiState.update {
                            it.copy(
                                originalProfile = profile,
                                imageUrl = profile.imageUrl?: "",
                                name = profile.name,
                                email = profile.email,
                                isLoading = false,
                                error = null,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun updateImageUrl(url: String) {
        _uiState.update { it.copy(imageUrl = url) }
    }

    private fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    private fun pickFromGallery() {
        viewModelScope.launch {
            _sideEffects.emit(EditProfileSideEffect.ShowImagePicker)
        }
    }

    private fun captureFromCamera() {
        viewModelScope.launch {
            _sideEffects.emit(EditProfileSideEffect.ShowCamera)
        }
    }

    private fun saveChanges() {
        viewModelScope.launch {
            val currentState = _uiState.value

            // Validation
            if (currentState.name.isBlank()) {
                _sideEffects.emit(EditProfileSideEffect.ShowError("Name cannot be empty"))
                return@launch
            }

            _uiState.update { it.copy(isSaving = true) }

            val isLocalPath = currentState.imageUrl.startsWith("/") || currentState.imageUrl.startsWith("file://")
            val localImagePath = if (isLocalPath) currentState.imageUrl.removePrefix("file://") else null

            val updatedProfile = currentState.originalProfile?.copy(
                imageUrl = if (isLocalPath) "" else currentState.imageUrl,
                name = currentState.name,
            )

            if (updatedProfile != null) {
                when (val res = profileRepository.updateProfile(updatedProfile, localImagePath)) {
                    is DataState.Success -> {
                        _uiState.update { it.copy(isSaving = false, originalProfile = updatedProfile) }
                        _sideEffects.emit(EditProfileSideEffect.ProfileSaved)
                        _sideEffects.emit(EditProfileSideEffect.ShowSnackbar("Profile updated successfully"))
                    }
                    is DataState.Error -> {
                        handleError(res.exception)
                        _uiState.update { it.copy(isSaving = false) }
                    }
                    DataState.Loading -> Unit
                }
            }
            
            // Clean up temp file
            localImagePath?.let { java.io.File(it).delete() }
        }
    }

    private fun resetToOriginal() {
        val original = _uiState.value.originalProfile
        if (original != null) {
            _uiState.update {
                it.copy(
                    imageUrl = original.imageUrl?: "",
                    name = original.name,
                    email = original.email,
                )
            }
        }
    }

    private fun dismissImagePicker() {
        _uiState.update { it.copy(showImagePickerDialog = false) }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun handleError(error: Throwable) {
        val message = error.message ?: "Something went wrong"
        _uiState.update { it.copy(error = message) }
        _sideEffects.emit(EditProfileSideEffect.ShowError(message))
    }

}

// MVI Contract

sealed interface EditProfileIntent {
    data object LoadProfile : EditProfileIntent
    data class ImageUrlChanged(val url: String) : EditProfileIntent
    data class NameChanged(val name: String) : EditProfileIntent
    data object PickFromGalleryClicked : EditProfileIntent
    data object CaptureFromCameraClicked : EditProfileIntent
    data object SaveChangesClicked : EditProfileIntent
    data object ResetClicked : EditProfileIntent
    data object BackPressed : EditProfileIntent
    data object DismissImagePicker : EditProfileIntent
    data object ErrorShown : EditProfileIntent
    data object ShowEmailChangeDialogClicked : EditProfileIntent
    data object RequestEmailChangeClicked : EditProfileIntent
    data class VerifyEmailChangeClicked(val otp: String) : EditProfileIntent
    data object DismissEmailChangeDialog : EditProfileIntent
    data class NewEmailChanged(val email: String) : EditProfileIntent
    data class EmailChanged(val email: String) : EditProfileIntent
}

enum class EmailChangeStep {
    Request, Verify
}

data class EditProfileState(
    val originalProfile: Profile? = null,
    val imageUrl: String = "",
    val name: String = "",
    val email: String = "",
    val newEmail: String = "",
    val otpCode: String = "",
    val showEmailChangeDialog: Boolean = false,
    val emailChangeStep: EmailChangeStep = EmailChangeStep.Request,
    val isEmailUpdating: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isLoadingImage: Boolean = false,
    val showImagePickerDialog: Boolean = false,
    val error: String? = null
)

sealed interface EditProfileSideEffect {
    data class ShowError(val message: String) : EditProfileSideEffect
    data class ShowSnackbar(val message: String) : EditProfileSideEffect
    data object ProfileSaved : EditProfileSideEffect
    data object BackPreseed : EditProfileSideEffect
    data object ShowImagePicker : EditProfileSideEffect
    data object ShowCamera : EditProfileSideEffect
    data object EmailChanged : EditProfileSideEffect
}
