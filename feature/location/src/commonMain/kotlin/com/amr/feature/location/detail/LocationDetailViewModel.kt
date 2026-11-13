package com.amr.feature.location.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.Location
import com.amr.core.domain.usecase.ConnectivityRecovery
import com.amr.core.domain.usecase.FetchRelatedCharactersUseCase
import com.amr.core.domain.usecase.ObserveConnectivityRestoredUseCase
import com.amr.core.domain.usecase.ObserveLocationDetailUseCase
import com.amr.core.domain.usecase.ObserveLocationResidentIdsUseCase
import com.amr.core.domain.usecase.ObserveRelatedCharactersUseCase
import com.amr.core.domain.usecase.RefreshLocationDetailUseCase
import com.amr.feature.location.presentation.DATABASE_ERROR_MESSAGE
import com.amr.feature.location.presentation.isConnectivityProblem
import com.amr.feature.location.presentation.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LocationDetailUiState(
    val isLoading: Boolean = true,
    val location: Location? = null,
    val isRefreshing: Boolean = false,
    /** Full-screen when nothing is cached, otherwise above the cached detail. */
    val errorMessage: String? = null,
    val residentCount: Int = 0,
    /** Cached residents within the visible window, in API order. */
    val residents: List<Character> = emptyList(),
    val isLoadingResidents: Boolean = false,
    val residentsErrorMessage: String? = null,
    /** False once every resident id is inside the visible window. */
    val canShowMoreResidents: Boolean = false
)

/**
 * Shows the cached location and refreshes it once on open. Residents are loaded
 * in windows of [RESIDENT_PAGE_SIZE]: only ids inside the window are fetched,
 * and the window grows when the list nears its end.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationDetailViewModel(
    private val locationId: Int,
    private val observeLocationDetail: ObserveLocationDetailUseCase,
    private val observeResidentIds: ObserveLocationResidentIdsUseCase,
    private val refreshLocationDetail: RefreshLocationDetailUseCase,
    private val observeRelatedCharacters: ObserveRelatedCharactersUseCase,
    private val fetchRelatedCharacters: FetchRelatedCharactersUseCase,
    connectivityRestored: ObserveConnectivityRestoredUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationDetailUiState())
    val uiState: StateFlow<LocationDetailUiState> = _uiState.asStateFlow()

    private val windowSize = MutableStateFlow(RESIDENT_PAGE_SIZE)
    /** Bumped by a retry after the resident-id query failed, to run that query again. */
    private val idQueryAttempt = MutableStateFlow(0)
    private var idQueryFailed = false

    /**
     * One collection of the resident-id query, shared by the resident observer, the
     * fetcher and the retry (null until the first ids arrive). Started with the
     * ViewModel, like the collectors that read it, and stopped with viewModelScope.
     * A failed query ends only the current attempt, so a retry can start a new one.
     */
    private val visibleIds = idQueryAttempt
        .flatMapLatest {
            combine(observeResidentIds(locationId), windowSize) { ids, size ->
                _uiState.update { it.copy(residentCount = ids.size, canShowMoreResidents = size < ids.size) }
                ids.take(size)
            }.catch { exception ->
                if (exception is CancellationException) throw exception
                idQueryFailed = true
                // Same database-error state as the resident observer shows.
                _uiState.update { it.copy(residentsErrorMessage = DATABASE_ERROR_MESSAGE) }
            }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    /** Operations that last failed for a connectivity reason; retried once the network returns. */
    private var refreshPending = false
    private var residentsPending = false

    private val recovery = ConnectivityRecovery(connectivityRestored)

    init {
        observeDetail()
        observeResidents()
        refresh()
        recovery.launchIn(viewModelScope, ::onConnectivityRestored)
    }

    /** Called by the screen: recovery only runs while the detail is visible. */
    fun setVisible(isVisible: Boolean) = recovery.setVisible(isVisible)

    internal fun onConnectivityRestored() {
        if (refreshPending) refresh()
        // Retries the failed resident window only; the window does not grow.
        if (residentsPending) loadMoreResidents()
    }

    private fun observeDetail() {
        viewModelScope.launch {
            observeLocationDetail(locationId)
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    _uiState.update { it.copy(isLoading = false, errorMessage = DATABASE_ERROR_MESSAGE) }
                }
                .collect { location ->
                    _uiState.update {
                        // Keep loading while nothing is cached and the first fetch is running.
                        it.copy(location = location, isLoading = location == null && it.isRefreshing)
                    }
                }
        }
    }

    private fun observeResidents() {
        viewModelScope.launch {
            visibleIds
                .filterNotNull()
                .flatMapLatest { ids -> observeRelatedCharacters(ids) }
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    _uiState.update { it.copy(residentsErrorMessage = DATABASE_ERROR_MESSAGE) }
                }
                .collect { residents -> _uiState.update { it.copy(residents = residents) } }
        }
        viewModelScope.launch {
            visibleIds.filterNotNull().collect { ids -> fetchResidents(ids) }
        }
    }

    private suspend fun fetchResidents(ids: List<Int>) {
        if (ids.isEmpty()) return
        _uiState.update { it.copy(isLoadingResidents = true, residentsErrorMessage = null) }
        try {
            fetchRelatedCharacters(ids)
            residentsPending = false
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            residentsPending = exception.isConnectivityProblem
            _uiState.update { it.copy(residentsErrorMessage = exception.toUserMessage()) }
        } finally {
            _uiState.update { it.copy(isLoadingResidents = false) }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return

        // Set before launching so a second call is rejected even before the coroutine starts.
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                refreshLocationDetail(locationId)
                refreshPending = false
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                refreshPending = exception.isConnectivityProblem
                _uiState.update { it.copy(errorMessage = exception.toUserMessage()) }
            } finally {
                _uiState.update { it.copy(isRefreshing = false, isLoading = false) }
            }
        }
    }

    /**
     * Called near the end of the resident list and by its retry button: retries a
     * failed window, otherwise widens the window. Ignored while a fetch is running.
     */
    fun loadMoreResidents() {
        val state = _uiState.value
        if (state.isLoadingResidents) return

        if (state.residentsErrorMessage != null) {
            if (idQueryFailed) {
                // Run the id query again (one new subscription); the fetch follows its ids.
                idQueryFailed = false
                _uiState.update { it.copy(residentsErrorMessage = null) }
                idQueryAttempt.update { it + 1 }
            } else {
                // Ids are known: retry the failed window right away, without waiting.
                val ids = visibleIds.value ?: return
                viewModelScope.launch { fetchResidents(ids) }
            }
        } else if (state.canShowMoreResidents) {
            windowSize.update { it + RESIDENT_PAGE_SIZE }
        }
    }

    companion object {
        const val RESIDENT_PAGE_SIZE = 20
    }
}
