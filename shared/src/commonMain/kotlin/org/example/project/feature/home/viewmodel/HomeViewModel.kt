
package org.example.project.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.example.project.core.presentation.FeedRefreshReason
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.example.project.core.presentation.FeedState
import org.example.project.core.presentation.PaginationState

import org.example.project.core.data.repository.FeedRepository
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.model.home.Comment
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.utils.DataState
import org.example.project.core.model.auth.UserLocation
import org.example.project.feature.home.CurrentLevelManager
import kotlin.time.Clock

@OptIn(FlowPreview::class)
class HomeViewModel(
    private val feedRepository: FeedRepository,
    private val postRepository: PostRepository,
    private val profileRepository: ProfileRepository,
    private val prefRepository: UserPreferencesRepository,
    private val currentLevelManager: CurrentLevelManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _sideEffects = Channel<HomeSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    private val _activeCommentsFlow = MutableStateFlow<StateFlow<PaginationState<Comment>>?>(null)
    val activeCommentsFlow: StateFlow<StateFlow<PaginationState<Comment>>?> = _activeCommentsFlow.asStateFlow()
    
    private val _optimisticComments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())

    val feedState: StateFlow<FeedState> = feedRepository.feedState
    val searchState: StateFlow<FeedState> = feedRepository.searchState

    init {
        viewModelScope.launch {
            combine(
                currentLevelManager.currentLevel,
                prefRepository.userData.map { it.userLocation }.distinctUntilChanged()
            ) { level, location ->
                Pair(level, location)
            }.collect { (level, location) ->
                feedRepository.initializeFeedForLevel(level, location)
            }
        }

        // PAGING PIPELINE STEP 4: VIEWMODEL DEBOUNCER
        // This block handles search pagination logic efficiently.
        // As the user types into the search bar, the UI updates `uiState.query`.
        // To prevent spamming the network on every single keystroke, we apply a debounce of 300ms.
        viewModelScope.launch {
            combine(
                currentLevelManager.currentLevel,
                // Wait 300ms after the user stops typing. If the query is distinct, proceed.
                _uiState.map { it.query }.debounce(300).distinctUntilChanged()
            ) { level, query ->
                Pair(level, query)
            }.collect { (level, query) ->
                if (query.isBlank()) {
                    // Stop search pagination and clear the search state
                    feedRepository.clearSearch()
                } else {
                    // Trigger the first page of search results for the given query
                    feedRepository.startSearch(query, level)
                }
            }
        }
    }

    val currentLevel: StateFlow<PostLevel> = currentLevelManager.currentLevel
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PostLevel.LOCALITY
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeIssues: StateFlow<Int> = currentLevelManager.currentLevel
        .flatMapLatest { level -> feedRepository.observeActiveIssuesCount(level) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val expandedPost: StateFlow<Post?> = _uiState
        .map { it.expandedPostId }
        .distinctUntilChanged()
        .flatMapLatest { postId ->
            if (postId == null) flowOf(null)
            else postRepository.observePost(postId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            profileRepository.observeProfile().collect { dataState ->
                if (dataState is DataState.Success && dataState.data != null) {
                    updateState { it.copy(currentUserImage = dataState.data.imageUrl) }
                }
            }
        }
    }

    fun loadMoreComments(postId: String) {
        postRepository.loadMoreComments(postId)
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SearchQueryChanged -> updateState { it.copy(query = intent.query) }
            HomeIntent.CreatePostClicked -> navigateToCreatePost()
            HomeIntent.ProfileClicked -> navigateToProfile()
            is HomeIntent.ReportClicked -> report(intent.postId, intent.reason)
            is HomeIntent.CommentsIconClicked -> openComments(intent.postId)
            HomeIntent.DismissCommentsSheet -> dismissComments()
            is HomeIntent.LikeClicked -> like(intent.postId)
            is HomeIntent.CommentSubmitted -> comment(intent.postId, intent.commentText)
            is HomeIntent.ShareClicked -> share(intent.post)
            is HomeIntent.PostClicked -> showPostDetail(intent.postId)
            HomeIntent.DismissPost -> closePostDetail()
            HomeIntent.ErrorShown -> clearError()
            is HomeIntent.ChangeLevel -> changeLevel(intent.level)
            is HomeIntent.ShowRefreshErrorDialog -> {
                viewModelScope.launch { _sideEffects.send(HomeSideEffect.ShowDialog(intent.message)) }
            }
            HomeIntent.LoadMorePosts -> feedRepository.loadMore()
            is HomeIntent.RefreshPosts,
            HomeIntent.RefreshCurrentPosts -> feedRepository.refresh(FeedRefreshReason.PULL_TO_REFRESH)
            HomeIntent.RetryPosts -> feedRepository.retry()
            HomeIntent.LoadMoreSearchPosts -> feedRepository.loadMoreSearch()
            HomeIntent.RefreshSearchPosts -> feedRepository.refreshSearch()
            HomeIntent.RetrySearchPosts -> feedRepository.retrySearch()
        }
    }

    private fun changeLevel(level: PostLevel) {
        viewModelScope.launch {
            currentLevelManager.updateLevel(level)
        }
    }

    private fun updateState(update: (HomeState) -> HomeState) {
        _uiState.update(update)
    }

    private fun navigateToCreatePost() {
        viewModelScope.launch { _sideEffects.send(HomeSideEffect.NavigateToCreatePost) }
    }

    private fun navigateToProfile() {
        viewModelScope.launch { _sideEffects.send(HomeSideEffect.NavigateToProfile) }
    }

    private fun like(postId: String) {
        viewModelScope.launch {
            when (val result = postRepository.likePost(postId)) {
                is DataState.Error -> handleError(result.exception)
                else -> Unit
            }
        }
    }

    private fun report(postId: String, reason: String?) {
        viewModelScope.launch {
            when (val result = postRepository.reportPost(postId, reason)) {
                is DataState.Error -> handleError(result.exception)
                else -> Unit
            }
        }
    }

    private fun openComments(postId: String) {
        postRepository.startComments(postId)
        val repoFlow = postRepository.observeComments(postId)
        
        val combinedFlow = combine(repoFlow, _optimisticComments) { state, optimisticMap ->
            val optimisticList = optimisticMap[postId] ?: emptyList()
            if (optimisticList.isEmpty()) {
                state
            } else {
                state.copy(items = optimisticList + state.items)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PaginationState()
        )
        
        _activeCommentsFlow.value = combinedFlow
        updateState { it.copy(showCommentsSheetForPostId = postId) }
    }

    private fun dismissComments() {
        val postId = _uiState.value.showCommentsSheetForPostId
        if (postId != null) {
            _optimisticComments.update { map -> map - postId }
        }
        _activeCommentsFlow.value = null
        updateState { it.copy(showCommentsSheetForPostId = null) }
    }

    private fun comment(postId: String, commentText: String) {
        val tempId = "temp_${Clock.System.now().toEpochMilliseconds()}"
        val newComment = Comment(
            id = tempId,
            postId = postId,
            text = commentText,
            timeAgo = "Just now",
            userName = "You",
            userImageUrl = _uiState.value.currentUserImage
        )
        
        _optimisticComments.update { map ->
            val list = map[postId] ?: emptyList()
            map + (postId to (listOf(newComment) + list))
        }
        
        viewModelScope.launch {
            when (val result = postRepository.addComment(postId, commentText)) {
                is DataState.Success -> {
                    // Do nothing, leave it in optimistic comments. It will be cleared when the sheet is dismissed.
                }
                is DataState.Error -> {
                    _optimisticComments.update { map ->
                        val list = map[postId] ?: emptyList()
                        map + (postId to list.filterNot { it.id == tempId })
                    }
                    handleError(result.exception)
                }
                else -> Unit
            }
        }
    }

    private fun share(post: Post) {
        viewModelScope.launch {
            val postUrl = "https://www.issuespot.com/post/${post.id}"
            val shareText = "${post.userName} posted an issue: ${post.postText}\n\nView it here: $postUrl"
            _sideEffects.send(HomeSideEffect.SharePost(shareText))
        }
    }

    private fun clearError() {
        updateState { it.copy(error = null) }
    }

    private suspend fun handleError(error: Throwable) {
        val message = error.message ?: "Something went wrong.\n\nPlease try again."
        updateState { it.copy(error = message) }
        _sideEffects.send(HomeSideEffect.ShowDialog(message))
    }

    private fun showPostDetail(postId: String) {
        updateState { it.copy(expandedPostId = postId) }
    }

    private fun closePostDetail() {
        updateState { it.copy(expandedPostId = null) }
    }
}

