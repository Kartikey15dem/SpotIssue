package org.example.project.core.utils.paging.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Internal class responsible for managing the coroutine lifecycle of the paging bridge.
 * It provides a dedicated CoroutineScope and ensures proper cleanup when paging is closed.
 */
internal class PagingLifecycle {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Launches a coroutine safely within the paging lifecycle scope.
     */
    fun launch(block: suspend CoroutineScope.() -> Unit) {
        scope.launch { block() }
    }

    /**
     * Cancels the underlying CoroutineScope, releasing all resources and collectors.
     */
    fun close() {
        scope.cancel()
    }
}
