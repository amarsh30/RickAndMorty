package com.amr.shared.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.amr.core.data.concurrency.ApplicationScope
import com.amr.core.domain.model.NetworkStatus
import com.amr.core.domain.repository.ConnectivityObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn

/**
 * Default-network status from [ConnectivityManager]. One OS callback is shared by
 * all collectors; it is registered on the first collector and unregistered
 * [STOP_TIMEOUT_MILLIS] after the last one leaves (e.g. the app went to background).
 *
 * NET_CAPABILITY_VALIDATED means Android verified internet access on the network;
 * it does not mean the Rick and Morty API answers.
 */
class AndroidConnectivityObserver(
    context: Context,
    applicationScope: ApplicationScope
) : ConnectivityObserver {

    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.toNetworkStatus())
            }

            override fun onLost(network: Network) {
                // Another network may take over; its capabilities arrive in a later callback.
                trySend(NetworkStatus.Disconnected)
            }
        }

        trySend(currentStatus())
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .shareIn(applicationScope.scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    private fun currentStatus(): NetworkStatus {
        val network = connectivityManager.activeNetwork ?: return NetworkStatus.Disconnected
        val capabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return NetworkStatus.Disconnected
        return capabilities.toNetworkStatus()
    }

    private fun NetworkCapabilities.toNetworkStatus() = NetworkStatus(
        isConnected = hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
        isInternetValidated = hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
