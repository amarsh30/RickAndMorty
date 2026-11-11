package com.amr.core.domain.usecase

import com.amr.core.domain.model.ApiStatus
import com.amr.core.domain.model.AppConnectivity
import com.amr.core.domain.model.NetworkStatus
import com.amr.core.domain.repository.ApiStatusRepository
import com.amr.core.domain.repository.ConnectivityObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityUseCasesTest {

    private class FakeConnectivity(initial: NetworkStatus) : ConnectivityObserver {
        val status = MutableStateFlow(initial)
        var collectors = 0
        override val networkStatus = kotlinx.coroutines.flow.flow {
            collectors++
            try {
                status.collect { emit(it) }
            } finally {
                collectors--
            }
        }
    }

    private class FakeApiStatus : ApiStatusRepository {
        override val apiStatus = MutableStateFlow(ApiStatus.Unknown)
    }

    private val online = NetworkStatus(isConnected = true, isInternetValidated = true)
    private val offline = NetworkStatus.Disconnected
    private val noInternet = NetworkStatus(isConnected = true, isInternetValidated = false)
    private val iosOnline = NetworkStatus(isConnected = true, isInternetValidated = null)

    // --- restored events ---

    @Test
    fun initialOnline_doesNotTrigger() = runTest {
        val connectivity = FakeConnectivity(online)
        var events = 0
        backgroundScope.launch { ObserveConnectivityRestoredUseCase(connectivity)().collect { events++ } }

        advanceTimeBy(10_000)

        assertEquals(0, events)
    }

    @Test
    fun offlineThenOnline_triggersOnceAfterDebounce() = runTest {
        val connectivity = FakeConnectivity(offline)
        var events = 0
        backgroundScope.launch { ObserveConnectivityRestoredUseCase(connectivity)().collect { events++ } }
        runCurrent()

        connectivity.status.value = online
        advanceTimeBy(1_499)
        runCurrent()
        assertEquals(0, events)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(1, events)

        advanceTimeBy(10_000)
        assertEquals(1, events)
    }

    @Test
    fun flappingConnection_withinDebounce_doesNotTrigger() = runTest {
        val connectivity = FakeConnectivity(offline)
        var events = 0
        backgroundScope.launch { ObserveConnectivityRestoredUseCase(connectivity)().collect { events++ } }
        runCurrent()

        repeat(5) {
            connectivity.status.value = online
            advanceTimeBy(500)
            connectivity.status.value = offline
            advanceTimeBy(500)
        }
        runCurrent()

        assertEquals(0, events)
    }

    @Test
    fun androidNetworkWithoutValidatedInternet_isNotARestore_butIosPathIs() = runTest {
        val connectivity = FakeConnectivity(offline)
        var events = 0
        backgroundScope.launch { ObserveConnectivityRestoredUseCase(connectivity)().collect { events++ } }
        runCurrent()

        connectivity.status.value = noInternet
        advanceTimeBy(2_000)
        assertEquals(0, events)

        // iOS cannot validate: a satisfied path counts as usable.
        connectivity.status.value = iosOnline
        advanceTimeBy(2_000)
        assertEquals(1, events)
    }

    // --- visibility gate ---

    @Test
    fun recovery_onlyCollectsWhileVisible() = runTest {
        val connectivity = FakeConnectivity(offline)
        val recovery = ConnectivityRecovery(ObserveConnectivityRestoredUseCase(connectivity))
        var restored = 0
        recovery.launchIn(backgroundScope) { restored++ }
        runCurrent()

        // Hidden: nothing is collected, so the platform callback is not held either.
        assertEquals(0, connectivity.collectors)
        connectivity.status.value = online
        advanceTimeBy(2_000)
        assertEquals(0, restored)

        recovery.setVisible(true)
        runCurrent()
        assertEquals(1, connectivity.collectors)
        connectivity.status.value = offline
        runCurrent()
        connectivity.status.value = online
        advanceTimeBy(2_000)
        assertEquals(1, restored)

        recovery.setVisible(false)
        runCurrent()
        assertEquals(0, connectivity.collectors)
    }

    @Test
    fun recovery_firstStart_neverTriggers_evenOnline() = runTest {
        val connectivity = FakeConnectivity(online)
        val recovery = ConnectivityRecovery(ObserveConnectivityRestoredUseCase(connectivity))
        var restored = 0
        recovery.launchIn(backgroundScope) { restored++ }

        recovery.setVisible(true)
        advanceTimeBy(5_000)

        assertEquals(0, restored)
    }

    @Test
    fun recovery_returningToScreen_whileOnline_triggersOnce() = runTest {
        val connectivity = FakeConnectivity(online)
        val recovery = ConnectivityRecovery(ObserveConnectivityRestoredUseCase(connectivity))
        var restored = 0
        recovery.launchIn(backgroundScope) { restored++ }
        recovery.setVisible(true)
        runCurrent()

        // Tab switch away and back.
        recovery.setVisible(false)
        runCurrent()
        recovery.setVisible(true)
        advanceTimeBy(5_000)

        assertEquals(1, restored)
    }

    @Test
    fun recovery_returningWhileOffline_waitsForTheTransition() = runTest {
        val connectivity = FakeConnectivity(online)
        val recovery = ConnectivityRecovery(ObserveConnectivityRestoredUseCase(connectivity))
        var restored = 0
        recovery.launchIn(backgroundScope) { restored++ }
        recovery.setVisible(true)
        runCurrent()
        recovery.setVisible(false)
        connectivity.status.value = offline
        runCurrent()

        recovery.setVisible(true)
        advanceTimeBy(5_000)
        assertEquals(0, restored)

        connectivity.status.value = online
        advanceTimeBy(2_000)
        assertEquals(1, restored)
    }

    // --- global status ---

    @Test
    fun appConnectivity_prefersNetworkProblems_thenApiHealth() = runTest {
        val connectivity = FakeConnectivity(offline)
        val api = FakeApiStatus()
        val status = ObserveAppConnectivityUseCase(connectivity, api)

        api.apiStatus.value = ApiStatus.Degraded
        assertEquals(AppConnectivity.Offline, status().first())

        connectivity.status.value = noInternet
        assertEquals(AppConnectivity.NoInternet, status().first())

        // Connected says nothing about the API: a degraded API is still reported.
        connectivity.status.value = online
        assertEquals(AppConnectivity.ApiDegraded, status().first())

        api.apiStatus.value = ApiStatus.Reachable
        assertEquals(AppConnectivity.Normal, status().first())

        api.apiStatus.value = ApiStatus.Unknown
        assertEquals(AppConnectivity.Normal, status().first())
    }

    @Test
    fun appConnectivity_timeoutIsNeutral_notServerDegraded_andRecovers() = runTest {
        val connectivity = FakeConnectivity(online)
        val api = FakeApiStatus()
        val status = ObserveAppConnectivityUseCase(connectivity, api)

        api.apiStatus.value = ApiStatus.Unstable
        assertEquals(AppConnectivity.Unstable, status().first())

        api.apiStatus.value = ApiStatus.Degraded
        assertEquals(AppConnectivity.ApiDegraded, status().first())

        api.apiStatus.value = ApiStatus.Reachable
        assertEquals(AppConnectivity.Normal, status().first())

        // Offline still wins over a timeout recorded earlier.
        api.apiStatus.value = ApiStatus.Unstable
        connectivity.status.value = offline
        assertEquals(AppConnectivity.Offline, status().first())
    }
}
