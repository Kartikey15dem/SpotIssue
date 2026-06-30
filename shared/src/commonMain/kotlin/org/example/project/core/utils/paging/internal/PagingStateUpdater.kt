package org.example.project.core.utils.paging.internal

import androidx.paging.PagingDataPresenter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.example.project.core.utils.paging.PagingState

/**
 * Internal class responsible for synchronizing the PagingDataPresenter's snapshot
 * and load states into a single, immutable PagingState.
 */
internal class PagingStateUpdater<T : Any>(
    private val presenter: PagingDataPresenter<T>,
    private val mutableState: MutableStateFlow<PagingState<T>>
) {
    /**
     * Updates the state with the latest items snapshot from the presenter.
     * This should be called whenever a PagingDataEvent occurs.
     */
    fun updateSnapshot() {
        mutableState.update { currentState ->
            currentState.copy(snapshot = presenter.snapshot())
        }
    }

    /**
     * Suspends and continuously collects load states from the presenter,
     * merging them atomically into the immutable PagingState.
     */
    suspend fun collectLoadStates() {
        presenter.loadStateFlow.collect { loadStates ->
            mutableState.update { currentState ->
                currentState.copy(loadStates = loadStates)
            }
        }
    }
}
