package org.example.project.core.utils.paging

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SwiftPagingBridge<T : Any>(
    pagingFlow: Flow<PagingData<T>>
) {

    private val scope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state =
        MutableStateFlow(PagingSnapshot<T>())

    val state: StateFlow<PagingSnapshot<T>>
        get() = _state.asStateFlow()

    private val presenter =
        object : PagingDataPresenter<T>() {

            override suspend fun presentPagingDataEvent(
                event: PagingDataEvent<T>
            ) {
                updateSnapshot()
            }
        }

    init {

        scope.launch {
            pagingFlow.collectLatest { pagingData ->
                presenter.collectFrom(pagingData)
            }
        }

        scope.launch {
            presenter.loadStateFlow.collect { loadStates ->
                updateLoadState(loadStates)
            }
        }
    }

    fun get(index: Int): T? {
        return presenter[index]
    }

    fun refresh() {
        presenter.refresh()
    }

    fun retry() {
        presenter.retry()
    }

    private fun updateSnapshot() {
        _state.value =
            _state.value.copy(
                items = presenter.snapshot().items
            )
    }

    private fun updateLoadState(
        loadStates: CombinedLoadStates?
    ) {
        if (loadStates == null) return

        val refresh = loadStates.refresh
        val append = loadStates.append

        _state.value =
            _state.value.copy(
                isRefreshing = refresh is LoadState.Loading,
                isAppending = append is LoadState.Loading,
                isRefreshError = refresh is LoadState.Error,
                isAppendError = append is LoadState.Error,
                isAppendEndOfPaginationReached = append.endOfPaginationReached,
                error = when {
                    refresh is LoadState.Error ->
                        refresh.error.message

                    append is LoadState.Error ->
                        append.error.message

                    else -> null
                }
            )
    }
}