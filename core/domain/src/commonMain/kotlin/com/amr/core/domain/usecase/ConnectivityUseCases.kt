package com.amr.core.domain.usecase

import com.amr.core.domain.model.ApiStatus
import com.amr.core.domain.model.AppConnectivity
import com.amr.core.domain.repository.ApiStatusRepository
import com.amr.core.domain.repository.ConnectivityObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** The global status: network problems first, then API health from real requests. */
class ObserveAppConnectivityUseCase(
    private val connectivityObserver: ConnectivityObserver,
    private val apiStatusRepository: ApiStatusRepository
) {
    operator fun invoke(): Flow<AppConnectivity> =
        combine(connectivityObserver.networkStatus, apiStatusRepository.apiStatus) { network, api ->
            when {
                !network.isConnected -> AppConnectivity.Offline
                network.isInternetValidated == false -> AppConnectivity.NoInternet
                api == ApiStatus.Degraded -> AppConnectivity.ApiDegraded
                api == ApiStatus.Unstable -> AppConnectivity.Unstable
                else -> AppConnectivity.Normal
            }
        }.distinctUntilChanged()
}

/**
 * Emits once each time the network becomes usable again after being unusable,
 * after it stayed usable for [debounceMillis]. The first status a collector sees
 * never triggers, so starting collection while online does nothing, and a
 * connection that drops again within the debounce window does not trigger.
 */
class ObserveConnectivityRestoredUseCase(
    private val connectivityObserver: ConnectivityObserver,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS
) {
    /** Whether the network is usable right now (one read, no ongoing collection). */
    suspend fun isNetworkUsable(): Boolean = connectivityObserver.networkStatus.first().isUsable

    operator fun invoke(): Flow<Unit> = channelFlow {
        var wasUnusable = false
        connectivityObserver.networkStatus
            .map { it.isUsable }
            .distinctUntilChanged()
            .collectLatest { usable ->
                if (!usable) {
                    wasUnusable = true
                } else if (wasUnusable) {
                    delay(debounceMillis)
                    wasUnusable = false
                    send(Unit)
                }
            }
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MILLIS = 1_500L
    }
}

/**
 * Lets a screen react to connectivity only while it is visible. While hidden
 * nothing is collected, so the platform callback can be released and a screen
 * in the back stack never fires requests.
 *
 * Triggers [launchIn]'s callback:
 * - on each unavailable -> available transition seen while visible, and
 * - when the screen becomes visible again (tab switch, back navigation, app
 *   foreground) while the network is usable: the network may have returned while
 *   it was hidden. The first time the screen becomes visible never triggers, so
 *   the screen's initial load is not doubled.
 *
 * The callback is only a hint: the screen retries only an operation that failed
 * for a connectivity reason, and its guards plus the repository's single-flight
 * merge it with any request already running. Nothing here repeats by itself, so
 * a failing recovery cannot loop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityRecovery(
    private val connectivityRestored: ObserveConnectivityRestoredUseCase
) {
    /**
     * [starts] counts how often the screen became visible. Each start is a distinct
     * value, so a quick stop/start pair is never conflated into "still visible".
     */
    private data class Visibility(val isVisible: Boolean, val starts: Int)

    private val visibility = MutableStateFlow(Visibility(isVisible = false, starts = 0))

    fun setVisible(isVisible: Boolean) {
        visibility.update { current ->
            when {
                isVisible && !current.isVisible -> Visibility(isVisible = true, starts = current.starts + 1)
                isVisible -> current
                else -> current.copy(isVisible = false)
            }
        }
    }

    fun launchIn(scope: CoroutineScope, onRestored: () -> Unit): Job = scope.launch {
        visibility
            .flatMapLatest { state ->
                if (!state.isVisible) {
                    emptyFlow()
                } else {
                    flow {
                        val isReturning = state.starts > 1
                        if (isReturning && connectivityRestored.isNetworkUsable()) emit(Unit)
                        emitAll(connectivityRestored())
                    }
                }
            }
            .collect { onRestored() }
    }
}
