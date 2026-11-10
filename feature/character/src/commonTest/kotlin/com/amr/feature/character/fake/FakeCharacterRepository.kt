package com.amr.feature.character.fake

import com.amr.core.domain.model.Character
import com.amr.core.domain.repository.CharacterRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCharacterRepository : CharacterRepository {

    val characters = MutableStateFlow<List<Character>>(emptyList())

    var refreshResult: List<Character> = emptyList()
    var errorToThrow: Exception? = null

    /** Jika diisi, refresh akan menunggu sampai gate di-complete (untuk uji state isRefreshing). */
    var gate: CompletableDeferred<Unit>? = null

    var refreshCount = 0
        private set

    override fun observeCharacters(): Flow<List<Character>> = characters

    override suspend fun refreshCharacters() {
        refreshCount++
        gate?.await()
        errorToThrow?.let { throw it }
        characters.value = refreshResult
    }
}
