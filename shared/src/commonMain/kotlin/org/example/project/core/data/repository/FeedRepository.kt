package org.example.project.core.data.repository

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.example.project.core.model.auth.UserLocation
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.presentation.FeedState
import org.example.project.core.presentation.FeedRefreshReason
import org.example.project.core.utils.DataState

/**
 * Home feature repository interface
 * Defines data operations for the home/feed feature
 */
interface FeedRepository {
    val feedState: StateFlow<FeedState>
    val searchState: StateFlow<FeedState>

    fun start(postLevel: PostLevel, userLocation: UserLocation)
    fun stop()
    
    fun refresh(reason: FeedRefreshReason)
    fun retry()
    fun loadMore()

    fun startSearch(query: String, postLevel: PostLevel)
    fun clearSearch()
    fun refreshSearch()
    fun retrySearch()
    fun loadMoreSearch()

    fun observeActiveIssuesCount(postLevel: PostLevel): Flow<Int>
    fun getPagedSearchPosts(query: String, postLevel: PostLevel): Flow<PagingData<Post>>
    fun observePosts(postLevel: PostLevel): Flow<List<Post>>
    suspend fun searchPosts(query: String, postLevel: PostLevel): DataState<List<Post>>

    suspend fun updateLikeStatus(postId: String, likesCount: Int, isLiked: Boolean)
    suspend fun updateReportStatus(postId: String, isReported: Boolean)
}
