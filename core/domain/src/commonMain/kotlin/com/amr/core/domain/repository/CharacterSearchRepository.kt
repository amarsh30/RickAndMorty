package com.amr.core.domain.repository

import com.amr.core.domain.model.Character
import com.amr.core.domain.model.CharacterFilter
import kotlinx.coroutines.flow.Flow

interface CharacterSearchRepository {

    /**
     * Cached characters matching [filter], without duplicates, ordered by id.
     * Works offline; may be incomplete until [searchRemote] has run for the filter.
     */
    fun observeSearchResults(filter: CharacterFilter): Flow<List<Character>>

    /**
     * Fetches matches from the API into the local cache; results arrive through
     * [observeSearchResults]. "No match" is not an error.
     *
     * @throws com.amr.core.domain.error.DataError when the search cannot be completed.
     */
    suspend fun searchRemote(filter: CharacterFilter)
}
