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
     * Get paged posts for a given post level (network PagingSource via Pager).
     */
    fun getPagedPosts(postLevel: PostLevel, forceRefresh: Boolean = false): Flow<PagingData<Post>>

    /**
     * Observe active issues count for a given post level (cached locally).
     */
    fun observeActiveIssuesCount(postLevel: PostLevel): Flow<DataState<Int>>
}
