package org.example.project.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.model.profile.Profile
import org.example.project.core.utils.DataState
import org.example.project.utils.AndroidVideoPicker

import android.net.Uri
import org.example.project.utils.AndroidImagePicker

class EditProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val imagePicker: AndroidImagePicker
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
            is EditProfileIntent.CameraImageCaptured -> {
                if (intent.success) {
                    _uiState.value.pendingCameraUri?.let { uri ->
                        updateImageUrl(uri.toString())
                    }
                }
                _uiState.update { it.copy(pendingCameraUri = null) }
            }
        }
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
            val uri = imagePicker.createImageUri()
            _uiState.update { it.copy(pendingCameraUri = uri) }
            _sideEffects.emit(EditProfileSideEffect.ShowCamera(uri))
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

            val updatedProfile = currentState.originalProfile?.copy(
                imageUrl = currentState.imageUrl,
                name = currentState.name,

            )

            if (updatedProfile != null) {
                when (val res = profileRepository.updateProfile(updatedProfile)) {
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
        }
    }

    private fun resetToOriginal() {
        val original = _uiState.value.originalProfile
        if (original != null) {
            _uiState.update {
                it.copy(
                    imageUrl = original.imageUrl?: "",
                    name = original.name,
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
    data class CameraImageCaptured(val success: Boolean) : EditProfileIntent
}

data class EditProfileState(
    val originalProfile: Profile? = null,
    val imageUrl: String = "",
    val name: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isLoadingImage: Boolean = false,
    val showImagePickerDialog: Boolean = false,
    val error: String? = null,
    val pendingCameraUri: Uri? = null
)

sealed interface EditProfileSideEffect {
    data class ShowError(val message: String) : EditProfileSideEffect
    data class ShowSnackbar(val message: String) : EditProfileSideEffect
    data object ProfileSaved : EditProfileSideEffect
    data object BackPreseed : EditProfileSideEffect
    data object ShowImagePicker : EditProfileSideEffect
    data class ShowCamera(val uri: Uri) : EditProfileSideEffect
}
