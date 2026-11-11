
package com.amr.feature.character.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.usecase.LoadNextPageCharactersUseCase
import com.amr.core.domain.usecase.ObserveCharactersUseCase
import com.amr.core.domain.usecase.ObservePaginationStateUseCase
import com.amr.core.domain.usecase.RefreshCharactersUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CharacterViewModel(
    private val observeCharactersUseCase: ObserveCharactersUseCase,
    private val refreshCharactersUseCase: RefreshCharactersUseCase,
    private val observePaginationStateUseCase: ObservePaginationStateUseCase,
    private val loadNextPageCharactersUseCase: LoadNextPageCharactersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterUiState())

    val uiState: StateFlow<CharacterUiState> =
        _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var paginationJob: Job? = null

    /**
     * True while the Room observation has failed and not yet emitted again.
     * errorMessage alone cannot tell a database error from a refresh error.
     */
    private var isObservationFailed = false

    /**
     * Reason of the last refresh failure, null when the last refresh succeeded or is running.
     * Kept while the database error is shown so it can be restored once Room recovers.
     */
    private var lastRefreshErrorReason: String? = null

    init {
        observeCharacters()
        observePaginationState()
        refreshCharacters()
    }

    private fun observePaginationState() {
        paginationJob = viewModelScope.launch {
            observePaginationStateUseCase()
                .catch { exception ->
                    if (exception is CancellationException) throw exception

                    // Unreadable metadata: stop auto load-more; a refresh rewrites the
                    // metadata and restarts this collector.
                    _uiState.value = _uiState.value.copy(
                        canLoadMore = false,
                        isEndReached = false
                    )
                }
                .collect { state ->
                    _uiState.value = _uiState.value.copy(
                        canLoadMore = state == PaginationState.HasMore,
                        isEndReached = state == PaginationState.EndReached
                    )
                }
        }
    }

    private fun observeCharacters() {
        observeJob = viewModelScope.launch {
            observeCharactersUseCase()
                .catch { exception ->
                    if (exception is CancellationException) throw exception

                    // Keep the characters already shown; the collector ends here and is
                    // restarted by the next refreshCharacters() call.
                    isObservationFailed = true
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = DATABASE_ERROR_MESSAGE
                    )
                }
                .collect { characters ->
                    val currentState = _uiState.value

                    // A Room emission after an observation failure means Room recovered, so
                    // its error is replaced by the last refresh failure, if any. Otherwise
                    // errorMessage comes from a failed refresh and must not be dropped; if the
                    // cache arrives after that failure, switch to the stale-data warning.
                    val errorMessage = when {
                        isObservationFailed -> lastRefreshErrorReason?.let { reason ->
                            refreshErrorMessage(reason, characters)
                        }
                        currentState.errorMessage != null && characters.isNotEmpty() ->
                            STALE_DATA_MESSAGE
                        else -> currentState.errorMessage
                    }
                    isObservationFailed = false

                    _uiState.value = currentState.copy(
                        isLoading = false,
                        characters = characters,
                        errorMessage = errorMessage
                    )
                }
        }
    }

    fun refreshCharacters() {
        // Restart a failed observation; an active collector is never duplicated.
        if (observeJob?.isActive != true) observeCharacters()
        if (paginationJob?.isActive != true) observePaginationState()

        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            lastRefreshErrorReason = null

            // A database error stays until Room emits again, even if the refresh succeeds.
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                errorMessage = if (isObservationFailed) _uiState.value.errorMessage else null,
                loadMoreErrorMessage = null
            )

            try {
                refreshCharactersUseCase()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: DataError) {
                showRefreshError(reason = exception.toMessage())
            } catch (exception: Exception) {
                // Fallback for failures outside the data error contract (e.g. local database).
                showRefreshError(reason = GENERIC_ERROR_MESSAGE)
            } finally {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false
                )
            }
        }
    }

    /**
     * Loads the next page when one exists. Ignored while a refresh or another
     * load-more is running, after the last page, and before the first page is known.
     * The repository serializes it with refresh, so a racing refresh cannot corrupt
     * the page order.
     */
    fun loadMoreCharacters() {
        val currentState = _uiState.value
        if (
            !currentState.canLoadMore ||
            currentState.isLoadingMore ||
            currentState.isRefreshing
        ) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingMore = true,
                loadMoreErrorMessage = null
            )

            try {
                loadNextPageCharactersUseCase()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: DataError) {
                _uiState.value = _uiState.value.copy(loadMoreErrorMessage = exception.toMessage())
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(loadMoreErrorMessage = GENERIC_ERROR_MESSAGE)
            } finally {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    /**
     * Shows [reason] only when there is no cached data to fall back on.
     * With cached data, the list stays visible with a generic stale-data warning.
     */
    private fun showRefreshError(reason: String) {
        lastRefreshErrorReason = reason

        // The database error has priority: it is the reason the list cannot update.
        if (isObservationFailed) return

        val currentState = _uiState.value

        _uiState.value = currentState.copy(
            errorMessage = refreshErrorMessage(reason, currentState.characters)
        )
    }

    private fun refreshErrorMessage(reason: String, characters: List<Character>): String {
        return if (characters.isEmpty()) reason else STALE_DATA_MESSAGE
    }

    private fun DataError.toMessage(): String {
        return when (this) {
            is DataError.NoConnection -> "Gagal memuat karakter. Periksa koneksi internet."
            is DataError.Timeout -> "Server terlalu lama merespons. Coba lagi."
            is DataError.Unauthorized -> "Akses ke data ditolak."
            is DataError.NotFound -> "Data karakter tidak ditemukan."
            is DataError.Server -> "Server sedang bermasalah. Coba lagi nanti."
            is DataError.InvalidData -> "Data dari server tidak dapat dibaca."
            is DataError.Unknown -> GENERIC_ERROR_MESSAGE
        }
    }

    private companion object {
        const val GENERIC_ERROR_MESSAGE = "Terjadi kesalahan. Coba lagi."
        const val STALE_DATA_MESSAGE = "Gagal memperbarui data. Menampilkan data tersimpan."
        const val DATABASE_ERROR_MESSAGE = "Gagal membaca data tersimpan. Coba lagi."
    }
}
