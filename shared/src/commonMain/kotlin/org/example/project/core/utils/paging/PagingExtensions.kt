package org.example.project.core.utils.paging

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

fun <T : Any> Flow<PagingData<T>>.asSwiftPagingBridge(): SwiftPagingBridge<T> {
    return SwiftPagingBridge(this)
}

/**
 * Creates a [PagingItems] instance from a Flow of PagingData.
 * This mirrors Android's `collectAsLazyPagingItems()` extension function, providing
 * a clean API for converting the PagingData stream into an observable presentation object.
 */
fun <T : Any> Flow<PagingData<T>>.collectAsPagingItems(): PagingItems<T> {
    return PagingItems(this)
}