package org.example.project.core.data.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.example.project.core.model.home.Post
import org.example.project.core.model.profile.Profile
import org.example.project.core.utils.DataState

/**
 * Profile feature repository interface
 * Defines data operations for the profile feature with caching strategy
 */
interface ProfileRepository {

    /**
     * Get paged user posts (network PagingSource via Pager).
     */
    fun getPagedUserPosts(sort: String = "LATEST"): Flow<PagingData<Post>>

    /**
     * Get paged liked posts (network PagingSource via Pager).
     */
    fun getPagedLikedPosts(sort: String = "LATEST"): Flow<PagingData<Post>>

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
