package org.example.project.core.presentation

import org.example.project.core.model.home.Post

data class FeedState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isBackgroundRefreshing: Boolean = false,
    val isRetrying: Boolean = false,
    val isOffline: Boolean = false,
    val hasMore: Boolean = true,
    val error: FeedError? = null
)
