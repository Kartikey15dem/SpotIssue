package org.example.project.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation backed by ConnectivityManager callbacks.
 *
 * Note: This is a process-wide singleton in Koin, so we intentionally don't unregister.
 */
class AndroidNetworkMonitor(
    context: Context,
) : NetworkMonitor {

    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    private val _isOnline = MutableStateFlow(currentHasValidatedInternet())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = currentHasValidatedInternet()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            _isOnline.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        }

        override fun onLost(network: Network) {
            _isOnline.value = currentHasValidatedInternet()
        }
    }

    init {
        // registerDefaultNetworkCallback is available from API 24, which is fine for this app.
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    private fun currentHasValidatedInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

