package org.example.project.core.data.repositoryImp

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.core.data.local.FeedLocalDataSource
import org.example.project.core.data.repository.FeedRepository
import org.example.project.core.database.IssueSpotDatabase
import org.example.project.core.data.mappers.toPost
import org.example.project.core.model.auth.UserLocation
import org.example.project.core.model.home.Post
import org.example.project.core.model.home.PostLevel
import org.example.project.core.presentation.FeedState
import org.example.project.core.presentation.FeedError
import org.example.project.core.presentation.FeedRefreshReason
import org.example.project.core.presentationcache.PresentationCache
import org.example.project.core.utils.DataState
import org.example.project.core.utils.NetworkMonitor
import org.example.project.core.utils.safeApiCall
import org.example.project.core.window.WindowEngine
import org.example.project.core.window.WindowMode
import org.example.project.core.window.WindowState
import org.example.project.core.window.FeedConfig
import org.example.project.core.network.services.HomeService
import org.example.project.core.network.dto.PagedResponse
import org.example.project.core.network.dto.PostWithProfileDto

class FeedRepositoryImpl(
    private val homeService: HomeService,
    private val database: IssueSpotDatabase,
    private val localDataSource: FeedLocalDataSource,
    private val networkMonitor: NetworkMonitor,
) : FeedRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    
    // ---------------------------------------------------------
    // COMPLEXITY EXPLANATION: Why all these maps and mutexes?
    // ---------------------------------------------------------
    // Pagination isn't just about loading a list. If a user rapidly swipes through tabs
    // (Locality -> State -> National), we need to ensure network responses for 'Locality'
    // don't accidentally get appended to the 'National' feed. 
    //
    // - presentationCache: Caches UI models to prevent unnecessary heavy UI recompositions.
    // - pagingMutex: Prevents race conditions. If `refresh` and `loadMore` are called exactly
    //   at the same millisecond, they could corrupt the database. Mutex locks the thread so they queue up safely.
    // - pagingStatesByLevel: Saves the user's scroll depth. If they scroll down in 'State', go to 'National', 
    //   and come back to 'State', they shouldn't lose their place!
    private val presentationCache = PresentationCache<Post, String> { it.id }
    private val pagingMutex = Mutex()
    private val pagingStatesByLevel = mutableMapOf<PostLevel, PagingState>()
    private val windowEnginesByLevel = mutableMapOf<PostLevel, WindowEngine<Post>>()
    private val automaticRefreshAttemptedLevels = mutableSetOf<PostLevel>()

    // State class to keep track of pagination details for a specific post level.
    // nextPage: The index/key for the next page to fetch from the API.
    // hasMore: Boolean flag indicating if there are more items to be fetched.
    // generation: A counter to handle concurrency and discard outdated network responses.
    private data class PagingState(
        val nextPage: Int = 0,
        val hasMore: Boolean = true,
        val generation: Long = 0L,
        val lastFailedAction: RetryAction? = null,
        val lastFailedPage: Int = 0
    )

    private enum class RetryAction {
        REFRESH, LOAD_MORE
    }

    private var pagingState = PagingState()
    private var searchPagingState = PagingState()

    private val _feedState = MutableStateFlow(FeedState())
    override val feedState: StateFlow<FeedState> = _feedState.asStateFlow()
    private val _searchState = MutableStateFlow(FeedState())
    override val searchState: StateFlow<FeedState> = _searchState.asStateFlow()

    private val levelFlow = MutableStateFlow<PostLevel?>(null)
    private data class SearchKey(val query: String, val level: PostLevel)
    private var activeSearchKey: SearchKey? = null
    private val windowState = MutableStateFlow(WindowEngine<Post>().getState())
    private var currentLocation: UserLocation? = null

    private var roomJob: Job? = null
    private var networkJob: Job? = null
    private var searchJob: Job? = null
    private var networkMonitorJob: Job? = null
    private var isOnline = true

    private fun windowEngineFor(postLevel: PostLevel): WindowEngine<Post> {
        return windowEnginesByLevel.getOrPut(postLevel) { WindowEngine() }
    }

    private fun saveCurrentPagingState() {
        levelFlow.value?.let { level ->
            pagingStatesByLevel[level] = pagingState
        }
    }
    
    // ---------------------------------------------------------
    // OBSERVERS
    // ---------------------------------------------------------

    init {
        observeNetwork()
    }

    // Continuously monitors device network status.
    // If the device comes back online, we trigger a background refresh to catch up
    // on missed posts, UNLESS we've already done so for the current level.
    private fun observeNetwork() {
        networkMonitorJob = scope.launch {
            networkMonitor.isOnline.collect { online ->
                val wasOffline = !isOnline
                isOnline = online
                // Update the UI state so it can show a "No Internet" banner if needed.
                _feedState.update { it.copy(isOffline = !online) }
                
                val currentLevel = levelFlow.value
                // If we just regained connection, silently fetch the latest items.
                if (online && wasOffline && currentLevel != null && currentLevel !in automaticRefreshAttemptedLevels) {
                    refresh(FeedRefreshReason.NETWORK_RESTORED)
                }
            }
        }
    }

    // ---------------------------------------------------------
    // CORE INITIALIZATION
    // ---------------------------------------------------------
    
    // We renamed this from `start` to `initializeFeedForLevel` because `start` is too generic.
    // This is called by the ViewModel when the user changes the location or post level (e.g. from Locality -> District).
    // It resets pagination state, clears the presentation cache, and starts observing the local database for the new level.
    override fun initializeFeedForLevel(postLevel: PostLevel, userLocation: UserLocation) {
        val levelChanged = levelFlow.value != postLevel
        currentLocation = userLocation
        
        if (levelChanged) {
            scope.launch {
                val cachedCount = localDataSource.getCachedPostCount(postLevel)
                var shouldRefreshLevel = false
                pagingMutex.withLock {
                    networkJob?.cancel()
                    saveCurrentPagingState()
                    val restoredState = pagingStatesByLevel[postLevel] ?: PagingState()
                    pagingState = restoredState.copy(
                        generation = restoredState.generation + 1,
                        lastFailedAction = null,
                    )
                    presentationCache.clear()
                    windowState.value = windowEngineFor(postLevel).getState()
                    levelFlow.value = postLevel
                    shouldRefreshLevel = postLevel !in automaticRefreshAttemptedLevels
                    
                    _feedState.update {
                        it.copy(
                            posts = emptyList(),
                            isLoading = shouldRefreshLevel && cachedCount == 0,
                            isRefreshing = false,
                            isAppending = false,
                            isBackgroundRefreshing = false,
                            isRetrying = false,
                            hasMore = pagingState.hasMore,
                            error = null,
                            appendError = null,
                            isOffline = !isOnline
                        )
                    }
                }
                restartRoomObservation()
                if (shouldRefreshLevel) {
                    refresh(FeedRefreshReason.LEVEL_CHANGED)
                } else if (cachedCount == 0) {
                    _feedState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
    
    // Renamed from `stop` to `stopFeedObservation`. Used to clean up jobs if the repository lifecycle ends.
    override fun stopFeedObservation() {
        networkJob?.cancel()
        roomJob?.cancel()
    }

    // ---------------------------------------------------------
    // SEARCH PAGINATION
    // ---------------------------------------------------------

    // Initiates a new search. If the query is identical to the active one, it does nothing.
    // Otherwise, it resets the search state to page 0 and fetches fresh results.
    override fun startSearch(query: String, postLevel: PostLevel) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) {
            clearSearch()
            return
        }

        val key = SearchKey(normalizedQuery, postLevel)
        if (activeSearchKey == key) return

        activeSearchKey = key
        searchJob?.cancel() // Cancel any ongoing, outdated search requests
        
        // Bump generation to discard outdated network responses.
        searchPagingState = PagingState(generation = searchPagingState.generation + 1)
        _searchState.value = FeedState(isLoading = true, hasMore = true, isOffline = !isOnline)
        refreshSearch() // Fetch page 0
    }

    // Clears the search state entirely. Called when user clears the search bar.
    override fun clearSearch() {
        activeSearchKey = null
        searchJob?.cancel()
        searchPagingState = PagingState(generation = searchPagingState.generation + 1)
        _searchState.value = FeedState()
    }

    override fun refreshSearch() {
        val key = activeSearchKey ?: return
        if (!isOnline) {
            _searchState.update { it.copy(isLoading = false, isRefreshing = false, isOffline = true) }
            return
        }

        searchJob?.cancel()
        searchJob = scope.launch {
            val requestGeneration = pagingMutex.withLock {
                searchPagingState = searchPagingState.copy(
                    nextPage = 0,
                    hasMore = true,
                    generation = searchPagingState.generation + 1,
                    lastFailedAction = null,
                    lastFailedPage = 0
                )
                searchPagingState.generation
            }

            val hasItems = _searchState.value.posts.isNotEmpty()
            _searchState.update {
                it.copy(
                    isLoading = !hasItems,
                    isRefreshing = hasItems,
                    isAppending = false,
                    isRetrying = false,
                    hasMore = true,
                    error = null,
                    appendError = null,
                    isOffline = false
                )
            }

            try {
                val response = fetchSearchPage(key, 0)
                val posts = response.items.map { it.toPost() }
                val hasMore = response.nextKey != null && posts.isNotEmpty()
                pagingMutex.withLock {
                    if (requestGeneration != searchPagingState.generation) return@launch
                    searchPagingState = searchPagingState.copy(
                        nextPage = response.nextKey ?: 0,
                        hasMore = hasMore
                    )
                }
                _searchState.update {
                    it.copy(
                        posts = posts,
                        isLoading = false,
                        isRefreshing = false,
                        isAppending = false,
                        isRetrying = false,
                        hasMore = hasMore,
                        error = null,
                        appendError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = mapToFeedError(e)
                pagingMutex.withLock {
                    if (requestGeneration == searchPagingState.generation) {
                        searchPagingState = searchPagingState.copy(lastFailedAction = RetryAction.REFRESH, lastFailedPage = 0)
                    }
                }
                _searchState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isRetrying = false,
                        error = error
                    )
                }
            }
        }
    }

    override fun loadMoreSearch() {
        val key = activeSearchKey ?: return
        if (!isOnline) return

        searchJob = scope.launch {
            val (shouldFetch, requestGeneration, pageToLoad) = pagingMutex.withLock {
                val state = _searchState.value
                if (state.isLoading || state.isRefreshing || state.isAppending || state.isRetrying || !searchPagingState.hasMore) {
                    return@withLock Triple(false, 0L, 0)
                }
                _searchState.update { it.copy(isAppending = true, appendError = null) }
                Triple(true, searchPagingState.generation, searchPagingState.nextPage)
            }

            if (!shouldFetch) return@launch

            try {
                val response = fetchSearchPage(key, pageToLoad)
                val posts = response.items.map { it.toPost() }
                val hasMore = response.nextKey != null && posts.isNotEmpty()
                pagingMutex.withLock {
                    if (requestGeneration != searchPagingState.generation) return@launch
                    searchPagingState = searchPagingState.copy(
                        nextPage = response.nextKey ?: pageToLoad,
                        hasMore = hasMore
                    )
                }
                _searchState.update {
                    it.copy(
                        posts = it.posts + posts,
                        isAppending = false,
                        hasMore = hasMore,
                        appendError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = mapToFeedError(e)
                pagingMutex.withLock {
                    if (requestGeneration == searchPagingState.generation) {
                        searchPagingState = searchPagingState.copy(lastFailedAction = RetryAction.LOAD_MORE, lastFailedPage = pageToLoad)
                    }
                }
                _searchState.update {
                    it.copy(
                        isAppending = false,
                        hasMore = searchPagingState.hasMore,
                        appendError = error
                    )
                }
            }
        }
    }

    override fun retrySearch() {
        when (searchPagingState.lastFailedAction) {
            RetryAction.LOAD_MORE -> loadMoreSearch()
            else -> refreshSearch()
        }
    }

    private suspend fun fetchSearchPage(key: SearchKey, page: Int): PagedResponse<PostWithProfileDto> {
        return homeService.searchPosts(
            query = key.query,
            level = key.level.name,
            page = page,
            limit = 20
        )
    }

    // ---------------------------------------------------------
    // ROOM DATABASE OBSERVATION
    // ---------------------------------------------------------

    // Creates a reactive pipeline from the Database to the UI State.
    // Instead of manually updating the UI after a network call, we update the DB.
    // Room automatically emits the new data through `observeNewestPosts`.
    private fun restartRoomObservation() {
        roomJob?.cancel()
        roomJob = scope.launch {
            combine(levelFlow, windowState) { level, window -> Pair(level, window) }
                .flatMapLatest { (level, window) ->
                    if (level == null) return@flatMapLatest kotlinx.coroutines.flow.flowOf(emptyList())
                    if (window.mode == WindowMode.SLIDING) {
                        localDataSource.observePostsAfterAnchor(level, window.anchor!!.createdAt, window.anchor!!.id, window.limit)
                    } else {
                        localDataSource.observeNewestPosts(level, window.limit)
                    }
                }
                .collect { roomPosts ->
                    // Update our in-memory cache to prevent UI stutters on list redraws.
                    presentationCache.update(roomPosts)
                    _feedState.update { it.copy(posts = presentationCache.items.toList()) }
                }
        }
    }

    // ---------------------------------------------------------
    // NETWORK UTILS
    // ---------------------------------------------------------

    // A robust fetching mechanism with exponential backoff.
    // If the server flakes or the user has a spotty connection (Timeout/Network errors),
    // we wait 1s, then 2s, then 4s, up to 4 attempts before finally failing.
    private suspend fun fetchPageWithRetry(page: Int): PagedResponse<PostWithProfileDto> {
        var attempt = 1
        var delayMs = 1000L
        while (true) {
            try {
                val level = levelFlow.value ?: throw IllegalStateException("No active level")
                val location = currentLocation ?: UserLocation()
                return homeService.getPosts(
                    level = level.name,
                    locality = location.locality,
                    district = location.district,
                    state = location.state,
                    country = location.country,
                    lat = location.latitude,
                    lon = location.longitude,
                    page = page,
                    limit = 20
                )
            } catch (e: CancellationException) {
                // Never swallow CancellationException, it breaks coroutines
                throw e
            } catch (e: Exception) {
                val error = mapToFeedError(e)
                val isRetryable = error is FeedError.Server || error is FeedError.Network || error is FeedError.Timeout
                if (!isRetryable || attempt >= 4) {
                    throw e // Give up
                }
                delay(delayMs)
                attempt++
                delayMs *= 2 // Exponential backoff
            }
        }
    }

    private fun mapToFeedError(e: Throwable): FeedError {
        val msg = e.message ?: ""
        return when {
            msg.contains("401") -> FeedError.Authentication()
            msg.contains("50") -> FeedError.Server()
            msg.contains("Timeout") -> FeedError.Timeout()
            msg.contains("resolve host") || msg.contains("Failed to connect") -> FeedError.Server("An error occurred")
            msg.contains("Serialization") || msg.contains("JSON") -> FeedError.Parsing()
            e is io.ktor.utils.io.errors.IOException -> FeedError.Network()
            else -> FeedError.Unknown(msg)
        }
    }

    override fun refresh(reason: FeedRefreshReason) {
        if (!isOnline) {
            return
        }

        val activeLevel = levelFlow.value
        if ((reason == FeedRefreshReason.LEVEL_CHANGED || reason == FeedRefreshReason.NETWORK_RESTORED) && activeLevel != null) {
            if (!automaticRefreshAttemptedLevels.add(activeLevel)) {
                return
            }
        }

        networkJob?.cancel()
        networkJob = scope.launch {
            val count = levelFlow.value?.let { localDataSource.getCachedPostCount(it) } ?: 0
            val isBackground = (reason == FeedRefreshReason.APP_RESUMED || reason == FeedRefreshReason.NETWORK_RESTORED) && count > 0

            // Create a new paging request generation to invalidate any ongoing requests.
            // Reset nextPage to 0 for a fresh start.
            val requestGeneration = pagingMutex.withLock {
                pagingState = pagingState.copy(
                    nextPage = 0,
                    generation = pagingState.generation + 1,
                    lastFailedAction = null,
                    hasMore = true
                )
                levelFlow.value?.let { level ->
                    windowState.value = windowEngineFor(level).reset()
                }
                pagingState.generation
            }

            _feedState.update {
                when {
                    count == 0 -> it.copy(
                        isLoading = true,
                        isRefreshing = false,
                        isAppending = false,
                        isBackgroundRefreshing = false,
                        isRetrying = false,
                        hasMore = true,
                        error = null,
                        appendError = null
                    )
                    isBackground -> it.copy(
                        isLoading = false,
                        isBackgroundRefreshing = true,
                        isAppending = false,
                        hasMore = true,
                        error = null,
                        appendError = null
                    )
                    else -> it.copy(
                        isLoading = false,
                        isRefreshing = true,
                        isAppending = false,
                        hasMore = true,
                        error = null,
                        appendError = null
                    )
                }
            }

            try {
                val response = fetchPageWithRetry(0)
                var hasMoreAfterRefresh = true
                
                pagingMutex.withLock {
                    // Check if the generation matches to avoid processing stale data.
                    if (requestGeneration != pagingState.generation) return@launch
                    val level = levelFlow.value ?: return@launch
                    val posts = response.items.map { it.toPost() }
                    // Store the newly fetched posts in the local database (Source of Truth).
                    localDataSource.replacePosts(level, posts)
                    response.activeIssuesCount?.let { count ->
                        localDataSource.cacheActiveIssues(level, count)
                    }
                    
                    // Update paging state with the next page index and hasMore flag based on API response.
                    pagingState = pagingState.copy(
                        nextPage = 1,
                        hasMore = response.nextKey != null && posts.isNotEmpty()
                    )
                    hasMoreAfterRefresh = pagingState.hasMore
                    saveCurrentPagingState()
                }
                _feedState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isAppending = false,
                        isBackgroundRefreshing = false,
                        isRetrying = false,
                        hasMore = hasMoreAfterRefresh,
                        error = null,
                        appendError = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = mapToFeedError(e)
                pagingMutex.withLock {
                    if (requestGeneration == pagingState.generation) {
                        pagingState = pagingState.copy(lastFailedAction = RetryAction.REFRESH, lastFailedPage = 0)
                        saveCurrentPagingState()
                    }
                }
                _feedState.update { 
                    it.copy(
                        isLoading = false, 
                        isRefreshing = false, 
                        isAppending = false,
                        isBackgroundRefreshing = false,
                        isRetrying = false,
                        hasMore = pagingState.hasMore,
                        error = if (isBackground) null else error
                    ) 
                }
            }
        }
    }

    override fun loadMore() {
        if (!isOnline) return
        
        scope.launch {
            // Determine if we should fetch more items based on current state and hasMore flag.
            val (shouldFetch, requestGeneration, pageToLoad) = pagingMutex.withLock {
                if (_feedState.value.isLoading || _feedState.value.isRefreshing || _feedState.value.isAppending || _feedState.value.isRetrying || _feedState.value.isBackgroundRefreshing || !pagingState.hasMore) {
                    return@withLock Triple(false, 0L, 0)
                }
                
                // Calculate window expansion to load more items from the local DB.
                val currentState = windowState.value
                val items = presentationCache.items
                val anchor = if (items.size > FeedConfig.LOAD_MORE_THRESHOLD) items[items.size - FeedConfig.LOAD_MORE_THRESHOLD] else items.lastOrNull()
                val activeLevel = levelFlow.value ?: return@withLock Triple(false, 0L, 0)
                val nextState = windowEngineFor(activeLevel).expand(anchor)
                
                if (nextState != currentState) {
                    windowState.value = nextState
                    // Mark state as appending (loading more).
                    _feedState.update { it.copy(isAppending = true, appendError = null) }
                    Triple(true, pagingState.generation, pagingState.nextPage)
                } else {
                    Triple(false, 0L, 0)
                }
            }

            if (!shouldFetch) return@launch
            
            try {
                val response = fetchPageWithRetry(pageToLoad)
                var hasMoreAfterAppend = true
                
                pagingMutex.withLock {
                    // Append fetched data to the local database.
                    if (requestGeneration != pagingState.generation) return@launch
                    val level = levelFlow.value ?: return@launch
                    val posts = response.items.map { it.toPost() }
                    localDataSource.appendPosts(level, posts)
                    response.activeIssuesCount?.let { count ->
                        localDataSource.cacheActiveIssues(level, count)
                    }
                    
                    // Increment the nextPage index and update hasMore flag.
                    pagingState = pagingState.copy(
                        nextPage = pageToLoad + 1,
                        hasMore = response.nextKey != null && posts.isNotEmpty()
                    )
                    hasMoreAfterAppend = pagingState.hasMore
                    saveCurrentPagingState()
                    _feedState.update {
                        it.copy(
                            isAppending = false,
                            hasMore = hasMoreAfterAppend,
                            appendError = null
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                pagingMutex.withLock {
                    if (requestGeneration == pagingState.generation) {
                        pagingState = pagingState.copy(lastFailedAction = RetryAction.LOAD_MORE, lastFailedPage = pageToLoad)
                        saveCurrentPagingState()
                    }
                }
                _feedState.update {
                    it.copy(
                        isAppending = false,
                        hasMore = pagingState.hasMore,
                        appendError = mapToFeedError(e)
                    )
                }
            }
        }
    }

    override fun retry() {
        if (!isOnline) return
        
        scope.launch {
            val (actionToRetry, requestGeneration, pageToLoad) = pagingMutex.withLock { 
                Triple(pagingState.lastFailedAction, pagingState.generation, pagingState.lastFailedPage)
            }
            
            when (actionToRetry) {
                RetryAction.REFRESH -> {
                    _feedState.update { it.copy(isRetrying = true, error = null, appendError = null) }
                    try {
                        val response = fetchPageWithRetry(0)
                        var hasMoreAfterRetry = true
                        pagingMutex.withLock {
                            if (requestGeneration != pagingState.generation) return@launch
                            val level = levelFlow.value ?: return@launch
                            val posts = response.items.map { it.toPost() }
                            localDataSource.replacePosts(level, posts)
                            response.activeIssuesCount?.let { count ->
                                localDataSource.cacheActiveIssues(level, count)
                            }
                            pagingState = pagingState.copy(nextPage = 1, hasMore = response.nextKey != null && posts.isNotEmpty(), lastFailedAction = null)
                            hasMoreAfterRetry = pagingState.hasMore
                            saveCurrentPagingState()
                        }
                        _feedState.update { it.copy(isRetrying = false, hasMore = hasMoreAfterRetry, error = null, appendError = null) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _feedState.update { it.copy(isRetrying = false, hasMore = pagingState.hasMore, error = mapToFeedError(e)) }
                    }
                }
                RetryAction.LOAD_MORE -> {
                    _feedState.update { it.copy(isRetrying = true, isAppending = true, appendError = null) }
                    try {
                        val response = fetchPageWithRetry(pageToLoad)
                        var hasMoreAfterRetry = true
                        pagingMutex.withLock {
                            if (requestGeneration != pagingState.generation) return@launch
                            val level = levelFlow.value ?: return@launch
                            val posts = response.items.map { it.toPost() }
                            localDataSource.appendPosts(level, posts)
                            response.activeIssuesCount?.let { count ->
                                localDataSource.cacheActiveIssues(level, count)
                            }
                            pagingState = pagingState.copy(nextPage = pageToLoad + 1, hasMore = response.nextKey != null && posts.isNotEmpty(), lastFailedAction = null)
                            hasMoreAfterRetry = pagingState.hasMore
                            saveCurrentPagingState()
                        }
                        _feedState.update {
                            it.copy(
                                isRetrying = false,
                                isAppending = false,
                                hasMore = hasMoreAfterRetry,
                                appendError = null
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _feedState.update {
                            it.copy(
                                isRetrying = false,
                                isAppending = false,
                                hasMore = pagingState.hasMore,
                                appendError = mapToFeedError(e)
                            )
                        }
                    }
                }
                null -> {
                    if (pagingState.nextPage == 0) refresh(FeedRefreshReason.RETRY)
                    else loadMore()
                }
            }
        }
    }

    override fun observeActiveIssuesCount(postLevel: PostLevel): Flow<Int> {
        return localDataSource.observeActiveIssues(postLevel).map { it ?: 0 }
    }
}
