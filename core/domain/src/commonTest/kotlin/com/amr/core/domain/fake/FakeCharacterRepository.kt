package com.amr.core.domain.fake

import com.amr.core.domain.model.Character
import com.amr.core.domain.model.PaginationState
import com.amr.core.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCharacterRepository : CharacterRepository {

    val characters = MutableStateFlow<List<Character>>(emptyList())
    val paginationState = MutableStateFlow<PaginationState>(PaginationState.Uninitialized)

    var refreshResult: List<Character> = emptyList()
    var errorToThrow: Exception? = null

    /** Jika diisi, loadNextPage() akan melempar exception ini. */
    var loadNextPageError: Exception? = null

    var refreshCount = 0
        private set

    var loadNextPageCount = 0
        private set

    override fun observeCharacters(): Flow<List<Character>> = characters

    override suspend fun refreshCharacters() {
        refreshCount++
        errorToThrow?.let { throw it }
        characters.value = refreshResult
    }

    override fun observePaginationState(): Flow<PaginationState> = paginationState

    override suspend fun loadNextPage() {
        loadNextPageCount++
        loadNextPageError?.let { throw it }
    }
}
