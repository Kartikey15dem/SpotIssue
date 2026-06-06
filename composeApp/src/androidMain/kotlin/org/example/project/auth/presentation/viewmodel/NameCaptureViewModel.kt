package org.example.project.auth.presentation.viewmodel

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
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.model.profile.Profile
import org.example.project.core.utils.DataState
import android.net.Uri
import android.content.Context
import org.example.project.utils.AndroidImagePicker
import org.example.project.utils.media.MediaCompressorUtil

sealed class NameCaptureEffect {
    data class ShowSnackbar(val message: String) : NameCaptureEffect()
    data object ShowImagePicker : NameCaptureEffect()
    data class ShowCamera(val uri: Uri) : NameCaptureEffect()
}

data class NameCaptureUiState(
    val name: String = "",
    val imageUrl: String = "",
    val isLoading: Boolean = false,
    val isLoadingImage: Boolean = false,
    val pendingCameraUri: Uri? = null
)

sealed class NameCaptureIntent {
    data class NameChanged(val name: String) : NameCaptureIntent()
    data class ImageUrlChanged(val url: String) : NameCaptureIntent()
    data object SubmitClicked : NameCaptureIntent()
    data object PickFromGalleryClicked : NameCaptureIntent()
    data object CaptureFromCameraClicked : NameCaptureIntent()
    data class CameraImageCaptured(val success: Boolean) : NameCaptureIntent()
}

class NameCaptureViewModel(
    private val context: Context,
    private val email: String,
    private val prefRepository: UserPreferencesRepository,
    private val profileRepository: ProfileRepository,
    private val imagePicker: AndroidImagePicker
) : ViewModel() {

    private val _uiState = MutableStateFlow(NameCaptureUiState())
    val uiState: StateFlow<NameCaptureUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<NameCaptureEffect>()
    val effect: SharedFlow<NameCaptureEffect> = _effect.asSharedFlow()

    fun handleIntent(intent: NameCaptureIntent) {
        when (intent) {
            is NameCaptureIntent.NameChanged -> updateName(intent.name)
            is NameCaptureIntent.ImageUrlChanged -> updateImageUrl(intent.url)
            is NameCaptureIntent.SubmitClicked -> submitProfile()
            NameCaptureIntent.PickFromGalleryClicked -> pickFromGallery()
            NameCaptureIntent.CaptureFromCameraClicked -> captureFromCamera()
            is NameCaptureIntent.CameraImageCaptured -> {
                if (intent.success) {
                    _uiState.value.pendingCameraUri?.let { uri ->
                        updateImageUrl(uri.toString())
                    }
                }
                _uiState.update { it.copy(pendingCameraUri = null) }
            }
        }
    }

    private fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    private fun updateImageUrl(url: String) {
        _uiState.update { it.copy(imageUrl = url) }
    }

    private fun pickFromGallery() {
        viewModelScope.launch {
            _effect.emit(NameCaptureEffect.ShowImagePicker)
        }
    }

    private fun captureFromCamera() {
        viewModelScope.launch {
            val uri = imagePicker.createImageUri()
            _uiState.update { it.copy(pendingCameraUri = uri) }
            _effect.emit(NameCaptureEffect.ShowCamera(uri))
        }
    }

    private fun submitProfile() {
        val currentName = _uiState.value.name.trim()
        val currentImageUrl = _uiState.value.imageUrl

        if (currentName.isEmpty()) {
            showError("Please enter your name")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            var localImagePath: String? = null
            if (currentImageUrl.startsWith("content://") || currentImageUrl.startsWith("file://")) {
                val compressedFile = MediaCompressorUtil.compressImage(context, currentImageUrl)
                localImagePath = compressedFile?.absolutePath
            }

            val profile = Profile(
                name = currentName,
                email = email.trim(),
                imageUrl = if (localImagePath != null) "" else currentImageUrl,
                totalPosts = 0,
                acks = 0,
                postByArea = listOf(0, 0, 0, 0),
                myPosts = emptyList(),
                ackPosts = emptyList()
            )

            when (val result = profileRepository.updateProfile(profile, localImagePath)) {
                is DataState.Success -> {
                    prefRepository.setLoggedIn(true)
                }
                is DataState.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    showError(result.exception.message ?: "Failed to update profile")
                }
                DataState.Loading -> {}
            }

            // Clean up temp file
            localImagePath?.let { java.io.File(it).delete() }
        }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _effect.emit(NameCaptureEffect.ShowSnackbar(message))
        }
    }
}
