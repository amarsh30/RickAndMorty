package com.amr.feature.character.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.CharacterFilter
import com.amr.core.domain.usecase.ObserveCharacterSearchUseCase
import com.amr.core.domain.usecase.SearchCharactersRemoteUseCase
import com.amr.feature.character.presentation.GENERIC_ERROR_MESSAGE
import com.amr.feature.character.presentation.isConnectivityProblem
import com.amr.feature.character.presentation.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class CharacterSearchUiState(
    val filter: CharacterFilter = CharacterFilter(),
    val results: List<Character> = emptyList(),
    val isSearchingRemote: Boolean = false,
    /** The online search failed for a connectivity reason: results come from the cache only. */
    val isOfflineResult: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Local-first search: cached matches are shown as soon as the debounced filter
 * changes, then the API search adds its matches to the cache, which updates the
 * same list. A newer filter cancels the previous API request.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class CharacterSearchViewModel(
    private val observeCharacterSearch: ObserveCharacterSearchUseCase,
    private val searchCharactersRemote: SearchCharactersRemoteUseCase
) : ViewModel() {

    private val filter = MutableStateFlow(CharacterFilter())

    private val _uiState = MutableStateFlow(CharacterSearchUiState())
    val uiState: StateFlow<CharacterSearchUiState> = _uiState.asStateFlow()

    private var remoteJob: Job? = null

    init {
        viewModelScope.launch {
            filter
                .debounce(SEARCH_DEBOUNCE_MILLIS)
                .map { it.normalized() }
                .distinctUntilChanged()
                .flatMapLatest { current ->
                    // A new filter starts a new API search; the old one is cancelled.
                    runRemoteSearch(current)
                    observeCharacterSearch(current)
                }
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    _uiState.value = _uiState.value.copy(errorMessage = DATABASE_ERROR_MESSAGE)
                }
                .collect { results ->
                    _uiState.value = _uiState.value.copy(results = results)
                }
        }
    }

    fun onFilterChange(newFilter: CharacterFilter) {
        filter.value = newFilter
        _uiState.value = _uiState.value.copy(filter = newFilter)
    }

    /** Repeats the API search for the current filter, e.g. after reconnecting. */
    fun retry() {
        runRemoteSearch(filter.value.normalized())
    }

    private fun runRemoteSearch(current: CharacterFilter) {
        remoteJob?.cancel()

        if (current.isEmpty) {
            _uiState.value = _uiState.value.copy(
                isSearchingRemote = false,
                isOfflineResult = false,
                errorMessage = null
            )
            return
        }

        // Started lazily so remoteJob is assigned before the body can finish.
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            _uiState.value = _uiState.value.copy(
                isSearchingRemote = true,
                isOfflineResult = false,
                errorMessage = null
            )
            try {
                searchCharactersRemote(current)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: DataError) {
                _uiState.value = _uiState.value.copy(
                    isOfflineResult = exception.isConnectivityProblem,
                    errorMessage = if (exception.isConnectivityProblem) null else exception.toUserMessage()
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = GENERIC_ERROR_MESSAGE)
            } finally {
                // Only the latest search owns the flag; a cancelled one was replaced.
                if (remoteJob === coroutineContext[Job]) {
                    _uiState.value = _uiState.value.copy(isSearchingRemote = false)
                }
            }
        }
        remoteJob = job
        job.start()
    }

    internal companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 400L
        const val DATABASE_ERROR_MESSAGE = "Gagal membaca data tersimpan. Coba lagi."
    }
}