sealed interface HomeIntent {
    data class ShowRefreshErrorDialog(val message: String) : HomeIntent
    data class SearchQueryChanged(val query: String) : HomeIntent
    data object CreatePostClicked : HomeIntent
    data object ProfileClicked : HomeIntent
    data class ReportClicked(val postId: String, val reason: String) : HomeIntent
    data class CommentsIconClicked(val postId: String) : HomeIntent
    data object DismissCommentsSheet : HomeIntent

    data class LikeClicked(val postId: String) : HomeIntent
    data class CommentSubmitted(val postId: String, val commentText: String) : HomeIntent
    data class ShareClicked(val post: Post) : HomeIntent
    data class PostClicked(val postId: String) : HomeIntent
    data object DismissPost : HomeIntent
    data object ErrorShown : HomeIntent
    data class ChangeLevel(val level: PostLevel) : HomeIntent
    data object LoadMorePosts : HomeIntent
    data class RefreshPosts(val location: UserLocation = UserLocation()) : HomeIntent
    data object RefreshCurrentPosts : HomeIntent
    data object RetryPosts : HomeIntent
    data object LoadMoreSearchPosts : HomeIntent
    data object RefreshSearchPosts : HomeIntent
    data object RetrySearchPosts : HomeIntent
}

data class HomeState(
    val query: String = "",
    val error: String? = null,
    val showCommentsSheetForPostId: String? = null,
    val expandedPostId: String? = null,
    val currentUserImage: String? = null
)

sealed interface HomeSideEffect {
    data object NavigateToCreatePost : HomeSideEffect
    data object NavigateToProfile : HomeSideEffect
    data class ShowDialog(val message: String) : HomeSideEffect
    data class SharePost(val text: String) : HomeSideEffect
}
