package org.example.project.core.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.example.project.core.window.FeedConfig

/**
 * A reusable composable that observes a [LazyListState] and triggers a callback
 * when the user scrolls near the bottom of the list.
 *
 * @param listState The state of the LazyColumn/LazyRow to observe.
 * @param buffer How many items from the bottom to trigger the load. Defaults to [FeedConfig.LOAD_MORE_THRESHOLD].
 * @param onLoadMore The callback to trigger when the bottom is reached.
 */
@Composable
fun InfiniteScrollHandler(
    listState: LazyListState,
    buffer: Int = FeedConfig.LOAD_MORE_THRESHOLD,
    isRefreshing: Boolean = false,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(listState, isRefreshing) {
        if (!isRefreshing) {
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                Pair(
                    layoutInfo.visibleItemsInfo.lastOrNull()?.index,
                    layoutInfo.totalItemsCount
                )
            }.collect { (lastVisibleItem, totalItems) ->
                if (lastVisibleItem != null && totalItems > 0 && lastVisibleItem >= totalItems - buffer) {
                    onLoadMore()
                }
            }
        }
    }
}
