package org.example.project.core.data.repositoryImp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.example.project.core.data.local.FeedLocalDataSource
import org.example.project.core.data.mappers.toPost
import org.example.project.core.data.repository.FeedRepository
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.model.auth.UserLocation
import org.example.project.core.model.home.PostLevel
import org.example.project.core.network.services.HomeService
import org.example.project.core.paging.OfflinePager
import org.example.project.core.paging.PagingResult
import org.example.project.core.presentation.FeedRefreshReason
import org.example.project.core.presentation.FeedState
import org.example.project.core.utils.NetworkMonitor

/**
 * FeedRepositoryImpl
 *
 * This repository handles fetching and caching the main feeds (Locality, State, National)
 * and Search results.
 *
 * ARCHITECTURE NOTE:
 * Instead of manually managing Mutex locks, `PagingState`, and `WindowEngine` caches here
 * (which used to take 700 lines of code), we delegate all pagination logic to `OfflinePager`.
 * - `feedPager` handles offline-first caching via Room Database.
 * - `searchPager` handles memory-only pagination for search results.
 */
class FeedRepositoryImpl(
    private val homeService: HomeService,
    private val database: IssueSpotDatabase,
    private val localDataSource: FeedLocalDataSource,
    private val networkMonitor: NetworkMonitor,
) : FeedRepository {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentLocation: UserLocation? = null
    private var networkMonitorJob: Job? = null
    private var isOnline = true

    // ---------------------------------------------------------
    // THE FEED PAGER (Uses the Generic OfflinePager)
    // ---------------------------------------------------------

    // We replaced 400 lines of complex Mutex and caching logic with this one engine.
    // It automatically handles exponential backoff, preventing race conditions when
    // rapidly swapping tabs, and pushing data to the UI without stuttering.
    private val feedPager =
        OfflinePager<PostLevel>(
            scope = scope,
            fetchFromNetwork = { level, page ->
                val location = currentLocation ?: UserLocation()
                val response =
                    homeService.getPosts(
                        level = level.name,
                        locality = location.locality,
                        district = location.district,
                        state = location.state,
                        country = location.country,
                        lat = location.latitude,
                        lon = location.longitude,
                        page = page,
                        limit = 20,
                    )
                // Cache active issues if present
                response.activeIssuesCount?.let { count ->
                    localDataSource.cacheActiveIssues(level, count)
                }
                val posts = response.items.map { it.toPost() }
                PagingResult(posts, response.nextKey, response.nextKey != null && posts.isNotEmpty())
            },
            saveToDatabase = { level, posts, isRefresh ->
                if (isRefresh) {
                    localDataSource.replacePosts(level, posts)
                } else {
                    localDataSource.appendPosts(level, posts)
                }
            },
            observeDatabase = { level, anchorCachedAt, anchorId, limit ->
                if (anchorCachedAt != null && anchorId != null) {
                    localDataSource.observePostsAfterAnchor(level, anchorCachedAt, anchorId, limit)
                } else {
                    localDataSource.observeNewestPosts(level, limit)
                }
            },
            getCachedCount = { level -> localDataSource.getCachedPostCount(level) },
            isOnlineProvider = { isOnline },
        )

    override val feedState: StateFlow<FeedState> = feedPager.uiState

    // ---------------------------------------------------------
    // THE SEARCH PAGER (Uses the Generic OfflinePager in Memory Mode)
    // ---------------------------------------------------------

    private data class SearchKey(
        val query: String,
        val level: PostLevel,
    )

    // Search doesn't use the database, so we set isInMemoryOnly = true
    private val searchPager =
        OfflinePager<SearchKey>(
            scope = scope,
            fetchFromNetwork = { key, page ->
                val response =
                    homeService.searchPosts(
                        query = key.query,
                        level = key.level.name,
                        page = page,
                        limit = 20,
                    )
                val posts = response.items.map { it.toPost() }
                PagingResult(posts, response.nextKey, response.nextKey != null && posts.isNotEmpty())
            },
            saveToDatabase = { _, _, _ -> }, // No-op for memory only
            observeDatabase = { _, _, _, _ -> kotlinx.coroutines.flow.flowOf(emptyList()) }, // No-op
            getCachedCount = { 0 },
            isOnlineProvider = { isOnline },
            isInMemoryOnly = true,
        )

    override val searchState: StateFlow<FeedState> = searchPager.uiState

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        networkMonitorJob =
            scope.launch {
                networkMonitor.isOnline.collect { online ->
                    isOnline = online
                    feedPager.updateOnlineStatus(online)
                    searchPager.updateOnlineStatus(online)

                    // When we come back online, silently attempt to fetch missing data
                    if (online) {
                        feedPager.refresh(FeedRefreshReason.NETWORK_RESTORED)
                    }
                }
            }
    }

    // ---------------------------------------------------------
    // FEED PUBLIC API
    // ---------------------------------------------------------

    override fun initializeFeedForLevel(
        postLevel: PostLevel,
        userLocation: UserLocation,
    ) {
        currentLocation = userLocation
        feedPager.start(postLevel)
    }

    override fun stopFeedObservation() {
        feedPager.stop()
    }

    override fun refresh(reason: FeedRefreshReason) = feedPager.refresh(reason)

    override fun retry() = feedPager.retry()

    override fun loadMore() = feedPager.loadMore()

    // ---------------------------------------------------------
    // SEARCH PUBLIC API
    // ---------------------------------------------------------

    override fun startSearch(
        query: String,
        postLevel: PostLevel,
    ) {
        val normalized = query.trim()
        if (normalized.isBlank()) {
            clearSearch()
            return
        }
        searchPager.start(SearchKey(normalized, postLevel))
    }

    override fun clearSearch() = searchPager.clear()

    override fun refreshSearch() = searchPager.refresh(FeedRefreshReason.PULL_TO_REFRESH)

    override fun retrySearch() = searchPager.retry()

    override fun loadMoreSearch() = searchPager.loadMore()

    // ---------------------------------------------------------
    // OTHER
    // ---------------------------------------------------------

    override fun observeActiveIssuesCount(postLevel: PostLevel): Flow<Int> = localDataSource.observeActiveIssues(postLevel).map { it ?: 0 }
}
