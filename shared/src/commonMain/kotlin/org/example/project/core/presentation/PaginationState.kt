package org.example.project.core.presentation

data class PaginationState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val isBackgroundRefreshing: Boolean = false,
    val isRetrying: Boolean = false,
    val isOffline: Boolean = false,
    val hasMore: Boolean = true,
    val error: FeedError? = null,
    val appendError: FeedError? = null,
)
