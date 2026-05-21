package org.example.project.core.data.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import org.example.project.home.domain.models.Post
import org.example.project.home.domain.models.PostLevel

import org.example.project.core.utils.DataState

/**
 * Home feature repository interface
 * Defines data operations for the home/feed feature
 */
interface FeedRepository {
    /**
     * Get paged posts for a given post level (Room-backed with RemoteMediator)
     */
    fun getPagedPosts(postLevel: PostLevel): Flow<PagingData<Post>>

    /**
     * Observe active issues count for a given post level (Room-backed)
     */
    fun observeActiveIssuesCount(postLevel: PostLevel): Flow<DataState<Int>>

    /**
     * Force refresh posts from API
     */
    suspend fun refreshPosts(postLevel: PostLevel): DataState<Unit>

    /**
     * Force refresh active issues count from API
     */
    suspend fun refreshActiveIssuesCount(postLevel: PostLevel): DataState<Unit>
}