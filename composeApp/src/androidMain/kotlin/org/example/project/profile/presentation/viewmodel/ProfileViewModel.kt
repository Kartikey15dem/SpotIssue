package org.example.project.profile.presentation.viewmodel

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
import org.example.project.home.domain.usecases.PostActionsUseCase
import org.example.project.profile.domain.repository.ProfileRepository
import org.example.project.home.domain.models.Post
import org.example.project.profile.data.local.mapper.Sort
import org.example.project.profile.domain.models.Profile


/**
 * ViewModel for Profile Screen with MVI pattern and caching strategy
 *
 * Caching Lifecycle:
 * 1. First time opening profile (ViewModel created):
 *    - Load from Room DB → Display instantly
 *    - If first load: Fetch from Supabase → Save to Room DB
 *
 * 2. Navigate away and back (ViewModel ALIVE):
 *    - ALWAYS reload from Room DB (data updated from other screens like HomeScreen)
 *    - Examples: User liked post in HomeScreen, created new post
 *    - NO Supabase call needed
 *
 * 3. App closed and reopened:
 *    - Load from Room DB instantly → Display
 *    - Fetch from Supabase → Update Room DB
 */
class ProfileViewModel(
    private val postActions: PostActionsUseCase,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    private val _sideEffects = MutableSharedFlow<ProfileSideEffect>()
    val sideEffects: SharedFlow<ProfileSideEffect> = _sideEffects.asSharedFlow()

    // In-memory sorted cache - for quick tab/sort switching
    // Reloaded from Room DB every time screen is visited
    private var cachedMyPostsLatest: List<Post> = emptyList()
    private var cachedMyPostsOldest: List<Post> = emptyList()
    private var cachedMyPostsPopular: List<Post> = emptyList()

    private var cachedLikedPostsLatest: List<Post> = emptyList()
    private var cachedLikedPostsOldest: List<Post> = emptyList()
    private var cachedLikedPostsPopular: List<Post> = emptyList()

    init {
        loadProfile()
    }

    /**
     * Call this when screen is resumed/visible
     * Reloads data from Room DB to catch updates from other screens
     */
    @Suppress("unused") // Will be called from ProfileScreen composable
    fun onScreenResumed() {
        viewModelScope.launch {
            loadFromRoomDB()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Step 1: ALWAYS load from Room DB first (instant, latest data)
            loadFromRoomDB()

            // Step 2: Fetch from Supabase only on first load
            if (profileRepository.isFirstLoad()) {
                fetchFromSupabase()
                profileRepository.markAsLoaded()
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Load from Room DB - source of truth during app session
     * Room DB is updated when user likes/creates/deletes posts in other screens
     */
    private suspend fun loadFromRoomDB() {
        // Load profile
        val cachedProfile = profileRepository.getProfile().getOrNull()
        if (cachedProfile != null) {
            _uiState.update { it.copy(profile = cachedProfile) }
        }

        // Load posts (sorted by Room DB)
        val myPosts = profileRepository.getUserPostsFromCache(Sort.LATEST)
        val likedPosts = profileRepository.getLikedPostsFromCache(Sort.LATEST)

        // Cache all sorted variants for quick tab/sort switching
        cacheAndSortPosts(myPosts, isMyPosts = true)
        cacheAndSortPosts(likedPosts, isMyPosts = false)
        updateDisplayedPosts()
    }

    /**
     * Fetch from Supabase and update Room DB cache
     */
    private fun fetchFromSupabase() {
        viewModelScope.launch {
            // Fetch profile
            profileRepository.fetchAndCacheProfile()
                .onSuccess {
                    // Room DB updated, reload
                    loadFromRoomDB()
                }
                .onFailure { error ->
                    handleError(error)
                }

            // Fetch user posts
            profileRepository.fetchAndCacheUserPosts()
                .onSuccess {
                    // Room DB updated, reload
                    loadFromRoomDB()
                }
                .onFailure { error ->
                    handleError(error)
                }

            // Fetch liked posts
            profileRepository.fetchAndCacheLikedPosts()
                .onSuccess {
                    // Room DB updated, reload
                    loadFromRoomDB()
                }
                .onFailure { error ->
                    handleError(error)
                }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Refresh -> refresh()
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
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            fetchFromSupabase()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    /**
     * Cache and pre-sort posts in all three orders
     * This is only called when posts are loaded/refreshed from API
     */
    private fun cacheAndSortPosts(posts: List<Post>, isMyPosts: Boolean) {
        if (isMyPosts) {
            cachedMyPostsLatest = sortByLatest(posts)
            cachedMyPostsOldest = sortByOldest(posts)
            cachedMyPostsPopular = sortByPopular(posts)
        } else {
            cachedLikedPostsLatest = sortByLatest(posts)
            cachedLikedPostsOldest = sortByOldest(posts)
            cachedLikedPostsPopular = sortByPopular(posts)
        }
    }

    /**
     * Update displayed posts based on current tab and sort selection
     * Uses cached sorted lists - no API call needed
     */
    private fun updateDisplayedPosts() {
        val currentState = _uiState.value
        val posts = when {
            currentState.isMine && currentState.sort == Sort.LATEST -> cachedMyPostsLatest
            currentState.isMine && currentState.sort == Sort.OLDEST -> cachedMyPostsOldest
            currentState.isMine && currentState.sort == Sort.POPULAR -> cachedMyPostsPopular
            !currentState.isMine && currentState.sort == Sort.LATEST -> cachedLikedPostsLatest
            !currentState.isMine && currentState.sort == Sort.OLDEST -> cachedLikedPostsOldest
            !currentState.isMine && currentState.sort == Sort.POPULAR -> cachedLikedPostsPopular
            else -> emptyList()
        }

        _uiState.update { it.copy(posts = posts) }
    }

    private fun changeTab(isMine: Boolean) {
        _uiState.update { it.copy(isMine = isMine) }
        updateDisplayedPosts()
    }

    private fun changeSort(sort: Sort) {
        _uiState.update { it.copy(sort = sort) }
        updateDisplayedPosts()
    }

    private fun sortByLatest(posts: List<Post>): List<Post> {
        // Sort by timeAgo (assuming "1 hour ago" format)
        // This is a simplified implementation - in real app, use timestamps
        return posts.sortedBy { post ->
            when {
                post.timeAgo.contains("hour") -> post.timeAgo.split(" ")[0].toIntOrNull() ?: 0
                post.timeAgo.contains("day") -> (post.timeAgo.split(" ")[0].toIntOrNull() ?: 0) * 24
                post.timeAgo.contains("week") -> (post.timeAgo.split(" ")[0].toIntOrNull() ?: 0) * 168
                else -> Int.MAX_VALUE
            }
        }
    }

    private fun sortByOldest(posts: List<Post>): List<Post> {
        return sortByLatest(posts).reversed()
    }

    private fun sortByPopular(posts: List<Post>): List<Post> {
        return posts.sortedByDescending { it.likes + it.comments }
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

    private fun deletePost(postId: String) {
        viewModelScope.launch {
            postActions.delete(postId)
                .onSuccess {
                    // Remove from cached lists
                    cachedMyPostsLatest = cachedMyPostsLatest.filter { it.id != postId }
                    cachedMyPostsOldest = cachedMyPostsOldest.filter { it.id != postId }
                    cachedMyPostsPopular = cachedMyPostsPopular.filter { it.id != postId }

                    updateDisplayedPosts()
                    _sideEffects.emit(ProfileSideEffect.ShowSnackbar("Post deleted successfully"))
                }
                .onFailure { error ->
                    handleError(error)
                }
        }
    }

    private fun likePost(postId: String) {
        viewModelScope.launch {
            postActions.like(postId)
                .onFailure { handleError(it) }
        }
    }

    private fun sharePost(postId: String) {
        viewModelScope.launch {
            val post = _uiState.value.posts.find { it.id == postId }
            if (post != null) {
                val shareText = "${post.userName}: ${post.postText}\n\nShared from IssueSpot"
                _sideEffects.emit(ProfileSideEffect.SharePost(shareText))
            }
        }
    }

    private fun reportPost(postId: String) {
        viewModelScope.launch {
            postActions.report(postId)
                .onSuccess {
                    _sideEffects.emit(ProfileSideEffect.ShowSnackbar("Post reported successfully"))
                }
                .onFailure { handleError(it) }
        }
    }

    @Suppress("unused") // Will be used for media viewer feature
    private fun openMediaViewer(postId: String) {
        viewModelScope.launch {
            _sideEffects.emit(ProfileSideEffect.OpenMediaViewer(postId))
        }
    }

    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun handleError(error: Throwable) {
        val message = error.message ?: "Something went wrong"
        _uiState.update { it.copy(error = message) }
        _sideEffects.emit(ProfileSideEffect.ShowError(message))
    }
}

// MVI Contract

sealed interface ProfileIntent {
    data object Refresh : ProfileIntent
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
}

data class ProfileState(
    val profile: Profile = Profile(
        imageUrl = "",
        name = "",
        location = "",
        totalPosts = 0,
        acks = 0,
        postByArea = listOf(0, 0, 0, 0),
        myPosts = emptyList(),
        ackPosts = emptyList()
    ),
    val posts: List<Post> = emptyList(),
    val isMine: Boolean = true,
    val sort: Sort = Sort.LATEST,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

sealed interface ProfileSideEffect {
    data object NavigateToCreatePost : ProfileSideEffect
    data object NavigateToEditProfile : ProfileSideEffect
    data class NavigateToPost(val postId: String) : ProfileSideEffect
    data class ShowError(val message: String) : ProfileSideEffect
    data class ShowSnackbar(val message: String) : ProfileSideEffect
    data class SharePost(val text: String) : ProfileSideEffect
    data class OpenMediaViewer(val postId: String) : ProfileSideEffect
}



