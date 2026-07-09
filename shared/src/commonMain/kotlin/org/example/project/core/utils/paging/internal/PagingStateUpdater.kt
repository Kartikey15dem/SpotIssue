package org.example.project.core.utils.paging.internal

import androidx.paging.PagingDataPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.combine
import org.example.project.core.utils.paging.PagingState

/**
 * Internal class responsible for synchronizing the PagingDataPresenter's snapshot
 * and load states into a single, immutable PagingState.
 */
internal class PagingStateUpdater<T : Any>(
    private val presenter: PagingDataPresenter<T>,
    private val mutableState: MutableStateFlow<PagingState<T>>
) {
    private val LOG = "[KMP_PAGING_TRACE]"

    private val snapshotFlow = MutableStateFlow(presenter.snapshot())

    /**
     * Updates the internal snapshot flow. 
     * This should be called whenever a PagingDataEvent occurs.
     */
    fun updateSnapshot() {
        val snapshot = presenter.snapshot()
        
        val firstId = snapshot.items.firstOrNull()?.let {
            (it as? org.example.project.core.model.home.Post)?.id ?: it.toString()
        }

        val lastId = snapshot.items.lastOrNull()?.let {
            (it as? org.example.project.core.model.home.Post)?.id ?: it.toString()
        }

        println("""
$LOG UPDATE SNAPSHOT
$LOG presenter=${presenter.hashCode()}
$LOG size=${snapshot.items.size}
$LOG first=$firstId
$LOG last=$lastId
$LOG time=${kotlin.time.Clock.System.now().toEpochMilliseconds()}
$LOG ==========================
""")
        println("[PRESENTER_EVENTS] SNAPSHOT UPDATED | size=${snapshot.items.size} | first=$firstId | last=$lastId")
        snapshotFlow.value = snapshot
    }

    /**
     * Suspends and continuously collects updates, coalescing snapshot and load states 
     * into a single publication to prevent redundant UI invalidations.
     */
    suspend fun collectUpdates() {
        combine(snapshotFlow, presenter.loadStateFlow) { snapshot, loadStates ->
            println("""
$LOG LOAD STATE
$LOG refresh=${loadStates?.refresh}
$LOG append=${loadStates?.append}
$LOG prepend=${loadStates?.prepend}
$LOG mediatorRefresh=${loadStates?.mediator?.refresh}
$LOG mediatorAppend=${loadStates?.mediator?.append}
$LOG ==========================
""")
            PagingState(snapshot = snapshot, loadStates = loadStates)
        }.collect { combinedState ->
            mutableState.value = combinedState
        }
    }
}
