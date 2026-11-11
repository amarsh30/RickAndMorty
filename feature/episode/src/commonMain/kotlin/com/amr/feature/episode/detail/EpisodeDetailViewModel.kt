package com.amr.feature.episode.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.Episode
import com.amr.core.domain.usecase.ConnectivityRecovery
import com.amr.core.domain.usecase.FetchRelatedCharactersUseCase
import com.amr.core.domain.usecase.ObserveConnectivityRestoredUseCase
import com.amr.core.domain.usecase.ObserveEpisodeCharacterIdsUseCase
import com.amr.core.domain.usecase.ObserveEpisodeDetailUseCase
import com.amr.core.domain.usecase.ObserveRelatedCharactersUseCase
import com.amr.core.domain.usecase.RefreshEpisodeDetailUseCase
import com.amr.feature.episode.presentation.DATABASE_ERROR_MESSAGE
import com.amr.feature.episode.presentation.isConnectivityProblem
import com.amr.feature.episode.presentation.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EpisodeDetailUiState(
    val isLoading: Boolean = true,
    val episode: Episode? = null,
    val isRefreshing: Boolean = false,
    /** Full-screen when nothing is cached, otherwise above the cached detail. */
    val errorMessage: String? = null,
    val characterCount: Int = 0,
    /** Cached characters within the visible window, in API order. */
    val characters: List<Character> = emptyList(),
    val isLoadingCharacters: Boolean = false,
    val charactersErrorMessage: String? = null,
    /** False once every character id is inside the visible window. */
    val canShowMoreCharacters: Boolean = false
)

/**
 * Shows the cached episode and refreshes it once on open. Characters are loaded
 * in windows of [CHARACTER_PAGE_SIZE]: only ids inside the window are fetched,
 * and the window grows when the list nears its end.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeDetailViewModel(
    private val episodeId: Int,
    private val observeEpisodeDetail: ObserveEpisodeDetailUseCase,
    private val observeCharacterIds: ObserveEpisodeCharacterIdsUseCase,
    private val refreshEpisodeDetail: RefreshEpisodeDetailUseCase,
    private val observeRelatedCharacters: ObserveRelatedCharactersUseCase,
    private val fetchRelatedCharacters: FetchRelatedCharactersUseCase,
    connectivityRestored: ObserveConnectivityRestoredUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EpisodeDetailUiState())
    val uiState: StateFlow<EpisodeDetailUiState> = _uiState.asStateFlow()

    private val windowSize = MutableStateFlow(CHARACTER_PAGE_SIZE)
    private val visibleIds = combine(observeCharacterIds(episodeId), windowSize) { ids, size ->
        _uiState.update { it.copy(characterCount = ids.size, canShowMoreCharacters = size < ids.size) }
        ids.take(size)
    }.distinctUntilChanged()

    /** Operations that last failed for a connectivity reason; retried once the network returns. */
    private var refreshPending = false
    private var charactersPending = false

    private val recovery = ConnectivityRecovery(connectivityRestored)

    init {
        observeDetail()
        observeCharacters()
        refresh()
        recovery.launchIn(viewModelScope, ::onConnectivityRestored)
    }

    /** Called by the screen: recovery only runs while the detail is visible. */
    fun setVisible(isVisible: Boolean) = recovery.setVisible(isVisible)

    internal fun onConnectivityRestored() {
        if (refreshPending) refresh()
        // Retries the failed character window only; the window does not grow.
        if (charactersPending) loadMoreCharacters()
    }

    private fun observeDetail() {
        viewModelScope.launch {
            observeEpisodeDetail(episodeId)
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    _uiState.update { it.copy(isLoading = false, errorMessage = DATABASE_ERROR_MESSAGE) }
                }
                .collect { episode ->
                    _uiState.update {
                        // Keep loading while nothing is cached and the first fetch is running.
                        it.copy(episode = episode, isLoading = episode == null && it.isRefreshing)
                    }
                }
        }
    }

    private fun observeCharacters() {
        viewModelScope.launch {
            visibleIds
                .flatMapLatest { ids -> observeRelatedCharacters(ids) }
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    _uiState.update { it.copy(charactersErrorMessage = DATABASE_ERROR_MESSAGE) }
                }
                .collect { characters -> _uiState.update { it.copy(characters = characters) } }
        }
        viewModelScope.launch {
            visibleIds.collect { ids -> fetchCharacters(ids) }
        }
    }

    private suspend fun fetchCharacters(ids: List<Int>) {
        if (ids.isEmpty()) return
        _uiState.update { it.copy(isLoadingCharacters = true, charactersErrorMessage = null) }
        try {
            fetchRelatedCharacters(ids)
            charactersPending = false
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            charactersPending = exception.isConnectivityProblem
            _uiState.update { it.copy(charactersErrorMessage = exception.toUserMessage()) }
        } finally {
            _uiState.update { it.copy(isLoadingCharacters = false) }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return

        // Set before launching so a second call is rejected even before the coroutine starts.
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                refreshEpisodeDetail(episodeId)
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
     * Called near the end of the character list and by its retry button: retries a
     * failed window, otherwise widens the window. Ignored while a fetch is running.
     */
    fun loadMoreCharacters() {
        val state = _uiState.value
        if (state.isLoadingCharacters) return

        if (state.charactersErrorMessage != null) {
            viewModelScope.launch { fetchCharacters(visibleIds.first()) }
        } else if (state.canShowMoreCharacters) {
            windowSize.update { it + CHARACTER_PAGE_SIZE }
        }
    }

    companion object {
        const val CHARACTER_PAGE_SIZE = 20
    }
}
