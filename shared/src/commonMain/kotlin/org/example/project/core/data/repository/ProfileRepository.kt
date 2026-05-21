package org.example.project.core.data.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.example.project.core.utils.DataState

/**
 * Profile feature repository interface
 * Defines data operations for the profile feature with caching strategy
 */
interface ProfileRepository {

    /**
     * Get paged user posts (Room-backed with RemoteMediator)
     */
    fun getPagedUserPosts(userId: String? = null): Flow<PagingData<Post>>

    /**
     * Get paged liked posts (Room-backed with RemoteMediator)
     */
    fun getPagedLikedPosts(userId: String? = null): Flow<PagingData<Post>>

    /**
     * Observe user profile (Room-backed)
     */
    fun observeProfile(userId: String? = null): Flow<DataState<Profile>>

    /**
     * Refresh profile data from remote
     */
    suspend fun refreshProfile(userId: String? = null): DataState<Unit>

    /**
     * Update user profile
     */
    suspend fun updateProfile(profile: Profile): DataState<Unit>
}