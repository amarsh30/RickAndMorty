package com.amr.core.domain.repository

import com.amr.core.domain.model.Character
import com.amr.core.domain.model.PaginationState
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {

    fun observeCharacters(): Flow<List<Character>>

    suspend fun refreshCharacters()

    fun observePaginationState(): Flow<PaginationState>

    /**
     * Loads the page after the ones already cached; the result arrives through
     * [observeCharacters]. Does nothing when [PaginationState.EndReached].
     *
     * @throws com.amr.core.domain.error.DataError when the page cannot be loaded.
     */
    suspend fun loadNextPage()
}
