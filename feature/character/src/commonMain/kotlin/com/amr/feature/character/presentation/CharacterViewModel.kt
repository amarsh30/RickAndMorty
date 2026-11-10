
package com.amr.feature.character.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            } catch (exception: Exception) {
                val currentState = _uiState.value

                _uiState.value = currentState.copy(
                    errorMessage = if (currentState.characters.isEmpty()) {
                        "Gagal memuat karakter. Periksa koneksi internet."
                    } else {
                        "Gagal memperbarui data. Menampilkan data tersimpan."
                    }
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false
                )
            }
        }
    }
}
