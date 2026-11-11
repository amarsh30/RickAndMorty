package com.amr.feature.episode.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amr.core.domain.model.Episode
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.usecase.ConnectivityRecovery
import com.amr.core.domain.usecase.LoadNextEpisodesPageUseCase
import com.amr.core.domain.usecase.ObserveConnectivityRestoredUseCase
import com.amr.core.domain.usecase.ObserveEpisodePaginationStateUseCase
import com.amr.core.domain.usecase.ObserveEpisodesUseCase
import com.amr.core.domain.usecase.RefreshEpisodesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EpisodeListUiState(
    val isLoading: Boolean = true,
    val episodes: List<Episode> = emptyList(),
    /** Full-screen when nothing is cached, otherwise a warning above the cached list. */
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val canLoadMore: Boolean = false,
    val isEndReached: Boolean = false,
    val isLoadingMore: Boolean = false,
    val loadMoreErrorMessage: String? = null
)

/** Room is the source of the list; refresh and load-more only write to it. */
class EpisodeListViewModel(
    private val observeEpisodes: ObserveEpisodesUseCase,
    private val observePaginationState: ObserveEpisodePaginationStateUseCase,
    private val refreshEpisodes: RefreshEpisodesUseCase,
    private val loadNextEpisodesPage: LoadNextEpisodesPageUseCase,
    connectivityRestored: ObserveConnectivityRestoredUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpisodeListUiState())
    val uiState: StateFlow<EpisodeListUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var paginationJob: Job? = null

    /** The operation that last failed for a connectivity reason; retried once the network returns. */
    private var pendingRecovery: PendingOperation? = null

    private val recovery = ConnectivityRecovery(connectivityRestored)

    init {
        startObserving()
        refresh()
        recovery.launchIn(viewModelScope, ::onConnectivityRestored)
    }

    /** Called by the screen: recovery only runs while the list is visible. */
    fun setVisible(isVisible: Boolean) = recovery.setVisible(isVisible)

    /** Retries only what last failed for a connectivity reason; guards and single-flight merge duplicates. */
    internal fun onConnectivityRestored() {
        when (pendingRecovery) {
            PendingOperation.REFRESH -> refresh()
            PendingOperation.LOAD_MORE -> loadMore()
            null -> Unit
        }
    }

    private fun startObserving() {
        if (observeJob?.isActive != true) {
            observeJob = viewModelScope.launch {
                observeEpisodes()
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        // Keeps what is shown; the next refresh restarts this collector.
                        _uiState.update { it.copy(isLoading = false, errorMessage = DATABASE_ERROR_MESSAGE) }
                    }
                    .collect { episodes ->
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                episodes = episodes,
                                // A failure reason is replaced by the stale warning once cached data exists.
                                errorMessage = if (state.errorMessage != null && episodes.isNotEmpty() &&
                                    state.errorMessage != DATABASE_ERROR_MESSAGE
                                ) STALE_DATA_MESSAGE else state.errorMessage
                            )
                        }
                    }
            }
        }
        if (paginationJob?.isActive != true) {
            paginationJob = viewModelScope.launch {
                observePaginationState()
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        _uiState.update { it.copy(canLoadMore = false, isEndReached = false) }
                    }
                    .collect { state ->
                        _uiState.update {
                            it.copy(
                                canLoadMore = state == PaginationState.HasMore,
                                isEndReached = state == PaginationState.EndReached
                            )
                        }
                    }
            }
        }
    }

    fun refresh() {
        startObserving()
        if (_uiState.value.isRefreshing) return

        // Set before launching so a second call is rejected even before the coroutine starts.
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null, loadMoreErrorMessage = null) }
        viewModelScope.launch {
            try {
                refreshEpisodes()
                // A fresh first page also supersedes a failed load-more.
                pendingRecovery = null
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (exception.isConnectivityProblem) pendingRecovery = PendingOperation.REFRESH
                _uiState.update { state ->
                    state.copy(
                        errorMessage = if (state.episodes.isEmpty()) exception.toUserMessage() else STALE_DATA_MESSAGE
                    )
                }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    /** Ignored while refreshing, already loading, at the end, or before page 1 is known. */
    fun loadMore() {
        val state = _uiState.value
        if (!state.canLoadMore || state.isLoadingMore || state.isRefreshing) return

        _uiState.update { it.copy(isLoadingMore = true, loadMoreErrorMessage = null) }
        viewModelScope.launch {
            try {
                loadNextEpisodesPage()
                if (pendingRecovery == PendingOperation.LOAD_MORE) pendingRecovery = null
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                // A pending refresh stays first: it rebuilds the list anyway.
                if (exception.isConnectivityProblem && pendingRecovery == null) {
                    pendingRecovery = PendingOperation.LOAD_MORE
                }
                _uiState.update { it.copy(loadMoreErrorMessage = exception.toUserMessage()) }
            } finally {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    private enum class PendingOperation { REFRESH, LOAD_MORE }
}
