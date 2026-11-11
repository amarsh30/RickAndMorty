package com.amr.feature.character.presentation

import com.amr.core.domain.model.Character

data class CharacterUiState(
    val isLoading: Boolean = true,
    val characters: List<Character> = emptyList(),
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    /** True when the cache reports another page; load-more is only triggered then. */
    val canLoadMore: Boolean = false,
    /** True once the last page has been stored. */
    val isEndReached: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** Load-more failure shown in the list footer; separate from [errorMessage]. */
    val loadMoreErrorMessage: String? = null
)
