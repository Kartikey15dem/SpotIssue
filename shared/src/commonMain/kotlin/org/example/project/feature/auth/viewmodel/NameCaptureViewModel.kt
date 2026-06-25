package org.example.project.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.model.profile.Profile
import org.example.project.core.utils.DataState
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.example.project.core.utils.FileSystem

sealed class NameCaptureEffect {
    data class ShowSnackbar(val message: String) : NameCaptureEffect()
    data object ShowImagePicker : NameCaptureEffect()
    data object ShowCamera : NameCaptureEffect()
}

data class NameCaptureUiState(
    val name: String = "",
    val imageUrl: String = "",
    val isLoading: Boolean = false,
    val isLoadingImage: Boolean = false
)

sealed class NameCaptureIntent {
    data class NameChanged(val name: String) : NameCaptureIntent()
    data class ImageUrlChanged(val url: String) : NameCaptureIntent()
    data object SubmitClicked : NameCaptureIntent()
    data object PickFromGalleryClicked : NameCaptureIntent()
    data object CaptureFromCameraClicked : NameCaptureIntent()
}

class NameCaptureViewModel(
    private val email: String,
    private val prefRepository: UserPreferencesRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NameCaptureUiState())
    val uiState: StateFlow<NameCaptureUiState> = _uiState.asStateFlow()

    private val _effect = Channel<NameCaptureEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun handleIntent(intent: NameCaptureIntent) {
        when (intent) {
            is NameCaptureIntent.NameChanged -> updateName(intent.name)
            is NameCaptureIntent.ImageUrlChanged -> updateImageUrl(intent.url)
            is NameCaptureIntent.SubmitClicked -> submitProfile()
            NameCaptureIntent.PickFromGalleryClicked -> pickFromGallery()
            NameCaptureIntent.CaptureFromCameraClicked -> captureFromCamera()
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
            _effect.send(NameCaptureEffect.ShowImagePicker)
        }
    }

    private fun captureFromCamera() {
        viewModelScope.launch {
            _effect.send(NameCaptureEffect.ShowCamera)
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

            val isLocalPath = currentImageUrl.startsWith("/") || currentImageUrl.startsWith("file://")
            val localImagePath = if (isLocalPath) currentImageUrl.removePrefix("file://") else null
            
            val profile = Profile(
                name = currentName,
                email = email.trim(),
                imageUrl = if (isLocalPath) "" else currentImageUrl,
                totalPosts = 0,
                acks = 0,
                postByArea = listOf(0, 0, 0, 0),
                myPosts = emptyList(),
                ackPosts = emptyList()
            )

            when (val result = profileRepository.updateProfile(profile, localImagePath)) {
                is DataState.Success -> {
                    localImagePath?.let { path ->
                        withContext(Dispatchers.IO) {
                            FileSystem.deleteFile(path)
                        }
                    }
                    prefRepository.setLoggedIn(true)
                }
                is DataState.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    showError(result.exception.message ?: "Failed to update profile")
                }
                DataState.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }


        }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _effect.send(NameCaptureEffect.ShowSnackbar(message))
        }
    }
}
