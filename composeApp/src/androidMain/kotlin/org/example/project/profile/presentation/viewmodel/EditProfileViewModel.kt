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
import org.example.project.core.di.ImagePicker
import org.example.project.profile.domain.usecases.GetProfileUseCase
import org.example.project.profile.domain.usecases.UpdateProfileUseCase
import org.example.project.profile.domain.models.Profile
import kotlin.onSuccess

/**
 * ViewModel for Edit Profile Screen with MVI pattern
 */
class EditProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val imagePicker: ImagePicker
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileState())
    val uiState: StateFlow<EditProfileState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<EditProfileSideEffect>()
    val sideEffects: SharedFlow<EditProfileSideEffect> = _sideEffects.asSharedFlow()

    init {
        loadProfile()
    }

    fun onIntent(intent: EditProfileIntent) {
        when (intent) {
            EditProfileIntent.LoadProfile -> loadProfile()
            is EditProfileIntent.ImageUrlChanged -> updateImageUrl(intent.url)
            is EditProfileIntent.NameChanged -> updateName(intent.name)
            is EditProfileIntent.LocalityChanged -> updateLocality(intent.locality)
            is EditProfileIntent.DistrictChanged -> updateDistrict(intent.district)
            is EditProfileIntent.StateChanged -> updateState(intent.state)
            is EditProfileIntent.CountryChanged -> updateCountry(intent.country)
            EditProfileIntent.PickFromGalleryClicked -> pickFromGallery()
            EditProfileIntent.CaptureFromCameraClicked -> captureFromCamera()
            EditProfileIntent.SaveChangesClicked -> saveChanges()
            EditProfileIntent.ResetClicked -> resetToOriginal()
            EditProfileIntent.DismissImagePicker -> dismissImagePicker()
            EditProfileIntent.BackPressed -> viewModelScope.launch { _sideEffects.emit(EditProfileSideEffect.BackPreseed) }
            EditProfileIntent.ErrorShown -> clearError()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getProfileUseCase()
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(
                            originalProfile = profile,
                            imageUrl = profile.imageUrl,
                            name = profile.name,
                            locality = profile.locality,
                            district = profile.district,
                            state = profile.state,
                            country = profile.country,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    handleError(error)
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun updateImageUrl(url: String) {
        _uiState.update { it.copy(imageUrl = url) }
    }

    private fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    private fun updateLocality(locality: String) {
        _uiState.update { it.copy(locality = locality) }
    }

    private fun updateDistrict(district: String) {
        _uiState.update { it.copy(district = district) }
    }

    private fun updateState(state: String) {
        _uiState.update { it.copy(state = state) }
    }

    private fun updateCountry(country: String) {
        _uiState.update { it.copy(country = country) }
    }

    private fun pickFromGallery() {
        viewModelScope.launch {
            _uiState.update { it.copy(showImagePickerDialog = false, isLoadingImage = true) }

            // Check and request permission
//            if (!imagePicker.hasGalleryPermission()) {
//                val granted = imagePicker.requestGalleryPermission()
//                if (!granted) {
//                    _sideEffects.emit(EditProfileSideEffect.ShowSnackbar("Gallery permission denied"))
//                    _uiState.update { it.copy(isLoadingImage = false) }
//                    return@launch
//                }
//            }

//            val imageUri = imagePicker.pickImageFromGallery()
//            if (imageUri != null) {
//                _uiState.update { it.copy(imageUrl = imageUri, isLoadingImage = false) }
//            } else {
//                _uiState.update { it.copy(isLoadingImage = false) }
//            }
        }
    }

    private fun captureFromCamera() {
        viewModelScope.launch {
            _uiState.update { it.copy(showImagePickerDialog = false, isLoadingImage = true) }

            // Check and request permission
//            if (!imagePicker.hasCameraPermission()) {
//                val granted = imagePicker.requestCameraPermission()
//                if (!granted) {
//                    _sideEffects.emit(EditProfileSideEffect.ShowSnackbar("Camera permission denied"))
//                    _uiState.update { it.copy(isLoadingImage = false) }
//                    return@launch
//                }
//            }
//
//            val imageUri = imagePicker.captureImageFromCamera()
//            if (imageUri != null) {
//                _uiState.update { it.copy(imageUrl = imageUri, isLoadingImage = false) }
//            } else {
//                _uiState.update { it.copy(isLoadingImage = false) }
//            }
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
                locality = currentState.locality,
                district = currentState.district,
                state = currentState.state,
                country = currentState.country,
                location = buildLocationString(
                    currentState.locality,
                    currentState.district,
                    currentState.state,
                    currentState.country
                )
            )

            if (updatedProfile != null) {
                updateProfileUseCase(updatedProfile)
                    .onSuccess { profile ->
                        _uiState.update { it.copy(isSaving = false, originalProfile = profile) }
                        _sideEffects.emit(EditProfileSideEffect.ProfileSaved)
                        _sideEffects.emit(EditProfileSideEffect.ShowSnackbar("Profile updated successfully"))
                    }
                    .onFailure { error ->
                        handleError(error)
                        _uiState.update { it.copy(isSaving = false) }
                    }
            }
        }
    }

    private fun resetToOriginal() {
        val original = _uiState.value.originalProfile
        if (original != null) {
            _uiState.update {
                it.copy(
                    imageUrl = original.imageUrl,
                    name = original.name,
                    locality = original.locality,
                    district = original.district,
                    state = original.state,
                    country = original.country
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

    private fun buildLocationString(locality: String, district: String, state: String, country: String): String {
        return listOf(locality, district, state, country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    }
}

// MVI Contract

sealed interface EditProfileIntent {
    data object LoadProfile : EditProfileIntent
    data class ImageUrlChanged(val url: String) : EditProfileIntent
    data class NameChanged(val name: String) : EditProfileIntent
    data class LocalityChanged(val locality: String) : EditProfileIntent
    data class DistrictChanged(val district: String) : EditProfileIntent
    data class StateChanged(val state: String) : EditProfileIntent
    data class CountryChanged(val country: String) : EditProfileIntent
    data object PickFromGalleryClicked : EditProfileIntent
    data object CaptureFromCameraClicked : EditProfileIntent
    data object SaveChangesClicked : EditProfileIntent
    data object ResetClicked : EditProfileIntent
    data object BackPressed : EditProfileIntent
    data object DismissImagePicker : EditProfileIntent
    data object ErrorShown : EditProfileIntent
}

data class EditProfileState(
    val originalProfile: Profile? = null,
    val imageUrl: String = "",
    val name: String = "",
    val locality: String = "",
    val district: String = "",
    val state: String = "",
    val country: String = "",
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
}

