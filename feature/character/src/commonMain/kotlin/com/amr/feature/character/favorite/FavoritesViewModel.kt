package com.amr.feature.character.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amr.core.domain.model.Character
import com.amr.core.domain.usecase.ObserveFavoritesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val favorites: List<Character> = emptyList(),
    val errorMessage: String? = null
)

/** Favorites are local-only, so there is nothing to refresh from the network. */
class FavoritesViewModel(
    private val observeFavorites: ObserveFavoritesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeFavorites()
                .catch { exception ->
                    if (exception is CancellationException) throw exception
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Gagal membaca favorit tersimpan."
                    )
                }
                .collect { favorites ->
                    _uiState.value = FavoritesUiState(isLoading = false, favorites = favorites)
                }
        }
    }
}
