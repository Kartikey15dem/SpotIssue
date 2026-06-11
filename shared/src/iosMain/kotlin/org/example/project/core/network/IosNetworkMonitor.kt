package org.example.project.core.network


import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import org.example.project.core.utils.NetworkMonitor
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

class DarwinNetworkMonitor : NetworkMonitor {

    override val isOnline: Flow<Boolean> = callbackFlow {
        // Create the network path monitor (equivalent to ConnectivityManager)
        val monitor = nw_path_monitor_create()

        // Set the callback for whenever the network status changes
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            val isConnected = status == nw_path_status_satisfied
            channel.trySend(isConnected)
        }

        // NWPathMonitor requires a dispatch queue to run on
        val queue = dispatch_get_main_queue()
        nw_path_monitor_set_queue(monitor, queue)

        // Start monitoring
        nw_path_monitor_start(monitor)

        // Cleanup when the Flow collection is cancelled
        awaitClose {
            nw_path_monitor_cancel(monitor)
        }
    }.conflate()
}

