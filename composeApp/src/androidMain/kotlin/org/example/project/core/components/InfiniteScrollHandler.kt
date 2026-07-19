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
    onLoadMore: () -> Unit
) {
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisibleItem ->
                // Check the total number of items currently in the list
                val totalItems = listState.layoutInfo.totalItemsCount
                
                // If the user has scrolled down far enough that the remaining invisible items
                // are less than or equal to our buffer (e.g. 5 items left), trigger a load!
                if (lastVisibleItem != null &&
                    totalItems > 0 &&
                    lastVisibleItem >= totalItems - buffer
                ) {
                    onLoadMore()
                }
            }
    }
}
