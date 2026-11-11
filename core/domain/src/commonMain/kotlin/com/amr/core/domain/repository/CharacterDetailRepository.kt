package com.amr.core.domain.repository

import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.Flow

interface CharacterDetailRepository {

    /** The cached character, or null while it is not cached anywhere. */
    fun observeCharacter(id: Int): Flow<Character?>

    /**
     * Fetches the character from the API into the cache; the result arrives
     * through [observeCharacter].
     *
     * @throws com.amr.core.domain.error.DataError when the character cannot be loaded.
     */
    suspend fun refreshCharacter(id: Int)
}
