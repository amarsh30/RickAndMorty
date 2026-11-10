
package com.amr.feature.character.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amr.core.domain.error.DataError
import com.amr.core.domain.usecase.ObserveCharactersUseCase
import com.amr.core.domain.usecase.RefreshCharactersUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CharacterViewModel(
    private val observeCharactersUseCase: ObserveCharactersUseCase,
    private val refreshCharactersUseCase: RefreshCharactersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterUiState())

    val uiState: StateFlow<CharacterUiState> =
        _uiState.asStateFlow()

    init {
        observeCharacters()
        refreshCharacters()
    }

    private fun observeCharacters() {
        viewModelScope.launch {
            observeCharactersUseCase()
                .collect { characters ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        characters = characters,
                        errorMessage = if (characters.isNotEmpty()) {
                            null
                        } else {
                            _uiState.value.errorMessage
                        }
                    )
                }
        }
    }

    fun refreshCharacters() {
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRefreshing = true,
                errorMessage = null
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
     * Shows [reason] only when there is no cached data to fall back on.
     * With cached data, the list stays visible with a generic stale-data warning.
     */
    private fun showRefreshError(reason: String) {
        val currentState = _uiState.value

        _uiState.value = currentState.copy(
            errorMessage = if (currentState.characters.isEmpty()) {
                reason
            } else {
                "Gagal memperbarui data. Menampilkan data tersimpan."
            }
        )
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
    }
}
