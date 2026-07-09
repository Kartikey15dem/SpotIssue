package org.example.project.core.utils.paging

import androidx.paging.CombinedLoadStates
import androidx.paging.ItemSnapshotList

/**
 * Represents the complete, immutable state of a paginated list at a given moment in time.
 * This is the single source of truth observed by the UI layer (e.g. SwiftUI).
 */
data class PagingState<T : Any>(
    val snapshot: ItemSnapshotList<T>,
    val loadStates: CombinedLoadStates? = null
) {
    val itemCount: Int
        get() = snapshot.size

    val items: List<T>
        get() = snapshot.items

    val isRefreshing: Boolean
        get() = loadStates?.refresh is androidx.paging.LoadState.Loading || loadStates?.mediator?.refresh is androidx.paging.LoadState.Loading

    val isAppending: Boolean
        get() = loadStates?.append is androidx.paging.LoadState.Loading || loadStates?.mediator?.append is androidx.paging.LoadState.Loading

    val isRefreshError: Boolean
        get() = loadStates?.refresh is androidx.paging.LoadState.Error || loadStates?.mediator?.refresh is androidx.paging.LoadState.Error

    val isAppendError: Boolean
        get() = loadStates?.append is androidx.paging.LoadState.Error || loadStates?.mediator?.append is androidx.paging.LoadState.Error

    val refreshError: String?
        get() = (loadStates?.refresh as? androidx.paging.LoadState.Error)?.error?.message
            ?: (loadStates?.mediator?.refresh as? androidx.paging.LoadState.Error)?.error?.message

    val appendError: String?
        get() = (loadStates?.append as? androidx.paging.LoadState.Error)?.error?.message
            ?: (loadStates?.mediator?.append as? androidx.paging.LoadState.Error)?.error?.message

    val error: String?
        get() = refreshError ?: appendError

    val isAppendEndOfPaginationReached: Boolean
        get() = (loadStates?.append?.endOfPaginationReached == true) && (loadStates?.mediator?.append?.endOfPaginationReached == true)
}
