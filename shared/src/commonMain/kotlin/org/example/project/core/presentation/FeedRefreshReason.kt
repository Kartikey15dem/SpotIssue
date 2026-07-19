package org.example.project.core.presentation

enum class FeedRefreshReason {
    INITIAL,
    PULL_TO_REFRESH,
    RETRY,
    LEVEL_CHANGED,
    APP_RESUMED,
    CACHE_EXPIRED,
    NETWORK_RESTORED,
}
