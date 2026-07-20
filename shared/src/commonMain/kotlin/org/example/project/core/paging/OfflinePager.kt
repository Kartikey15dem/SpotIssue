package org.example.project.core.paging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.core.model.home.Post
import org.example.project.core.presentation.FeedError
import org.example.project.core.presentation.FeedRefreshReason
import org.example.project.core.presentation.FeedState
import org.example.project.core.presentationcache.PresentationCache
import org.example.project.core.window.FeedConfig
import org.example.project.core.window.WindowEngine

data class PagingResult(
    val items: List<Post>,
    val nextKey: Int?,
    val hasMore: Boolean,
)

enum class RetryAction {
    REFRESH,
    LOAD_MORE,
}

data class PagingState(
    val nextPage: Int = 0,
    val hasMore: Boolean = true,
    val generation: Long = 0L,
    val lastFailedAction: RetryAction? = null,
    val lastFailedPage: Int = 0,
)

/**
 * OfflinePager: The Generic Engine for Offline-First Pagination
 *
 * This class abstracts away all the complex boilerplate required to build an offline-first
 * paginated list that caches items in a Room database and displays them via a UI StateFlow.
 *
 * WHY THIS EXISTS:
 * If you build pagination directly inside a Repository, you end up copy-pasting Mutex locks,
 * Exponential Backoff logic, and `PagingState` tracking into every single screen. This class
 * handles it generically.
 *
 * HOW IT WORKS:
 * 1. UI calls `start(key)`. We check the local DB. If empty, we show a loading spinner.
 * 2. `refresh()` or `loadMore()` is called. We lock the `pagingMutex` to prevent race conditions.
 * 3. We fetch data from the network (`fetchFromNetwork`).
 * 4. We save the data to the local DB (`saveToDatabase`).
 * 5. The local DB automatically emits the new list to `observeDatabase`.
 * 6. We update our `PresentationCache` (to stop the UI from stuttering) and push the new list to `uiState`.
 */
