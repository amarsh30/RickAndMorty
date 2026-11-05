package com.amr.feature.character.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amr.core.domain.usecase.ObserveCharactersUseCase
import com.amr.core.domain.usecase.RefreshCharactersUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
                        characters = characters
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
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Gagal memperbarui karakter"
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false
                )
            }
        }
    }
}