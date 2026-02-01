package org.example.project.createPost.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.home.domain.models.CreatePost
import org.example.project.home.domain.models.MediaType
import org.example.project.home.domain.models.PostLevel
import org.example.project.home.domain.repository.PostRepository

class CreatePostViewModel(
    private val postRepository: PostRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostState())
    val uiState: StateFlow<CreatePostState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<CreatePostSideEffect>()
    val sideEffects: SharedFlow<CreatePostSideEffect> = _sideEffects

    fun onIntent(intent: CreatePostIntent) {
        when (intent) {
            CreatePostIntent.CloseClicked -> onNavigateBack()
            CreatePostIntent.CancelClicked -> onNavigateBack()

            is CreatePostIntent.DescriptionChanged -> {
                changeDescription(description = intent.description)
            }
            CreatePostIntent.AddPhotoClicked -> {
                handleAddPhoto()
            }
            CreatePostIntent.AddVideoClicked -> {
                handleAddVideo()
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
                // Show error
                _uiState.value = _uiState.value.copy(error = "Please describe the issue")

            }else {
                postRepository.createPost(
                    CreatePost(
                        userId = "",
                        userUrl = "",
                        userName = "",
                        postText = _uiState.value.description,
                        mediaType = MediaType.VIDEO,
                        mediaUrl = "",
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = true)
                // Simulate posting
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun handleAddPhoto() {
        viewModelScope.launch {
            _sideEffects.emit(CreatePostSideEffect.ShowPhotoPicker)
        }
    }

    private fun handleAddVideo() {
        viewModelScope.launch {
            _sideEffects.emit(CreatePostSideEffect.ShowVideoPicker)
        }
    }

    private fun onNavigateBack() {
        viewModelScope.launch {
            _sideEffects.emit(CreatePostSideEffect.NavigateBack)
        }
    }
}

sealed interface CreatePostIntent {
    data object CloseClicked : CreatePostIntent
    data class DescriptionChanged(val description: String) : CreatePostIntent
    data object AddPhotoClicked : CreatePostIntent
    data object AddVideoClicked : CreatePostIntent
    data object CancelClicked : CreatePostIntent
    data object PostIssueClicked : CreatePostIntent
}

// MVI - State
data class CreatePostState(
    val userName: String = "",
    val location: String = "",
    val selectedPostLevel: PostLevel = PostLevel.LOCALITY,
    val description: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showIssueScopeDropdown: Boolean = false,
    val imageUrl : String? = null,
    val videoUrl : String? = null
)

sealed interface CreatePostSideEffect {
    data object NavigateBack : CreatePostSideEffect
    data object ShowPhotoPicker : CreatePostSideEffect
    data object ShowVideoPicker : CreatePostSideEffect
    data class ShowError(val message: String) : CreatePostSideEffect
    data class PostCreated(val postId: String) : CreatePostSideEffect
}