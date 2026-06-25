package org.example.project.core.data.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.example.project.core.model.auth.UserLocation
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel

import org.example.project.core.utils.DataState

/**
 * Home feature repository interface
 * Defines data operations for the home/feed feature
 */
interface FeedRepository {
    /**
     * Get paged posts for a given post level (network PagingSource via Pager).
     */
    fun getPagedPosts(postLevel: PostLevel, userLocation: UserLocation = UserLocation(), forceRefresh: Boolean = false): Flow<PagingData<Post>>

    /**
     * Observe active issues count for a given post level (cached locally).
     */
    fun observeActiveIssuesCount(postLevel: PostLevel): Flow<Int>

    /**
     * Get paged search results for a given query and post level.
     */
    fun getPagedSearchPosts(query: String, postLevel: PostLevel): Flow<PagingData<Post>>

    fun observePosts(postLevel: PostLevel): Flow<List<Post>>

    suspend fun refreshPosts(
        postLevel: PostLevel,
        userLocation: UserLocation = UserLocation()
    ): DataState<List<Post>>

    suspend fun searchPosts(query: String, postLevel: PostLevel): DataState<List<Post>>

    suspend fun updateLikeStatus(postId: String, likesCount: Int, isLiked: Boolean)
    suspend fun updateReportStatus(postId: String, isReported: Boolean)
}
