package org.example.project.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.example.project.core.model.home.Post
import org.example.project.core.presentation.FeedRefreshReason
import org.example.project.core.presentation.FeedState
import org.example.project.core.model.profile.Profile
import org.example.project.core.utils.DataState

/**
 * Profile feature repository interface
 * Defines data operations for the profile feature with caching strategy
 */
interface ProfileRepository {
    val profilePostsState: StateFlow<FeedState>

    fun startProfilePosts(isMine: Boolean, sort: String = "LATEST")

    fun stopProfilePosts()

    fun refreshProfilePosts(reason: FeedRefreshReason)

    fun retryProfilePosts()

    fun loadMoreProfilePosts()





    suspend fun refreshUserPosts(sort: String = "LATEST"): DataState<List<Post>>

    suspend fun refreshLikedPosts(sort: String = "LATEST"): DataState<List<Post>>

    /**
     * Observe user profile (Room-backed)
     */
    fun observeProfile(): Flow<DataState<Profile?>>

    /**
     * Refresh profile data from remote
     */
    suspend fun refreshProfile(): DataState<Unit>

    /**
     * Update user profile
     */
    suspend fun updateProfile(profile: Profile, imagePath: String? = null): DataState<Unit>

    /**
     * Request email change (send OTP to new email)
     */
    suspend fun requestEmailChange(newEmail: String): DataState<Unit>

    /**
     * Verify email change (verify OTP and update email)
     */
    suspend fun verifyEmailChange(newEmail: String, code: String): DataState<Unit>

    suspend fun logOut()
}
