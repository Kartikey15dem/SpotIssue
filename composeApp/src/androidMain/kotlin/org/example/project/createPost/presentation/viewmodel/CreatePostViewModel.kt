package org.example.project.createPost.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.core.model.createPost.CreatePost
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.model.home.MediaType
import org.example.project.core.utils.DataState
import org.example.project.core.model.home.PostLevel
import org.example.project.core.datastore.UserPreferencesRepository

class CreatePostViewModel(
    private val postRepository: PostRepository,
    private val profileRepository : ProfileRepository,
    private val prefRepository: UserPreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostState())
    val uiState: StateFlow<CreatePostState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<CreatePostSideEffect>()
    val sideEffects: SharedFlow<CreatePostSideEffect> = _sideEffects

    // Profile can be observed later when we need to prefill author fields.

    fun onIntent(intent: CreatePostIntent) {
        when (intent) {
            CreatePostIntent.CloseClicked -> onNavigateBack()
            CreatePostIntent.CancelClicked -> onNavigateBack()

            is CreatePostIntent.DescriptionChanged -> {
                changeDescription(description = intent.description)
            }
            CreatePostIntent.AddMediaClicked -> {
                handleAddMedia()
            }
            CreatePostIntent.AddPdfClicked -> {
                handleAddPdf()
            }
            CreatePostIntent.RemoveMedia -> {
                removeMedia()
            }
            CreatePostIntent.PostIssueClicked -> {
                createPost()
            }
        }
    }

    private fun changeDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    private fun createPost(){
        viewModelScope.launch {
            if (_uiState.value.description.isBlank()) {
                _sideEffects.emit(CreatePostSideEffect.ShowError("Please describe the issue"))
            } else {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val userLocation = prefRepository.userData.value.userLocation
                                when(val result = postRepository.createPost(
                    CreatePost(
                        userId = "",
                        userUrl = "",
                        userName = "",
                        postLevel = PostLevel.LOCALITY,
                        postText = _uiState.value.description,
                        mediaType = _uiState.value.selectedMediaType ?: MediaType.IMAGE,
                        mediaUrl = _uiState.value.selectedMediaUri,
                        location = userLocation
                    )
                )) {
                    is DataState.Success -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _sideEffects.emit(CreatePostSideEffect.PostCreated("new_post"))
                        onNavigateBack()
                    }
                    is DataState.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _sideEffects.emit(CreatePostSideEffect.ShowError(result.exception.message ?: "Failed to create post"))
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun handleAddMedia() {
        viewModelScope.launch {
            _sideEffects.emit(CreatePostSideEffect.ShowMediaPicker)
        }
    }

    private fun handleAddPdf() {
        viewModelScope.launch {
            _sideEffects.emit(CreatePostSideEffect.ShowPdfPicker)
        }
    }

    private fun onNavigateBack() {
        viewModelScope.launch {
            _sideEffects.emit(CreatePostSideEffect.NavigateBack)
        }
    }

    // --- Set visual media with mime type detection ---
    fun setVisualMedia(uri: String, mimeType: String?) {
        // Validate media first
        if (!validateMedia(mimeType)) {
            viewModelScope.launch {
                _sideEffects.emit(CreatePostSideEffect.ShowError("Unsupported media format"))
            }
            return
        }

        val type = when {
            mimeType?.startsWith("image/") == true -> MediaType.IMAGE
            mimeType?.startsWith("video/") == true -> MediaType.VIDEO
            else -> null
        }
        _uiState.value = _uiState.value.copy(selectedMediaUri = uri, selectedMediaType = type)
    }

    // --- Set PDF document ---
    fun setDocumentUrl(uri: String) {
        // Validate PDF format
        if (!validateMedia("application/pdf")) {
            viewModelScope.launch {
                _sideEffects.emit(CreatePostSideEffect.ShowError("PDF format is not supported"))
            }
            return
        }
        _uiState.value = _uiState.value.copy(selectedMediaUri = uri, selectedMediaType = MediaType.PDF)
    }

    // --- Helper for Validation ---
    fun validateMedia(mimeType: String?): Boolean {
        // Production supported types
        val supportedTypes = listOf(
            "image/jpeg", "image/png", "image/webp",
            "video/mp4", "video/mpeg", "video/quicktime",
            "application/pdf"
        )
        return mimeType != null && supportedTypes.any { mimeType.startsWith(it) }
    }

    // --- Legacy methods for backward compatibility ---
    @Deprecated("Use setVisualMedia instead")
    fun setImageUrl(imageUrl: String) {
        _uiState.value = _uiState.value.copy(
            selectedMediaUri = imageUrl,
            selectedMediaType = MediaType.IMAGE
        )
    }

    @Deprecated("Use setVisualMedia instead")
    fun setVideoUrl(videoUrl: String) {
        _uiState.value = _uiState.value.copy(
            selectedMediaUri = videoUrl,
            selectedMediaType = MediaType.VIDEO
        )
    }

    private fun removeMedia(){
        _uiState.value = _uiState.value.copy(
            selectedMediaUri = null,
            selectedMediaType = null
        )
    }
}

sealed interface CreatePostIntent {
    data object CloseClicked : CreatePostIntent
    data class DescriptionChanged(val description: String) : CreatePostIntent
    data object AddMediaClicked : CreatePostIntent
    data object AddPdfClicked : CreatePostIntent
    data object RemoveMedia : CreatePostIntent
    data object CancelClicked : CreatePostIntent
    data object PostIssueClicked : CreatePostIntent
}

// MVI - State
data class CreatePostState(
    val userName: String = "Current User", // TODO: Get from auth
    val location: String = "Current Location", // TODO: Get from location service
    val description: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showIssueScopeDropdown: Boolean = false,
    val selectedMediaUri: String? = "",
    val selectedMediaType: MediaType? = MediaType.IMAGE
)

sealed interface CreatePostSideEffect {
    data object NavigateBack : CreatePostSideEffect
    data object ShowMediaPicker : CreatePostSideEffect
    data object ShowPdfPicker : CreatePostSideEffect
    data class ShowError(val message: String) : CreatePostSideEffect
    data class PostCreated(val postId: String) : CreatePostSideEffect
}
