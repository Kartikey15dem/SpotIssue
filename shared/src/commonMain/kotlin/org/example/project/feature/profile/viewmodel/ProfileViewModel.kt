package org.example.project.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertHeaderItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import org.example.project.core.data.repository.PostRepository
import org.example.project.core.data.repository.ProfileRepository
import org.example.project.core.data.mappers.Sort
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.Comment
import org.example.project.core.model.profile.Profile
import org.example.project.core.utils.DataState
import kotlin.time.Clock

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    /* ===================================================================================
     * SECTION: PROFILE & SMART SORTING ARCHITECTURE
     * ===================================================================================
     * Manages the complex intersection of user profile analytics and their personal feed.
     * 
     * Optimistic UI (PostOverrides):
     * Implements a local override map (`postOverrides`) to instantly reflect likes and
     * comments in the UI without waiting for an expensive Paging3 invalidation/network call.
     * 
     * Smart Sorting:
     * Listens to tab changes (My Posts vs Liked Posts) and Sorting parameters (Latest, 
     * Oldest, Popular) and swaps the underlying PagingData Flow dynamically.
     */

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private val _sideEffects = Channel<ProfileSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    private val userPostsCache = mutableMapOf<Sort, Flow<PagingData<Post>>>()
    private val likedPostsCache = mutableMapOf<Sort, Flow<PagingData<Post>>>()
    private var userPostsListJob: Job? = null
    private var likedPostsListJob: Job? = null

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

    private fun getPostFlow(sort: Sort): Flow<PagingData<Post>> {
        return userPostsCache.getOrPut(sort) {
            profileRepository.getPagedUserPosts(sort.name).cachedIn(viewModelScope)
        }
    }

    private fun getLikedFlow(sort: Sort): Flow<PagingData<Post>> {
        return likedPostsCache.getOrPut(sort) {
            profileRepository.getPagedLikedPosts(sort.name).cachedIn(viewModelScope)
        }
    }

    private fun initPostFlows() {
        updateState { it.copy(
            userPostsFlow = getPostFlow(Sort.LATEST),
            likedPostsFlow = getLikedFlow(Sort.LATEST)
        ) }
    }

    private fun changeSort(sort: Sort) {
        updateState { it.copy(sort = sort, userPostsFlow = getPostFlow(sort), likedPostsFlow = getLikedFlow(sort)) }
    }



    private fun refreshPostLists(sort: Sort) {
        viewModelScope.launch {
            updateState { it.copy(isNetworkLoading = true) }
            when (val result = profileRepository.refreshUserPosts(sort.name)) {
                is DataState.Success -> updateState { it.copy(isNetworkLoading = false, isLocalLoading = false) }
                is DataState.Error -> {
                    updateState { it.copy(isNetworkLoading = false) }
                    handleError(result.exception)
                }
                DataState.Loading -> Unit
            }
            when (val result = profileRepository.refreshLikedPosts(sort.name)) {
                is DataState.Success -> updateState { it.copy(isNetworkLoading = false, isLocalLoading = false) }
                is DataState.Error -> {
                    updateState { it.copy(isNetworkLoading = false) }
                    handleError(result.exception)
                }
                DataState.Loading -> Unit
            }
        }
    }

    private fun deletePost(postId: String) {
        viewModelScope.launch {
            when (val result = postRepository.deletePost(postId)) {
                is DataState.Success -> {
                    // Do nothing
                }
                is DataState.Error -> {
                    handleError(result.exception)
                }
                else -> Unit
            }
        }
    }

    private fun like(postId: String, currentIsLiked: Boolean, currentLikesCount: Int) {
        val targetIsLiked = !currentIsLiked
        val targetLikesCount = if (targetIsLiked) currentLikesCount + 1 else (currentLikesCount - 1).coerceAtLeast(0)
        updateOverride(postId, isLiked = targetIsLiked, likesCount = targetLikesCount)

        viewModelScope.launch {
            when (val result = postRepository.likePost(postId)) {
                is DataState.Error -> {
                    updateOverride(postId, isLiked = currentIsLiked, likesCount = currentLikesCount)
                    handleError(result.exception)
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
                is DataState.Success -> {}
                is DataState.Error -> {
                    updateOverride(postId, isReported = currentIsReported)
                    handleError(result.exception)
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
        val optimisticComment = Comment(
            id = "temp_${Clock.System.now()}",
            postId = postId,
            text = comment,
            timeAgo = "Just now",
            userName = "You",
            userImageUrl = _uiState.value.profile?.imageUrl
        )
        
        val currentFlow = _uiState.value.postOverrides[postId]?.commentsFlow
        if (currentFlow != null) {
            val updatedFlow = currentFlow.map { pagingData ->
                pagingData.insertHeaderItem(item = optimisticComment)
            }
            updateOverride(postId, commentsFlow = updatedFlow)
        }

        viewModelScope.launch {
            when (val result = postRepository.addComment(postId, comment)) {
                is DataState.Success -> {
                    // Do nothing
                }
                is DataState.Error -> {
                    updateOverride(postId, commentsCount = currentCommentCount)
                    handleError(result.exception)
                }
                else -> Unit
            }
        }
    }

    private fun share(post: Post) {
        viewModelScope.launch {
            val postUrl = "https://www.issuespot.com/post/${post.id}"
            val shareText = "${post.userName} posted an issue: ${post.postText}\\n\\nView it here: $postUrl"
            _sideEffects.send(ProfileSideEffect.SharePost(shareText))
        }
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

    private fun navigateToCreatePost() {
        viewModelScope.launch {
            _sideEffects.send(ProfileSideEffect.NavigateToCreatePost)
        }
    }

    private fun navigateToEditProfile() {
        viewModelScope.launch {
            _sideEffects.send(ProfileSideEffect.NavigateToEditProfile)
        }
    }

    private fun navigateToPost(postId: String) {
        viewModelScope.launch {
            _sideEffects.send(ProfileSideEffect.NavigateToPost(postId))
        }
    }


    private fun showPostDetail(post: Post) {
        updateState { it.copy(expandedPost = post) }
    }

    private fun closePostDetail() {
        updateState { it.copy(expandedPost = null) }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.CreatePostClicked -> navigateToCreatePost()
            ProfileIntent.EditProfileClicked -> navigateToEditProfile()
            is ProfileIntent.TabChanged -> changeTab(intent.isMine)
            is ProfileIntent.SortChanged -> changeSort(intent.sort)
            is ProfileIntent.DeletePostClicked -> deletePost(intent.postId)
            is ProfileIntent.LikeClicked -> like(intent.postId, intent.currentIsLiked, intent.currentLikesCount)
            is ProfileIntent.CommentsIconClicked -> openComments(intent.postId, intent.currentCommentsCount)
            ProfileIntent.DismissCommentsSheet -> dismissComments()
            is ProfileIntent.CommentSubmitted -> comment(intent.postId, intent.commentText, intent.currentCommentCount)
            is ProfileIntent.PostClicked -> showPostDetail(intent.post)
            ProfileIntent.DismissPost -> closePostDetail()
            is ProfileIntent.ShareClicked -> share(intent.post)
            is ProfileIntent.ReportClicked -> report(intent.postId, intent.reason)
            ProfileIntent.ErrorShown -> clearError()
            ProfileIntent.RetryProfileClicked -> fetchProfile()
        }
    }

    fun selectMine(isMine: Boolean) = onIntent(ProfileIntent.TabChanged(isMine))
    fun selectSort(sort: Sort) = onIntent(ProfileIntent.SortChanged(sort))
    fun openCreatePost() = onIntent(ProfileIntent.CreatePostClicked)
    fun openEditProfile() = onIntent(ProfileIntent.EditProfileClicked)
    fun openPost(post: Post) = onIntent(ProfileIntent.PostClicked(post))
    fun closePost() = onIntent(ProfileIntent.DismissPost)
    fun likePost(post: Post) = onIntent(ProfileIntent.LikeClicked(post.id, post.isLiked, post.likes))
    fun openComments(post: Post) = onIntent(ProfileIntent.CommentsIconClicked(post.id, post.comments))
    fun closeComments() = onIntent(ProfileIntent.DismissCommentsSheet)
    fun submitComment(postId: String, text: String, currentCount: Int) =
        onIntent(ProfileIntent.CommentSubmitted(postId, text, currentCount))
    fun sharePost(post: Post) = onIntent(ProfileIntent.ShareClicked(post))

    private fun changeTab(isMine: Boolean) {
        updateState { it.copy(isMine = isMine) }
    }

    private fun clearError() {
        updateState { it.copy(error = null) }
    }

    private fun updateState(update: (ProfileState) -> ProfileState) {
        _uiState.update(update)
    }

    private suspend fun handleError(error: Throwable) {
        val message = error.message ?: "Something went wrong"
        updateState { it.copy(error = message) }
        _sideEffects.send(ProfileSideEffect.ShowError(message))
    }
}

sealed interface ProfileIntent {
    data object CreatePostClicked : ProfileIntent
    data object EditProfileClicked : ProfileIntent
    data class TabChanged(val isMine: Boolean) : ProfileIntent
    data class SortChanged(val sort: Sort) : ProfileIntent
    data class DeletePostClicked(val postId: String) : ProfileIntent
    data class LikeClicked(val postId: String, val currentIsLiked: Boolean, val currentLikesCount: Int) : ProfileIntent
    data class CommentsIconClicked(val postId: String, val currentCommentsCount: Int) : ProfileIntent
    data object DismissCommentsSheet : ProfileIntent
    data class CommentSubmitted(val postId: String, val commentText: String, val currentCommentCount: Int) : ProfileIntent
    data class ShareClicked(val post: Post) : ProfileIntent
    data class ReportClicked(val postId: String, val reason: String?) : ProfileIntent
    data class PostClicked(val post: Post) : ProfileIntent
    data object DismissPost : ProfileIntent
    data object ErrorShown : ProfileIntent
    data object RetryProfileClicked : ProfileIntent
}

data class PostOverride(
    val isLiked: Boolean? = null,
    val likesCount: Int? = null,
    val commentsCount: Int? = null,
    val isReported: Boolean? = null,
    val commentsFlow: Flow<PagingData<Comment>>? = null
)

data class ProfileState(
    val profile: Profile? = null,
    val isProfileLoading: Boolean = true,
    val userPostsFlow: Flow<PagingData<Post>>? = null,
    val likedPostsFlow: Flow<PagingData<Post>>? = null,
    val isMine: Boolean = true,
    val sort: Sort = Sort.LATEST,
    val isLocalLoading: Boolean = true,
    val isNetworkLoading: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val profileError: String? = null,
    val postOverrides: Map<String, PostOverride> = emptyMap(),
    val showCommentsSheetForPostId: String? = null,
    val expandedPost: Post? = null
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
