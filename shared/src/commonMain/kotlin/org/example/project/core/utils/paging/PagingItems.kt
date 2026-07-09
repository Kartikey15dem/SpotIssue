package org.example.project.core.utils.paging

import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.core.utils.paging.internal.PagingCollector
import org.example.project.core.utils.paging.internal.PagingLifecycle
import org.example.project.core.utils.paging.internal.PagingStateUpdater
import kotlin.time.Clock

private const val LOG = "[KMP_PAGING_TRACE]"

/**
 * The core presentation object for paging in Kotlin Multiplatform.
 * This class serves the same purpose as Android's LazyPagingItems, wrapping a
 * PagingDataPresenter and exposing a single observable StateFlow for the UI.
 * It delegates actual collection, state updates, and lifecycle management to internal helpers.
 */
class PagingItems<T : Any>(
    pagingFlow: Flow<PagingData<T>>
) {
    private val lifecycle = PagingLifecycle()

    private val mutableState = MutableStateFlow(
        PagingState<T>(
            snapshot = androidx.paging.ItemSnapshotList(0, 0, emptyList<T>()),
            loadStates = null
        )
    )

    private lateinit var stateUpdater: PagingStateUpdater<T>

    private val presenter: PagingDataPresenter<T> = object : PagingDataPresenter<T>() {
        override suspend fun presentPagingDataEvent(event: PagingDataEvent<T>) {
            println("""
$LOG PAGING EVENT
$LOG event=${event.toString().substringBefore("@")}
$LOG before=${snapshot().items.size}
$LOG ==========================
""")
            println("[PRESENTER_EVENTS] EVENT RECEIVED | event=${event.toString().substringBefore("@")} | snapshotBefore=${snapshot().items.size} | time=${kotlin.time.Clock.System.now().toEpochMilliseconds()}")
            
            if (::stateUpdater.isInitialized) {
                stateUpdater.updateSnapshot()
            }
            
            println("""
$LOG PAGING EVENT COMPLETE
$LOG after=${snapshot().items.size}
$LOG ==========================
""")
            println("[PRESENTER_EVENTS] EVENT COMPLETE | event=${event.toString().substringBefore("@")} | snapshotAfter=${snapshot().items.size}")
        }
    }

    private val collector = PagingCollector(presenter)

    val state: StateFlow<PagingState<T>>
        get() = mutableState.asStateFlow()

    val itemCount: Int
        get() = mutableState.value.items.size

    init {
        println("""
$LOG PAGING ITEMS CREATED
$LOG instance=${this.hashCode()}
$LOG ==========================
""")
        stateUpdater = PagingStateUpdater(presenter, mutableState)
        
        // Initial snapshot sync
        stateUpdater.updateSnapshot()
        
        lifecycle.launch {
            collector.collectFrom(pagingFlow)
        }

        lifecycle.launch {
            stateUpdater.collectUpdates()
        }
    }

    operator fun get(index: Int): T? {
        if (index == itemCount - 1) {
            println("""
$LOG VIEWPORT HINT
$LOG requestedIndex=$index
$LOG itemCount=$itemCount
$LOG ==========================
""")
        }
        return try {
            if (index >= 0 && index < presenter.snapshot().size) {
                presenter[index]
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun peek(index: Int): T? {
        return try {
            if (index >= 0 && index < presenter.snapshot().size) {
                presenter.peek(index)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun refresh() {
        println("[KMP_PAGING]\nPagingItems REFRESH")
        presenter.refresh()
    }

    fun retry() {
        println("[KMP_PAGING]\nPagingItems RETRY")
        presenter.retry()
    }

    fun close() {
        println("""
$LOG PAGING ITEMS CLOSED
$LOG instance=${this.hashCode()}
$LOG ==========================
""")
        lifecycle.close()
    }
}
