package org.example.project.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertHeaderItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.core.data.repository.FeedRepository
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.model.home.Comment
import org.example.project.core.utils.DataState
import org.example.project.home.presentation.CurrentLevelManager
import org.example.project.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.flow.combine

class HomeViewModel(
    private val feedRepository: FeedRepository,
    private val postRepository: PostRepository,
    private val prefRepository: UserPreferencesRepository,
    private val currentLevelManager: CurrentLevelManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<HomeSideEffect>()
    val sideEffects: SharedFlow<HomeSideEffect> = _sideEffects.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                currentLevelManager.currentLevel,
                prefRepository.userData
            ) { level, userData ->
                level to userData.userLocation
            }.collect { (level, location) ->
                updateState { it.copy(
                    postLevel = level,
                    postsFlow = feedRepository.getPagedPosts(level, location, forceRefresh = true).cachedIn(viewModelScope)
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
            is HomeIntent.CommentsIconClicked -> openComments(intent.postId, intent.currentCommentsCount)
            HomeIntent.DismissCommentsSheet -> dismissComments()
            is HomeIntent.LikeClicked -> like(intent.postId, intent.currentIsLiked, intent.currentLikesCount)
            is HomeIntent.CommentSubmitted -> comment(intent.postId, intent.commentText, intent.currentCommentCount)
            is HomeIntent.ShareClicked -> share(intent.post)
            is HomeIntent.PostClicked -> showPostDetail(intent.post)
            is HomeIntent.DismissPost -> closePostDetial()
            HomeIntent.ErrorShown -> clearError()
        }
    }

    private fun updateState(update: (HomeState) -> HomeState) {
        _uiState.update(update)
    }

    private fun refresh() {
        val currentLevel = _uiState.value.postLevel
        val currentLocation = prefRepository.userData.value.userLocation
        viewModelScope.launch {
            updateState { it.copy(isRefreshing = true) }
            updateState {
                it.copy(
                    postsFlow = feedRepository.getPagedPosts(currentLevel, currentLocation, forceRefresh = true).cachedIn(viewModelScope),
                )
            }
            updateState { it.copy(isRefreshing = false) }
        }
    }

    private var searchJob: Job? = null

    private fun updateSearchQuery(query: String) {
        updateState { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            updateState { it.copy(searchPostsFlow = null) }
            return
        }
        val currentLevel = _uiState.value.postLevel
        searchJob = viewModelScope.launch {
            delay(300)
            val flow = feedRepository.getPagedSearchPosts(query, currentLevel).cachedIn(viewModelScope)
            updateState { it.copy(searchPostsFlow = flow) }
        }
    }

    private fun navigateToCreatePost() {
        viewModelScope.launch { _sideEffects.emit(HomeSideEffect.NavigateToCreatePost) }
    }

    private fun navigateToProfile() {
        viewModelScope.launch { _sideEffects.emit(HomeSideEffect.NavigateToProfile) }
    }

    private fun like(postId: String, currentIsLiked: Boolean, currentLikesCount: Int) {
        val targetIsLiked = !currentIsLiked
        val targetLikesCount = if (targetIsLiked) currentLikesCount + 1 else (currentLikesCount - 1).coerceAtLeast(0)
        updateOverride(postId, isLiked = targetIsLiked, likesCount = targetLikesCount)

        viewModelScope.launch {
            when (val result = postRepository.likePost(postId)) {
                is DataState.Error -> {
                    updateOverride(postId, isLiked = currentIsLiked, likesCount = currentLikesCount)
                    handleError(Throwable("Failed to update like status. Please try again."))
                }
                else -> Unit
            }
        }
    }

    private fun report(postId: String, reason: String?) {
        val currentIsReported = _uiState.value.postOverrides[postId]?.isReported ?: false
        updateOverride(postId, isReported = true)
        viewModelScope.launch {
            when (val result = postRepository.reportPost(postId, reason)) {
                is DataState.Success -> _sideEffects.emit(HomeSideEffect.ShowSnackbar("Post reported successfully"))
                is DataState.Error -> {
                    updateOverride(postId, isReported = currentIsReported)
                    handleError(Throwable("Failed to submit report. Please check your connection."))
                }
                else -> Unit
            }
        }
    }

    private fun openComments(postId: String, currentCommentsCount: Int) {
        val existingOverride = _uiState.value.postOverrides[postId]
        if (existingOverride?.commentsFlow == null) {
            val flow = postRepository.getPagedComments(postId).cachedIn(viewModelScope)
            updateOverride(postId, commentsFlow = flow, commentsCount = existingOverride?.commentsCount ?: currentCommentsCount)
        }
        updateState { it.copy(showCommentsSheetForPostId = postId) }
    }

    private fun dismissComments() {
        updateState { it.copy(showCommentsSheetForPostId = null) }
    }

    private fun comment(postId: String, comment: String, currentCommentCount: Int) {
        updateOverride(postId, commentsCount = currentCommentCount + 1)
        
        val currentFlow = _uiState.value.postOverrides[postId]?.commentsFlow
        if (currentFlow != null) {
            val optimisticComment = Comment(
                id = "temp_${System.currentTimeMillis()}",
                postId = postId,
                text = comment,
                timeAgo = "Just now",
                userName = "You",
                userImageUrl = null
            )
            val updatedFlow = currentFlow.map { pagingData ->
                pagingData.insertHeaderItem(item = optimisticComment)
            }
            updateOverride(postId, commentsFlow = updatedFlow)
        }

        viewModelScope.launch {
            when (val result = postRepository.addComment(postId, comment)) {
                is DataState.Success -> {
                    _sideEffects.emit(HomeSideEffect.ShowSnackbar("Comment posted successfully!"))
                }
                is DataState.Error -> {
                    updateOverride(postId, commentsCount = currentCommentCount)
                    handleError(Throwable("Could not post comment. Connection lost."))
                }
                else -> Unit
            }
        }
    }

    private fun share(post: Post) {
        viewModelScope.launch {
            val postUrl = "https://www.issuespot.com/post/${post.id}"
            val shareText = "${post.userName} posted an issue: ${post.postText}\n\nView it here: $postUrl"
            _sideEffects.emit(HomeSideEffect.SharePost(shareText))
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

    private fun showPostDetail(post : Post) {
        updateState { it.copy(expandedPost = post) }
    }

    private fun closePostDetial(){
        updateState { it.copy(expandedPost = null) }
    }

    private fun updateOverride(
        postId: String,
        isLiked: Boolean? = null,
        likesCount: Int? = null,
        commentsCount: Int? = null,
        isReported: Boolean? = null,
        commentsFlow: Flow<PagingData<Comment>>? = null
    ) {
        updateState { currentState ->
            val existingOverride = currentState.postOverrides[postId]
            val newOverride = PostOverride(
                isLiked = isLiked ?: existingOverride?.isLiked,
                likesCount = likesCount ?: existingOverride?.likesCount,
                commentsCount = commentsCount ?: existingOverride?.commentsCount,
                isReported = isReported ?: existingOverride?.isReported,
                commentsFlow = commentsFlow ?: existingOverride?.commentsFlow
            )
            currentState.copy(postOverrides = currentState.postOverrides + (postId to newOverride))
        }
    }
}

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data class SearchQueryChanged(val query: String) : HomeIntent
    data object CreatePostClicked : HomeIntent
    data object ProfileClicked : HomeIntent
    data class ReportClicked(val postId: String, val reason: String) : HomeIntent
    data class CommentsIconClicked(val postId: String, val currentCommentsCount: Int) : HomeIntent
    data object DismissCommentsSheet : HomeIntent

    data class LikeClicked(val postId: String, val currentIsLiked: Boolean, val currentLikesCount: Int) : HomeIntent
    data class CommentSubmitted(val postId: String, val commentText: String, val currentCommentCount: Int) : HomeIntent
    data class ShareClicked(val post: Post) : HomeIntent
    data class PostClicked(val post: Post) : HomeIntent
    data object DismissPost : HomeIntent
    data object ErrorShown : HomeIntent
}

data class HomeState(
    val postLevel: PostLevel = PostLevel.LOCALITY,
    val activeIssues: DataState<Int> = DataState.Loading,
    val isRefreshing: Boolean = false,
    val postsFlow: Flow<PagingData<Post>>? = null,
    val searchPostsFlow: Flow<PagingData<Post>>? = null,
    val query: String = "",
    val error: String? = null,
    val postOverrides: Map<String, PostOverride> = emptyMap(),
    val showCommentsSheetForPostId: String? = null,
    val expandedPost : Post? = null
)

sealed interface HomeSideEffect {
    data object NavigateToCreatePost : HomeSideEffect
    data object NavigateToProfile : HomeSideEffect
    data class ShowError(val message: String) : HomeSideEffect
    data class ShowSnackbar(val message: String) : HomeSideEffect
    data class SharePost(val text: String) : HomeSideEffect
}

data class PostOverride(
    val isLiked: Boolean? = null,
    val likesCount: Int? = null,
    val commentsCount: Int? = null,
    val isReported: Boolean? = null,
    val commentsFlow: Flow<PagingData<Comment>>? = null
)