package org.example.project.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertHeaderItem
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
import kotlinx.coroutines.launch

import org.example.project.core.data.repository.FeedRepository
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.datastore.UserPreferencesRepository
import org.example.project.core.model.home.Comment
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.utils.DataState
import org.example.project.feature.home.CurrentLevelManager

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

    private val conversationCache = mutableMapOf<String, Flow<PagingData<Comment>>>()

    private val _activeCommentsFlow = MutableStateFlow<Flow<PagingData<Comment>>?>(null)
    val activeCommentsFlow = _activeCommentsFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val pagedPosts: Flow<PagingData<Post>> = combine(
        currentLevelManager.currentLevel,
        _uiState.map { it.query }.debounce(300).distinctUntilChanged(),
        prefRepository.userData.map { it.userLocation }.distinctUntilChanged()
    ) { level, query, location ->
        Triple(level, query, location)
    }.flatMapLatest { (level, query, location) ->
        println("[KMP_PAGING_VIEWMODEL]\nNEW PAGING FLOW\nlevel=$level\nquery=$query\nlocation=$location\ntime=${kotlin.time.Clock.System.now()}")
        if (query.isBlank()) {
            feedRepository.getPagedPosts(level, location, forceRefresh = false)
        } else {
            feedRepository.getPagedSearchPosts(query, level)
        }
    }.onEach {
        println("[PAGING_VM] PAGING DATA EMITTED | flowHash=${System.identityHashCode(it)} | time=${kotlin.time.Clock.System.now()}")
    }.cachedIn(viewModelScope)

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
            is HomeIntent.ShowRefreshErrorSnackbar -> {
                viewModelScope.launch { _sideEffects.send(HomeSideEffect.ShowSnackbar(intent.message)) }
            }
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

    private fun comments(postId: String): Flow<PagingData<Comment>> {
        return conversationCache.getOrPut(postId) {
            postRepository.getPagedComments(postId).cachedIn(viewModelScope)
        }
    }

    private fun openComments(postId: String) {
        val cachedFlow = comments(postId)
        _activeCommentsFlow.value = cachedFlow
        updateState { it.copy(showCommentsSheetForPostId = postId) }
    }

    private fun dismissComments() {
        _activeCommentsFlow.value = null
        updateState { it.copy(showCommentsSheetForPostId = null) }
    }

    private fun comment(postId: String, comment: String) {
        val currentFlow = conversationCache[postId]
        if (currentFlow != null) {
            val optimisticComment = Comment(
                id = "temp_${kotlin.random.Random.nextLong()}",
                postId = postId,
                text = comment,
                timeAgo = "Just now",
                userName = "You",
                userImageUrl = _uiState.value.currentUserImage ?: ""
            )
            
            val updatedFlow = currentFlow.map { pagingData ->
                pagingData.insertHeaderItem(item = optimisticComment)
            }
            conversationCache[postId] = updatedFlow
            _activeCommentsFlow.value = updatedFlow
            
            viewModelScope.launch {
                when (val result = postRepository.addComment(postId, comment)) {
                    is DataState.Error -> {
                        conversationCache[postId] = currentFlow
                        _activeCommentsFlow.value = currentFlow
                        handleError(result.exception)
                    }
                    else -> {}
                }
            }
        } else {
            viewModelScope.launch {
                when (val result = postRepository.addComment(postId, comment)) {
                    is DataState.Error -> handleError(result.exception)
                    else -> {}
                }
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
        val message = error.message ?: "Something went wrong"
        updateState { it.copy(error = message) }
        _sideEffects.send(HomeSideEffect.ShowError(message))
    }

    private fun showPostDetail(postId: String) {
        updateState { it.copy(expandedPostId = postId) }
    }

    private fun closePostDetail() {
        updateState { it.copy(expandedPostId = null) }
    }
}

sealed interface HomeIntent {
    data class ShowRefreshErrorSnackbar(val message: String) : HomeIntent
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
    data class ShowError(val message: String) : HomeSideEffect
    data class ShowSnackbar(val message: String) : HomeSideEffect
    data class SharePost(val text: String) : HomeSideEffect
}