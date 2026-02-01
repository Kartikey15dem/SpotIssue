package org.example.project.home.presentation.viewmodel

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
import org.example.project.home.domain.usecases.GetActiveIssuesUseCase
import org.example.project.home.domain.usecases.GetCachedActiveIssuesUseCase
import org.example.project.home.domain.usecases.GetCachedPostsUseCase
import org.example.project.home.domain.usecases.GetPostsUseCase
import org.example.project.home.domain.usecases.IsCacheStaleUseCase
import org.example.project.home.domain.usecases.PostActionsUseCase
import org.example.project.home.domain.usecases.RefreshPostsUseCase
import org.example.project.home.presentation.CurrentLevelManager
import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel

class HomeViewModel(
    private val postActions: PostActionsUseCase,
    private val getPostsUseCase: GetPostsUseCase,
    private val getCachedPostsUseCase: GetCachedPostsUseCase,
    private val getActiveIssuesUseCase: GetActiveIssuesUseCase,
    private val getCachedActiveIssuesUseCase: GetCachedActiveIssuesUseCase,
    private val refreshPostsUseCase: RefreshPostsUseCase,
    private val isCacheStaleUseCase: IsCacheStaleUseCase,
    private val currentLevelManager: CurrentLevelManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<HomeSideEffect>()
    val sideEffects: SharedFlow<HomeSideEffect> = _sideEffects.asSharedFlow()


    init {
        viewModelScope.launch {
            currentLevelManager.currentLevel.collect { level ->
                _uiState.update { it.copy(postLevel = level) }
                loadDataForLevel(level)
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

    /**
     * Dual-loading strategy:
     * 1. Load cached data immediately (instant UI update)
     * 2. Check if cache is stale
     * 3. If stale, fetch from API (show loading but keep cached data visible)
     * 4. Update both in-memory cache and Room DB when fresh data arrives
     */
    private fun loadDataForLevel(postLevel: PostLevel) {
        viewModelScope.launch {
            // Step 1: Load cached data from Room DB immediately
            loadCachedData(postLevel)

            // Step 2: Check if we need to fetch from API
            val isPostsStale = isCacheStaleUseCase.forPosts(postLevel)
            val isActiveIssuesStale = isCacheStaleUseCase.forActiveIssues(postLevel)

            if (isPostsStale || isActiveIssuesStale) {
                // Step 3: Fetch fresh data from API (show loading indicator)
                _uiState.update { it.copy(isRefreshing = true) }

                if (isPostsStale) {
                    loadFreshPosts(postLevel)
                }

                if (isActiveIssuesStale) {
                    loadFreshActiveIssues(postLevel)
                }

                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /**
     * Load cached data from Room DB instantly
     */
    private suspend fun loadCachedData(postLevel: PostLevel) {
        // Load cached posts
        getCachedPostsUseCase(postLevel)
            .onSuccess { posts ->
                if (posts.isNotEmpty()) {
                    _uiState.update { it.copy(feeds = posts) }
                }
            }

        // Load cached active issues count
        getCachedActiveIssuesUseCase(postLevel)
            .onSuccess { count ->
                count?.let {
                    _uiState.update { state -> state.copy(activeIssues = it) }
                }
            }
    }

    /**
     * Fetch fresh posts from API
     */
    private suspend fun loadFreshPosts(postLevel: PostLevel) {
        refreshPostsUseCase(postLevel)
            .onSuccess { posts ->
                _uiState.update { it.copy(feeds = posts, error = null) }
            }
            .onFailure { error ->
                handleError(error)
            }
    }

    /**
     * Fetch fresh active issues count from API
     */
    private suspend fun loadFreshActiveIssues(postLevel: PostLevel) {
        getActiveIssuesUseCase(postLevel)
            .onSuccess { count ->
                _uiState.update { it.copy(activeIssues = count) }
            }
            .onFailure { error ->
                // Silent failure for active issues count
                _uiState.update { it.copy(activeIssues = 0) }
            }
    }

    private fun refresh() {
        val currentLevel = _uiState.value.postLevel
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            // Force refresh both posts and active issues
            loadFreshPosts(currentLevel)
            loadFreshActiveIssues(currentLevel)

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        // TODO: Implement search filtering logic
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
            postActions.like(postId)
                .onFailure { handleError(it) }
        }
    }

    private fun report(postId: String, reason: String?) {
        viewModelScope.launch {
            postActions.report(postId, reason)
                .onSuccess {
                    _sideEffects.emit(HomeSideEffect.ShowSnackbar("Post reported successfully"))
                }
                .onFailure { handleError(it) }
        }
    }

    private fun comment(postId: String, comment: String) {
        viewModelScope.launch {
            postActions.comment(postId, comment)
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
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun handleError(error: Throwable) {
        val message = error.message ?: "Something went wrong"
        _uiState.update { it.copy(error = message) }
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
    val activeIssues: Int = 0,
    val isRefreshing: Boolean = false, // Shows loading indicator while keeping cached data visible
    val feeds: List<Post> = emptyList(),
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

