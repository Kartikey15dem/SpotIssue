package org.example.project.home.presentation.viewmodel

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.data.repository.FeedRepository
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.utils.DataState
import org.example.project.home.presentation.CurrentLevelManager


class HomeViewModel(
    private val feedRepository: FeedRepository,
    private val postRepository: PostRepository,
    private val currentLevelManager: CurrentLevelManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<HomeSideEffect>()
    val sideEffects: SharedFlow<HomeSideEffect> = _sideEffects.asSharedFlow()

    init {
        viewModelScope.launch {
            currentLevelManager.currentLevel.collect { level ->
                updateState { it.copy(
                    postLevel = level,
                    postsFlow = feedRepository.getPagedPosts(level).cachedIn(viewModelScope)
                ) }
                observeActiveIssues(level)
            }
        }
    }

    private fun observeActiveIssues(level: PostLevel) {
        viewModelScope.launch {
            feedRepository.observeActiveIssuesCount(level).collect { dataState ->
                updateState { it.copy(activeIssues = dataState) }
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Refresh -> refresh()
            is HomeIntent.SearchQueryChanged -> updateSearchQuery(intent.query)
            HomeIntent.CreatePostClicked -> navigateToCreatePost()
            HomeIntent.ProfileClicked -> navigateToProfile()
            is HomeIntent.ReportClicked -> report(intent.postId, intent.reason)
            is HomeIntent.LikeClicked -> like(intent.postId)
            is HomeIntent.CommentClicked -> comment(intent.postId, intent.comment)
            is HomeIntent.ShareClicked -> share(intent.post)
            HomeIntent.ErrorShown -> clearError()
        }
    }

    private fun updateState(update: (HomeState) -> HomeState) {
        _uiState.update(update)
    }

    private fun refresh() {
        val currentLevel = _uiState.value.postLevel
        viewModelScope.launch {
                updateState { it.copy(isRefreshing = true) }
            
            // Recreate paging flow so UI can treat this like a "pull-to-refresh" event.
            // This mirrors the android-client approach where refresh re-requests the paging flow.
            updateState {
                it.copy(
                    postsFlow = feedRepository.getPagedPosts(currentLevel, forceRefresh = true).cachedIn(viewModelScope),
                )
            }

            updateState { it.copy(isRefreshing = false) }
        }
    }

    private fun updateSearchQuery(query: String) {
        updateState { it.copy(query = query) }
    }

    private fun navigateToCreatePost() {
        viewModelScope.launch {
            _sideEffects.emit(HomeSideEffect.NavigateToCreatePost)
        }
    }

    private fun navigateToProfile() {
        viewModelScope.launch {
            _sideEffects.emit(HomeSideEffect.NavigateToProfile)
        }
    }

    private fun like(postId: String) {
        viewModelScope.launch {
            postRepository.likePost(postId)
                .onFailure { handleError(it) }
        }
    }

    private fun report(postId: String, reason: String?) {
        viewModelScope.launch {
            postRepository.reportPost(postId, reason)
                .onSuccess {
                    _sideEffects.emit(HomeSideEffect.ShowSnackbar("Post reported successfully"))
                }
                .onFailure { handleError(it) }
        }
    }

    private fun comment(postId: String, comment: String) {
        viewModelScope.launch {
            postRepository.addComment(postId, comment)
                .onFailure { handleError(it) }
        }
    }

    private fun share(post: Post) {
        viewModelScope.launch {
            if (post != null) {
                val shareText = "${post.userName}: ${post.postText}\n\nShared from IssueSpot"
                _sideEffects.emit(HomeSideEffect.SharePost(shareText))
            }
        }
    }

    private fun clearError() {
        updateState { it.copy(error = null) }
    }

    private suspend fun handleError(error: Throwable) {
        val message = error.message ?: "Something went wrong"
        updateState { it.copy(error = message) }
        _sideEffects.emit(HomeSideEffect.ShowError(message))
    }
}

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data class SearchQueryChanged(val query: String) : HomeIntent
    data object CreatePostClicked : HomeIntent
    data object ProfileClicked : HomeIntent
    data class ReportClicked(val postId: String, val reason: String? = null) : HomeIntent
    data class LikeClicked(val postId: String) : HomeIntent
    data class CommentClicked(val postId: String, val comment: String) : HomeIntent
    data class ShareClicked(val post: Post) : HomeIntent
    data object ErrorShown : HomeIntent
}

data class HomeState(
    val postLevel: PostLevel = PostLevel.LOCALITY,
    val activeIssues: DataState<Int> = DataState.Loading,
    val isRefreshing: Boolean = false,
    val postsFlow: Flow<PagingData<Post>>? = null,
    val query: String = "",
    val error: String? = null
)

sealed interface HomeSideEffect {
    data object NavigateToCreatePost : HomeSideEffect
    data object NavigateToProfile : HomeSideEffect
    data class ShowError(val message: String) : HomeSideEffect
    data class ShowSnackbar(val message: String) : HomeSideEffect
    data class SharePost(val text: String) : HomeSideEffect
}
