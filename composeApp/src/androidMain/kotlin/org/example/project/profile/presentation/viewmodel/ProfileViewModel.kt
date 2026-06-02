package org.example.project.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.data.mappers.Sort
import org.example.project.core.model.home.Post
import org.example.project.core.model.profile.Profile
import org.example.project.core.utils.DataState

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<ProfileSideEffect>()
    val sideEffects: SharedFlow<ProfileSideEffect> = _sideEffects.asSharedFlow()

    init {
        observeProfile()
        initPostFlows()
        fetchProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileRepository.observeProfile().collect { dataState ->
                when (dataState) {
                    is DataState.Loading -> updateState { it.copy(isProfileLoading = true) }
                    is DataState.Success -> updateState { it.copy(
                        isProfileLoading = false,
                        profile = dataState.data
                    ) }
                    is DataState.Error -> {
                        updateState { it.copy(isProfileLoading = false) }
                        handleError(dataState.exception)
                    }
                }
            }
        }
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            updateState { it.copy(isProfileLoading = true, profileError = null) }
            val result = profileRepository.refreshProfile()
            if (result is DataState.Error) {
                updateState { it.copy(isProfileLoading = false, profileError = result.exception.message ?: "Failed to load profile") }
                handleError(result.exception)
            }
        }
    }

    private fun initPostFlows() {
        val userFlow = profileRepository.getPagedUserPosts().cachedIn(viewModelScope)
        val likedFlow = profileRepository.getPagedLikedPosts().cachedIn(viewModelScope)
        updateState { it.copy(
            userPostsFlow = userFlow,
            likedPostsFlow = likedFlow
        ) }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.CreatePostClicked -> navigateToCreatePost()
            ProfileIntent.EditProfileClicked -> navigateToEditProfile()
            is ProfileIntent.TabChanged -> changeTab(intent.isMine)
            is ProfileIntent.SortChanged -> changeSort(intent.sort)
            is ProfileIntent.DeletePostClicked -> deletePost(intent.postId)
            is ProfileIntent.LikeClicked -> likePost(intent.postId)
            is ProfileIntent.CommentClicked -> navigateToPost(intent.postId)
            is ProfileIntent.ShareClicked -> sharePost(intent.postId)
            is ProfileIntent.ReportClicked -> reportPost(intent.postId)
            ProfileIntent.ErrorShown -> clearError()
            ProfileIntent.RetryProfileClicked -> fetchProfile()
        }
    }

    private fun updateState(update: (ProfileState) -> ProfileState) {
        _uiState.update(update)
    }

    private fun changeTab(isMine: Boolean) {
        updateState { it.copy(isMine = isMine) }
    }

    private fun changeSort(sort: Sort) {
        updateState { it.copy(sort = sort) }
    }

    private fun deletePost(postId: String) {
        viewModelScope.launch {
            when (val result = postRepository.deletePost(postId)) {
                is DataState.Success -> {
                    _sideEffects.emit(ProfileSideEffect.ShowSnackbar("Post deleted successfully"))
                }
                is DataState.Error -> {
                    handleError(result.exception)
                }
                else -> Unit
            }
        }
    }

    private fun likePost(postId: String) {
        viewModelScope.launch {
            when (val result = postRepository.likePost(postId)) {
                is DataState.Error -> handleError(result.exception)
                else -> Unit
            }
        }
    }

    private fun sharePost(postId: String) {
        viewModelScope.launch {
            _sideEffects.emit(ProfileSideEffect.SharePost("Shared post $postId from IssueSpot"))
        }
    }

    private fun reportPost(postId: String) {
        viewModelScope.launch {
            when (val result = postRepository.reportPost(postId, null)) {
                is DataState.Success -> {
                    _sideEffects.emit(ProfileSideEffect.ShowSnackbar("Post reported successfully"))
                }
                is DataState.Error -> {
                    handleError(result.exception)
                }
                else -> Unit
            }
        }
    }

    private fun navigateToCreatePost() {
        viewModelScope.launch {
            _sideEffects.emit(ProfileSideEffect.NavigateToCreatePost)
        }
    }

    private fun navigateToEditProfile() {
        viewModelScope.launch {
            _sideEffects.emit(ProfileSideEffect.NavigateToEditProfile)
        }
    }

    private fun navigateToPost(postId: String) {
        viewModelScope.launch {
            _sideEffects.emit(ProfileSideEffect.NavigateToPost(postId))
        }
    }

    private fun clearError() {
        updateState { it.copy(error = null) }
    }

    private suspend fun handleError(error: Throwable) {
        val message = error.message ?: "Something went wrong"
        updateState { it.copy(error = message) }
        _sideEffects.emit(ProfileSideEffect.ShowError(message))
    }
}

sealed interface ProfileIntent {
    data object CreatePostClicked : ProfileIntent
    data object EditProfileClicked : ProfileIntent
    data class TabChanged(val isMine: Boolean) : ProfileIntent
    data class SortChanged(val sort: Sort) : ProfileIntent
    data class DeletePostClicked(val postId: String) : ProfileIntent
    data class LikeClicked(val postId: String) : ProfileIntent
    data class CommentClicked(val postId: String) : ProfileIntent
    data class ShareClicked(val postId: String) : ProfileIntent
    data class ReportClicked(val postId: String) : ProfileIntent
    data object ErrorShown : ProfileIntent
    data object RetryProfileClicked : ProfileIntent
}

data class ProfileState(
    val profile: Profile? = null,
    val isProfileLoading: Boolean = true,
    val userPostsFlow: Flow<PagingData<Post>>? = null,
    val likedPostsFlow: Flow<PagingData<Post>>? = null,
    val isMine: Boolean = true,
    val sort: Sort = Sort.LATEST,
    val isLoading: Boolean = false,
    val error: String? = null,
    val profileError: String? = null
) {
    val activePostsFlow: Flow<PagingData<Post>>? 
        get() = if (isMine) userPostsFlow else likedPostsFlow
}

sealed interface ProfileSideEffect {
    data object NavigateToCreatePost : ProfileSideEffect
    data object NavigateToEditProfile : ProfileSideEffect
    data class NavigateToPost(val postId: String) : ProfileSideEffect
    data class ShowError(val message: String) : ProfileSideEffect
    data class ShowSnackbar(val message: String) : ProfileSideEffect
    data class SharePost(val text: String) : ProfileSideEffect
    data class OpenMediaViewer(val postId: String) : ProfileSideEffect
}
