package com.amr.feature.character.fake

import com.amr.core.domain.model.NetworkStatus
import com.amr.core.domain.repository.ConnectivityObserver
import com.amr.core.domain.usecase.ObserveConnectivityRestoredUseCase
import kotlinx.coroutines.flow.MutableStateFlow

val ONLINE = NetworkStatus(isConnected = true, isInternetValidated = true)
val OFFLINE = NetworkStatus.Disconnected

class FakeConnectivityObserver(initial: NetworkStatus = ONLINE) : ConnectivityObserver {
    val status = MutableStateFlow(initial)
    override val networkStatus = status
}

/** A restored-events use case over [observer]; by default the network never changes. */
fun connectivityRestored(observer: ConnectivityObserver = FakeConnectivityObserver()) =
    ObserveConnectivityRestoredUseCase(observer)
