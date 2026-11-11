package com.amr.shared.connectivity

import com.amr.core.data.concurrency.ApplicationScope
import com.amr.core.domain.model.NetworkStatus
import com.amr.core.domain.repository.ConnectivityObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.shareIn
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_queue_create

/**
 * Network path status from NWPathMonitor. A satisfied path only proves a usable
 * route exists, not verified internet access, so [NetworkStatus.isInternetValidated]
 * is always null here. One monitor is shared by all collectors and cancelled
 * [STOP_TIMEOUT_MILLIS] after the last one leaves.
 *
 * Not compiled or tested on Windows: needs verification on macOS.
 */
class IosConnectivityObserver(
    applicationScope: ApplicationScope
) : ConnectivityObserver {

    override val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val monitor = nw_path_monitor_create()
        val queue = dispatch_queue_create("com.amr.connectivity", null)

        nw_path_monitor_set_update_handler(monitor) { path ->
            trySend(
                NetworkStatus(
                    isConnected = nw_path_get_status(path) == nw_path_status_satisfied,
                    isInternetValidated = null
                )
            )
        }
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_start(monitor)

        awaitClose { nw_path_monitor_cancel(monitor) }
    }
        .distinctUntilChanged()
        .shareIn(applicationScope.scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