class OfflinePager<Key : Any>(
    // The coroutine scope to launch network and database jobs (usually Dispatchers.IO)
    private val scope: CoroutineScope,
    // The lambda that actually makes the API call. It returns a `PagingResult` containing the list of Posts.
    private val fetchFromNetwork: suspend (key: Key, page: Int) -> PagingResult,
    // The lambda that saves the network response to the Room database.
    private val saveToDatabase: suspend (key: Key, items: List<Post>, isRefresh: Boolean) -> Unit,
    // The lambda that actively listens to the Room database for changes (using a flow).
    private val observeDatabase: (key: Key, anchorCachedAt: Long?, anchorId: String?, limit: Int) -> Flow<List<Post>>,
    // Returns the total number of items currently cached in the database for a specific key.
    private val getCachedCount: suspend (key: Key) -> Int,
    // Checks if the device has an active internet connection.
    private val isOnlineProvider: () -> Boolean,
    // If true, the pager skips the database entirely and just stores the items in memory (e.g. for Search).
    private val isInMemoryOnly: Boolean = false,
) {
    // ---------------------------------------------------------
    // CONCURRENCY & CACHING HANDLERS
    // ---------------------------------------------------------

    // pagingMutex: The "Bouncer". If the user rapidly swipes pull-to-refresh while also scrolling
    // to the bottom, the Mutex forces these actions to wait in line so they don't corrupt the database.
    private val pagingMutex = Mutex()

    // pagingStatesByKey: Remembers the user's scroll state for different tabs. If they scroll down 5 pages
    // in the "Locality" tab, switch to "State", and switch back, this map remembers they were on page 5.
    private val pagingStatesByKey = mutableMapOf<Key, PagingState>()

    // windowEnginesByKey: Controls how many items from the local database are actually loaded into memory.
    // We don't want to load 10,000 cached posts at once, so the window engine uses a "Sliding Window" limit.
    private val windowEnginesByKey = mutableMapOf<Key, WindowEngine<Post>>()

    // Remembers which tabs have already had a silent background refresh when the app started.
    private val automaticRefreshAttemptedKeys = mutableSetOf<Key>()

    // presentationCache: Caches UI models. Jetpack Compose can lag if it has to recalculate massive lists.
    // This intercepts the database output and ensures smooth scrolling.
    private val presentationCache = PresentationCache<Post, String> { it.id }

    private var networkJob: Job? = null
    private var dbJob: Job? = null

    private val _activeKey = MutableStateFlow<Key?>(null)
    private var pagingState = PagingState()

    private val _uiState = MutableStateFlow(FeedState())
    val uiState: StateFlow<FeedState> = _uiState.asStateFlow()

    private val windowState = MutableStateFlow(WindowEngine<Post>().getState())

    private fun windowEngineFor(key: Key): WindowEngine<Post> = windowEnginesByKey.getOrPut(key) { WindowEngine() }

    private fun saveCurrentPagingState() {
        _activeKey.value?.let { key ->
            pagingStatesByKey[key] = pagingState
        }
    }

    fun start(key: Key) {
        if (_activeKey.value == key) return

        scope.launch {
            val cachedCount = if (isInMemoryOnly) 0 else getCachedCount(key)
            var shouldRefresh = false

            pagingMutex.withLock {
                networkJob?.cancel()
                saveCurrentPagingState()
                val restored = pagingStatesByKey[key] ?: PagingState()
                pagingState =
                    restored.copy(
                        generation = restored.generation + 1,
                        lastFailedAction = null,
                    )
                presentationCache.clear()
                _activeKey.value = key
                windowState.value = windowEngineFor(key).getState()
                shouldRefresh = key !in automaticRefreshAttemptedKeys

                _uiState.update {
                    it.copy(
                        posts = if (isInMemoryOnly) emptyList() else it.posts,
                        isLoading = shouldRefresh && cachedCount == 0,
                        isRefreshing = false,
                        isAppending = false,
                        isBackgroundRefreshing = false,
                        isRetrying = false,
                        hasMore = pagingState.hasMore,
                        error = null,
                        appendError = null,
                        isOffline = !isOnlineProvider(),
                    )
                }
            }

            if (!isInMemoryOnly) {
                restartDatabaseObservation()
            }

            if (shouldRefresh) {
                refresh(FeedRefreshReason.LEVEL_CHANGED)
            } else if (cachedCount == 0 && !isInMemoryOnly) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun stop() {
        networkJob?.cancel()
        dbJob?.cancel()
    }

    fun clear() {
        _activeKey.value = null
        networkJob?.cancel()
        pagingState = PagingState(generation = pagingState.generation + 1)
        _uiState.value = FeedState()
    }

    fun updateOnlineStatus(isOnline: Boolean) {
        _uiState.update { it.copy(isOffline = !isOnline) }
    }

    // ---------------------------------------------------------
    // PAGINATION LOGIC
    // ---------------------------------------------------------

    // Triggers a network request for Page 0 (Fresh data).
    fun refresh(reason: FeedRefreshReason) {
        val key = _activeKey.value ?: return
        if (!isOnlineProvider()) return

        // Don't auto-refresh the same tab twice when the app resumes.
        if ((reason == FeedRefreshReason.LEVEL_CHANGED || reason == FeedRefreshReason.NETWORK_RESTORED)) {
            if (!automaticRefreshAttemptedKeys.add(key)) return
        }

        networkJob?.cancel() // Cancel any ongoing fetch
        networkJob =
            scope.launch {
                val cachedCount = if (isInMemoryOnly) _uiState.value.posts.size else getCachedCount(key)
                val isBackground =
                    (reason == FeedRefreshReason.APP_RESUMED || reason == FeedRefreshReason.NETWORK_RESTORED) && cachedCount > 0

                // 1. Show the loading spinner in the UI State IMMEDIATELY before waiting for the lock
                _uiState.update {
                    when {
                        cachedCount == 0 ->
                            it.copy(
                                isLoading = true,
                                isRefreshing = false,
                                isBackgroundRefreshing = false,
                                isRetrying = false,
                                error = null,
                                appendError = null,
                            )
                        isBackground ->
                            it.copy(
                                isLoading = false,
                                isBackgroundRefreshing = true,
                                isRefreshing = false,
                                error = null,
                                appendError = null,
                            )
                        else ->
                            it.copy(
                                isLoading = false,
                                isRefreshing = true,
                                isBackgroundRefreshing = false,
                                error = null,
                                appendError = null,
                            )
                    }
                }

                // 2. Lock the Mutex and bump the Generation Counter.
                // Why bump generation? If a previous refresh was taking too long, its response might arrive LATE.
                // By incrementing the generation, we can check the generation later and discard late, outdated data.
                val requestGeneration =
                    pagingMutex.withLock {
                        pagingState =
                            pagingState.copy(
                                nextPage = 0, // Reset to page 0
                                hasMore = true,
                                generation = pagingState.generation + 1,
                                lastFailedAction = null,
                                lastFailedPage = 0,
                            )
                        windowState.value = windowEngineFor(key).reset()
                        pagingState.generation
                    }

                try {
                    // 3. Fetch from Network (with exponential backoff retries if the server fails)
                    val response = fetchPageWithRetry(key, 0)

                    // 4. Lock the Mutex again before touching the Database
                    pagingMutex.withLock {
                        // **THE GENERATION CHECK**
                        // If the user triggered ANOTHER refresh while we were waiting, our requestGeneration
                        // will be OLDER than the pagingState.generation. We abort instantly!
                        if (requestGeneration != pagingState.generation) return@launch
                        val currentKey = _activeKey.value ?: return@launch

                        if (isInMemoryOnly) {
                            _uiState.update { it.copy(posts = response.items) } // For Search
                        } else {
                            saveToDatabase(currentKey, response.items, true) // For Feed/Profile
                        }

                        // Increment nextPage for the next time loadMore() is called
                        pagingState =
                            pagingState.copy(
                                nextPage = response.nextKey ?: 0,
                                hasMore = response.hasMore,
                            )
                        saveCurrentPagingState()
                    }

                    // 5. Hide the loading spinner
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isAppending = false,
                            isBackgroundRefreshing = false,
                            isRetrying = false,
                            hasMore = pagingState.hasMore,
                            error = null,
                            appendError = null,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // If it completely fails, map the error and show it in the UI
                    val error = mapToFeedError(e)
                    pagingMutex.withLock {
                        if (requestGeneration == pagingState.generation) {
                            pagingState = pagingState.copy(lastFailedAction = RetryAction.REFRESH, lastFailedPage = 0)
                            saveCurrentPagingState()
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isBackgroundRefreshing = false,
                            isRetrying = false,
                            error = if (isBackground) null else error,
                        )
                    }
                }
            }
    }

    fun loadMore() {
        val key = _activeKey.value ?: return
        if (!isOnlineProvider()) return

        networkJob =
            scope.launch {
                val (shouldFetch, requestGeneration, pageToLoad) =
                    pagingMutex.withLock {
                        val state = _uiState.value
                        if (state.isLoading ||
                            state.isRefreshing ||
                            state.isAppending ||
                            state.isRetrying ||
                            state.isBackgroundRefreshing ||
                            !pagingState.hasMore
                        ) {
                            return@withLock Triple(false, 0L, 0)
                        }

                        if (!isInMemoryOnly) {
                            val currentState = windowState.value
                            val items = presentationCache.items
                            val anchor =
                                if (items.size >
                                    FeedConfig.LOAD_MORE_THRESHOLD
                                ) {
                                    items[items.size - FeedConfig.LOAD_MORE_THRESHOLD]
                                } else {
                                    items.lastOrNull()
                                }
                            val nextState = windowEngineFor(key).expand(anchor)
                            if (nextState != currentState) {
                                windowState.value = nextState
                            } else {
                                return@withLock Triple(false, 0L, 0)
                            }
                        }

                        _uiState.update { it.copy(isAppending = true, appendError = null) }
                        Triple(true, pagingState.generation, pagingState.nextPage)
                    }

                if (!shouldFetch) return@launch

                try {
                    val response = fetchPageWithRetry(key, pageToLoad)

                    pagingMutex.withLock {
                        if (requestGeneration != pagingState.generation) return@launch
                        val currentKey = _activeKey.value ?: return@launch

                        if (isInMemoryOnly) {
                            _uiState.update { it.copy(posts = it.posts + response.items) }
                        } else {
                            saveToDatabase(currentKey, response.items, false)
                        }

                        pagingState =
                            pagingState.copy(
                                nextPage = response.nextKey ?: pageToLoad,
                                hasMore = response.hasMore,
                            )
                        saveCurrentPagingState()
                    }

                    _uiState.update {
                        it.copy(isAppending = false, hasMore = pagingState.hasMore, appendError = null)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val error = mapToFeedError(e)
                    pagingMutex.withLock {
                        if (requestGeneration == pagingState.generation) {
                            pagingState = pagingState.copy(lastFailedAction = RetryAction.LOAD_MORE, lastFailedPage = pageToLoad)
                            saveCurrentPagingState()
                        }
                    }
                    _uiState.update {
                        it.copy(isAppending = false, appendError = error)
                    }
                }
            }
    }

    fun retry() {
        when (pagingState.lastFailedAction) {
            RetryAction.LOAD_MORE -> loadMore()
            else -> refresh(FeedRefreshReason.RETRY)
        }
    }

    // ---------------------------------------------------------
    // EXPONENTIAL BACKOFF RETRY
    // ---------------------------------------------------------

    // A robust fetching mechanism. If the server flakes out or the user hits a temporary
    // network timeout, it won't crash instantly. It will wait 1 second, then try again.
    // If it fails again, it waits 2 seconds... up to 4 attempts.
    private suspend fun fetchPageWithRetry(
        key: Key,
        page: Int,
    ): PagingResult {
        var attempt = 1
        var delayMs = 1000L
        while (true) {
            try {
                return fetchFromNetwork(key, page)
            } catch (e: CancellationException) {
                throw e // NEVER swallow CancellationException in Coroutines!
            } catch (e: Exception) {
                val error = mapToFeedError(e)
                val isRetryable = error is FeedError.Server || error is FeedError.Network || error is FeedError.Timeout
                if (!isRetryable || attempt >= 4) throw e // Give up
                delay(delayMs)
                attempt++
                delayMs *= 2 // Double the wait time
            }
        }
    }

    private fun restartDatabaseObservation() {
        dbJob?.cancel()
        dbJob =
            scope.launch {
                combine(_activeKey, windowState) { key, window -> Pair(key, window) }
                    .flatMapLatest { (key, window) ->
                        if (key == null) {
                            flowOf(emptyList())
                        } else {
                            observeDatabase(
                                key,
                                if (window.anchor is org.example.project.core.model.home.Post) {
                                    (window.anchor as org.example.project.core.model.home.Post)
                                        .cachedAt
                                } else {
                                    null
                                },
                                if (window.anchor is org.example.project.core.model.home.Post) {
                                    (window.anchor as org.example.project.core.model.home.Post)
                                        .id
                                } else {
                                    null
                                },
                                window.limit,
                            )
                        }
                    }.collect { dbItems ->
                        presentationCache.update(dbItems)
                        _uiState.update { it.copy(posts = presentationCache.items.toList()) }
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
}
