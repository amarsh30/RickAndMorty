package com.amr.feature.character.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.usecase.ObserveCharacterDetailUseCase
import com.amr.core.domain.usecase.ObserveIsFavoriteUseCase
import com.amr.core.domain.usecase.RefreshCharacterDetailUseCase
import com.amr.core.domain.usecase.SetFavoriteUseCase
import com.amr.feature.character.presentation.GENERIC_ERROR_MESSAGE
import com.amr.feature.character.presentation.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class CharacterDetailUiState(
    val isLoading: Boolean = true,
    val character: Character? = null,
    val isFavorite: Boolean = false,
    val isRefreshing: Boolean = false,
    /** Shown full-screen when nothing is cached, otherwise above the cached detail. */
    val errorMessage: String? = null
)

/**
 * Shows the cached character (list, search or favorite cache) and refreshes it
 * from the API once on open. Room stays the source of the displayed data.
 */
class CharacterDetailViewModel(
    private val characterId: Int,
    private val observeCharacterDetail: ObserveCharacterDetailUseCase,
    private val refreshCharacterDetail: RefreshCharacterDetailUseCase,
    private val observeIsFavorite: ObserveIsFavoriteUseCase,
    private val setFavorite: SetFavoriteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterDetailUiState())
    val uiState: StateFlow<CharacterDetailUiState> = _uiState.asStateFlow()

    init {
        observe()
        refresh()
    }

    private fun observe() {
        viewModelScope.launch {
            combine(
                observeCharacterDetail(characterId),
                observeIsFavorite(characterId)
            ) { character, isFavorite -> character to isFavorite }
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = DATABASE_ERROR_MESSAGE
                    )
                }
                .collect { (character, isFavorite) ->
                    _uiState.value = _uiState.value.copy(
                        // Keep loading while nothing is cached and the first fetch is running.
                        isLoading = character == null && _uiState.value.isRefreshing,
                        character = character,
                        isFavorite = isFavorite
                    )
                }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, errorMessage = null)
            try {
                refreshCharacterDetail(characterId)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: DataError) {
                _uiState.value = _uiState.value.copy(errorMessage = exception.toUserMessage())
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = GENERIC_ERROR_MESSAGE)
            } finally {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    isLoading = false
                )
            }
        }
    }

    fun toggleFavorite() {
        val state = _uiState.value
        val character = state.character ?: return

        viewModelScope.launch {
            try {
                setFavorite(character, favorite = !state.isFavorite)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = FAVORITE_ERROR_MESSAGE)
            }
        }
    }

    private companion object {
        const val DATABASE_ERROR_MESSAGE = "Gagal membaca data tersimpan. Coba lagi."
        const val FAVORITE_ERROR_MESSAGE = "Gagal menyimpan favorit. Coba lagi."
    }
}
