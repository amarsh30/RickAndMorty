package com.amr.feature.character.presentation

import com.amr.core.domain.model.Character

data class CharacterUiState(
    val isLoading: Boolean = true,
    val characters: List<Character> = emptyList(),
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false
)