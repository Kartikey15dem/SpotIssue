package org.example.project.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.data.mappers.Sort
import org.example.project.core.presentation.FeedRefreshReason
import org.example.project.core.presentation.FeedState
import org.example.project.core.presentation.PaginationState
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.Comment
import org.example.project.core.model.profile.Profile
import org.example.project.core.utils.DataState
import kotlin.time.Clock


class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<ProfileSideEffect>()
    val sideEffects = _sideEffects.asSharedFlow()

    private val _activeCommentsFlow = MutableStateFlow<StateFlow<PaginationState<Comment>>?>(null)
    val activeCommentsFlow: StateFlow<StateFlow<PaginationState<Comment>>?> = _activeCommentsFlow.asStateFlow()
    
    private val _optimisticComments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())

    val profilePostsState: StateFlow<FeedState> = profileRepository.profilePostsState

    init {
        profileRepository.clearRefreshState()
        observeProfile()
        fetchProfile()
        observePostSelection()
    }

    private fun observePostSelection() {
        viewModelScope.launch {
            _uiState
                .map { Pair(it.isMine, it.sort) }
                .distinctUntilChanged()
                .collect { (isMine, sort) ->
                    profileRepository.startProfilePosts(isMine = isMine, sort = sort.name.lowercase())
                }
        }
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

    private fun changeSort(sort: Sort) {
        if (_uiState.value.sort == sort) return
        updateState { it.copy(sort = sort) }
    }

    private fun deletePost(postId: String) {
        viewModelScope.launch {
            when (val result = postRepository.deletePost(postId)) {
                is DataState.Success -> {
                    // Close expanded post if it was deleted
                    if (_uiState.value.expandedPost?.id == postId) {
                        updateState { it.copy(expandedPost = null) }
                    }
                }
                is DataState.Error -> {
                    handleError(result.exception)
                }
                else -> Unit
            }
        }
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
            userImageUrl = _uiState.value.profile?.imageUrl
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
            _sideEffects.emit(ProfileSideEffect.SharePost(shareText))
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


    private fun showPostDetail(post: Post) {
        updateState { it.copy(expandedPost = post) }
    }

    private fun closePostDetail() {
        updateState { it.copy(expandedPost = null) }
    }

    fun loadMoreComments(postId: String) {
        postRepository.loadMoreComments(postId)
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.CreatePostClicked -> navigateToCreatePost()
            ProfileIntent.EditProfileClicked -> navigateToEditProfile()
            is ProfileIntent.TabChanged -> changeTab(intent.isMine)
            is ProfileIntent.SortChanged -> changeSort(intent.sort)
            is ProfileIntent.DeletePostClicked -> deletePost(intent.postId)
            is ProfileIntent.LikeClicked -> like(intent.postId)
            is ProfileIntent.CommentsIconClicked -> openComments(intent.postId)
            ProfileIntent.DismissCommentsSheet -> dismissComments()
            is ProfileIntent.CommentSubmitted -> comment(intent.postId, intent.commentText)
            is ProfileIntent.PostClicked -> showPostDetail(intent.post)
            ProfileIntent.DismissPost -> closePostDetail()
            is ProfileIntent.ShareClicked -> share(intent.post)
            is ProfileIntent.ReportClicked -> report(intent.postId, intent.reason)
            ProfileIntent.ErrorShown -> clearError()
            ProfileIntent.RetryProfileClicked -> fetchProfile()
            is ProfileIntent.ShowRefreshErrorDialog -> {}
            ProfileIntent.LoadMorePosts -> profileRepository.loadMoreProfilePosts()
            ProfileIntent.RefreshPosts -> profileRepository.refreshProfilePosts(FeedRefreshReason.PULL_TO_REFRESH)
            ProfileIntent.RetryPosts -> profileRepository.retryProfilePosts()
        }
    }

    private fun changeTab(isMine: Boolean) {
        if (_uiState.value.isMine == isMine) return
        updateState { it.copy(isMine = isMine) }
    }

    fun selectMine(isMine: Boolean) = onIntent(ProfileIntent.TabChanged(isMine))
    fun selectSort(sort: Sort) = onIntent(ProfileIntent.SortChanged(sort))
    fun openCreatePost() = onIntent(ProfileIntent.CreatePostClicked)
    fun openEditProfile() = onIntent(ProfileIntent.EditProfileClicked)
    fun openPost(post: Post) = onIntent(ProfileIntent.PostClicked(post))
    fun closePost() = onIntent(ProfileIntent.DismissPost)
    fun likePost(post: Post) = onIntent(ProfileIntent.LikeClicked(post.id))
    fun openComments(post: Post) = onIntent(ProfileIntent.CommentsIconClicked(post.id))
    fun closeComments() = onIntent(ProfileIntent.DismissCommentsSheet)
    fun submitComment(postId: String, text: String) =
        onIntent(ProfileIntent.CommentSubmitted(postId, text))
    fun sharePost(post: Post) = onIntent(ProfileIntent.ShareClicked(post))

    private fun clearError() {
        updateState { it.copy(error = null) }
    }

    private fun updateState(update: (ProfileState) -> ProfileState) {
        _uiState.update(update)
    }

    private suspend fun handleError(error: Throwable) {
        val message = error.message ?: "Something went wrong.\n\nPlease try again."
        updateState { it.copy(error = message) }
        _sideEffects.emit(ProfileSideEffect.ShowDialog(message))
    }
}

sealed interface ProfileIntent {
    data object CreatePostClicked : ProfileIntent
    data object EditProfileClicked : ProfileIntent
    data class TabChanged(val isMine: Boolean) : ProfileIntent
    data class SortChanged(val sort: Sort) : ProfileIntent
    data class DeletePostClicked(val postId: String) : ProfileIntent
    data class LikeClicked(val postId: String) : ProfileIntent
    data class CommentsIconClicked(val postId: String) : ProfileIntent
    data object DismissCommentsSheet : ProfileIntent
    data class CommentSubmitted(val postId: String, val commentText: String) : ProfileIntent
    data class ShareClicked(val post: Post) : ProfileIntent
    data class ReportClicked(val postId: String, val reason: String?) : ProfileIntent
    data class PostClicked(val post: Post) : ProfileIntent
    data object DismissPost : ProfileIntent
    data object ErrorShown : ProfileIntent
    data object RetryProfileClicked : ProfileIntent
    data class ShowRefreshErrorDialog(val message: String) : ProfileIntent
    data object LoadMorePosts : ProfileIntent
    data object RefreshPosts : ProfileIntent
    data object RetryPosts : ProfileIntent
}

data class ProfileState(
    val profile: Profile? = null,
    val isProfileLoading: Boolean = true,
    val isMine: Boolean = true,
    val sort: Sort = Sort.LATEST,
    val error: String? = null,
    val profileError: String? = null,
    val showCommentsSheetForPostId: String? = null,
    val expandedPost: Post? = null
)

sealed interface ProfileSideEffect {
    data object NavigateToCreatePost : ProfileSideEffect
    data object NavigateToEditProfile : ProfileSideEffect
    data class NavigateToPost(val postId: String) : ProfileSideEffect
    data class ShowDialog(val message: String) : ProfileSideEffect
    data class SharePost(val text: String) : ProfileSideEffect
    data class OpenMediaViewer(val postId: String) : ProfileSideEffect
}
