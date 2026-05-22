package org.example.project.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS stub implementation.
 *
 * This keeps common repositories compiling for iOS even though IssueSpot UI is Android-only today.
 * We default to "online" to preserve existing behavior on iOS.
 */
class IosNetworkMonitor : NetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
}

