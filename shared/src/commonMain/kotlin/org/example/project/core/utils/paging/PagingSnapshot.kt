package org.example.project.core.utils.paging

data class PagingSnapshot<T : Any>(
    val items: List<T> = emptyList(),
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val error: String? = null,
    val isRefreshError: Boolean = false,
    val isAppendError: Boolean = false,
    val isAppendEndOfPaginationReached: Boolean = false
)