package org.example.project.core.utils.paging.internal

import androidx.paging.PagingData
import androidx.paging.PagingDataPresenter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Internal class responsible for continuously collecting the upstream Flow
 * of PagingData and passing it to the PagingDataPresenter.
 */
internal class PagingCollector<T : Any>(
    private val presenter: PagingDataPresenter<T>
) {
    private val LOG = "[KMP_PAGING_TRACE]"

    /**
     * Suspends and continuously collects the paging flow.
     * This should be called from within the bridge's CoroutineScope.
     */
    suspend fun collectFrom(pagingFlow: Flow<PagingData<T>>) {
        println("""
$LOG COLLECT START
$LOG flow=${pagingFlow.hashCode()}
$LOG ==========================
""")
        pagingFlow.collectLatest { pagingData ->
            presenter.collectFrom(pagingData)
        }
    }
}
