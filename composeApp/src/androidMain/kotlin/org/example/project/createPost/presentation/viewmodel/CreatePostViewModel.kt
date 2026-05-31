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
import org.example.project.core.datastore.model.UploadStatus
import org.example.project.core.datastore.model.UploadDraftState


data class SelectedMediaItem(
    val uri: String,
    val type: MediaType
)

class CreatePostViewModel(
    private val postRepository: PostRepository,
    private val prefRepository: UserPreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostState())
    val uiState: StateFlow<CreatePostState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<CreatePostSideEffect>()
    val sideEffects: SharedFlow<CreatePostSideEffect> = _sideEffects

        init {
        viewModelScope.launch {
            prefRepository.userData.collect { userData ->
                val draft = userData.uploadDraftState
                if (draft.status == UploadStatus.ERROR) {
                    _uiState.value = _uiState.value.copy(
                        description = draft.postText,
                        // Could also restore media here if needed
                    )
                    _sideEffects.emit(CreatePostSideEffect.ShowError(draft.errorMessage ?: "Upload failed. Draft restored."))
                    // Reset status to IDLE so we don't keep showing the error
                    prefRepository.updateUploadDraftState(draft.copy(status = UploadStatus.IDLE))
                } else if (draft.status == UploadStatus.SUCCESS) {
                    _sideEffects.emit(CreatePostSideEffect.PostCreated("new_post"))
                    prefRepository.updateUploadDraftState(UploadDraftState.DEFAULT)
                    // Wait, if they are still on this screen, close it
                    onNavigateBack()
                } else if (draft.status == UploadStatus.UPLOADING) {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    fun onIntent(intent: CreatePostIntent) {
        when (intent) {
            CreatePostIntent.CloseClicked -> onNavigateBack()
            CreatePostIntent.CancelClicked -> onNavigateBack()
            is CreatePostIntent.DescriptionChanged -> changeDescription(intent.description)
            CreatePostIntent.AddMediaClicked -> handleAddMedia()
            CreatePostIntent.AddPdfClicked -> handleAddPdf()
            CreatePostIntent.RemoveMedia -> removeMedia()
            is CreatePostIntent.RemoveImage -> removeImage(intent.uri)
            CreatePostIntent.PostIssueClicked -> createPost()
        }
    }

    private fun changeDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

        private fun createPost() {
        viewModelScope.launch {
            if (_uiState.value.description.isBlank()) {
                _sideEffects.emit(CreatePostSideEffect.ShowError("Please describe the issue"))
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val mediaUris = _uiState.value.selectedMedia?.map { it.uri } ?: emptyList()
            val mediaType = _uiState.value.selectedMedia?.firstOrNull()?.type?.name ?: "IMAGE"

            // 1. Save state to DataStore
            val draft = UploadDraftState(
                status = UploadStatus.UPLOADING,
                postText = _uiState.value.description,
                postLevel = "LOCALITY",
                mediaUris = mediaUris,
                mediaType = mediaType
            )
            prefRepository.updateUploadDraftState(draft)

            // 2. Tell UI to enqueue WorkManager
            _sideEffects.emit(CreatePostSideEffect.StartBackgroundUpload)
            
            // Note: We don't navigate back immediately, we let the UI show the progress 
            // overlay if the user stays, or they can background the app.
        }
    }

    private fun handleAddMedia() {
        viewModelScope.launch { _sideEffects.emit(CreatePostSideEffect.ShowMediaPicker) }
    }

    private fun handleAddPdf() {
        viewModelScope.launch { _sideEffects.emit(CreatePostSideEffect.ShowPdfPicker) }
    }

    private fun onNavigateBack() {
        viewModelScope.launch { _sideEffects.emit(CreatePostSideEffect.NavigateBack) }
    }

    fun setVisualMedia(mediaItems: List<Pair<String, String?>>) {
        val maxItems = 10

        val processedItems = mediaItems.mapNotNull { (uri, mimeType) ->
            if (!validateMedia(mimeType)) return@mapNotNull null
            val type = when {
                mimeType?.startsWith("image/") == true -> MediaType.IMAGE
                mimeType?.startsWith("video/") == true -> MediaType.VIDEO
                else -> null
            }
            if (type != null) SelectedMediaItem(uri, type) else null
        }

        val videosCount = processedItems.count { it.type == MediaType.VIDEO }
        val imagesCount = processedItems.count { it.type == MediaType.IMAGE }

        when {
            videosCount > 1 -> {
                emitError("You can only select 1 video per post.")
            }
            videosCount == 1 && imagesCount > 0 -> {
                emitError("You cannot mix images and video. Please select one or the other.")
            }
            processedItems.size > maxItems -> {
                emitError("You can only select up to $maxItems items.")
            }
            processedItems.isNotEmpty() -> {
                // Rules passed! Replace the existing selection with the new one
                _uiState.value = _uiState.value.copy(selectedMedia = processedItems)
            }
        }
    }

    fun setDocumentUrl(uri: String) {
        if (!validateMedia("application/pdf")) {
            emitError("PDF format is not supported")
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedMedia = listOf(SelectedMediaItem(uri, MediaType.PDF))
        )
    }

    private fun emitError(message: String) {
        viewModelScope.launch {
            _sideEffects.emit(CreatePostSideEffect.ShowError(message))
        }
    }

    fun validateMedia(mimeType: String?): Boolean {
        val supportedTypes = listOf(
            "image/jpeg", "image/png", "image/webp",
            "video/mp4", "video/mpeg", "video/quicktime",
            "application/pdf"
        )
        return mimeType != null && supportedTypes.any { mimeType.startsWith(it) }
    }

    private fun removeMedia(){
        _uiState.value = _uiState.value.copy(selectedMedia = null)
    }
    private fun removeImage(uriToRemove: String) {
        val updatedList = _uiState.value.selectedMedia?.filterNot { it.uri == uriToRemove }
        _uiState.value = _uiState.value.copy(selectedMedia = updatedList)
    }
}

sealed interface CreatePostIntent {
    data object CloseClicked : CreatePostIntent
    data class DescriptionChanged(val description: String) : CreatePostIntent
    data object AddMediaClicked : CreatePostIntent
    data object AddPdfClicked : CreatePostIntent
    data object RemoveMedia : CreatePostIntent
    data class RemoveImage(val uri: String) : CreatePostIntent
    data object CancelClicked : CreatePostIntent
    data object PostIssueClicked : CreatePostIntent
}

data class CreatePostState(
    val userName: String = "Current User",
    val location: String = "Current Location",
    val description: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showIssueScopeDropdown: Boolean = false,
    val selectedMedia: List<SelectedMediaItem>? = null
)

sealed interface CreatePostSideEffect {
    data object NavigateBack : CreatePostSideEffect
    data object ShowMediaPicker : CreatePostSideEffect
    data object ShowPdfPicker : CreatePostSideEffect
    data object StartBackgroundUpload : CreatePostSideEffect
    data class ShowError(val message: String) : CreatePostSideEffect
    data class PostCreated(val postId: String) : CreatePostSideEffect
}