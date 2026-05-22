package org.example.project.core.network

import kotlinx.coroutines.flow.StateFlow

/**
 * Multiplatform network monitor.
 *
 * Repository layer uses this to decide whether to page from network or from Room.
 * ViewModels should not contain online/offline branching logic.
 */
interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

