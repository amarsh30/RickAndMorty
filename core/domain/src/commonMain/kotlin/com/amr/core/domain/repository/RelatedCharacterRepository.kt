package com.amr.core.domain.repository

import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.Flow

/**
 * Characters referenced by id from another resource (location residents,
 * episode cast). Fetched characters go to the bounded search cache, never
 * into the paginated character list.
 */
interface RelatedCharacterRepository {

    /** The cached characters among [ids], in the order of [ids]; uncached ids are skipped. */
    fun observeCharacters(ids: List<Int>): Flow<List<Character>>

    /**
     * Fetches the ids that are neither cached nor already requested, in batches of
     * at most [MAX_BATCH_SIZE]. Ids the API does not know are not requested again.
     *
     * @throws com.amr.core.domain.error.DataError when a batch cannot be fetched.
     */
    suspend fun fetchMissing(ids: List<Int>)

    companion object {
        const val MAX_BATCH_SIZE = 50
    }
}
