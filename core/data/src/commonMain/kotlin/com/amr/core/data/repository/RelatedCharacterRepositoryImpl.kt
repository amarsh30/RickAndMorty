package com.amr.core.data.repository

import com.amr.core.data.error.toDataError
import com.amr.core.data.local.datasource.CharacterLookupLocalDataSource
import com.amr.core.data.mapper.toDomain
import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.repository.RelatedCharacterRepository
import com.amr.core.domain.repository.RelatedCharacterRepository.Companion.MAX_BATCH_SIZE
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Must be a single instance: it remembers in-flight and unknown ids across callers.
 * Found characters are not remembered here; the cache is the record of them, so
 * a character evicted from the bounded search cache can be fetched again.
 */
class RelatedCharacterRepositoryImpl(
    private val remoteDataSource: CharacterRemoteDataSource,
    private val localDataSource: CharacterLookupLocalDataSource
) : RelatedCharacterRepository {

    private val lock = Mutex()
    private val inFlight = mutableSetOf<Int>()
    /** Ids the API answered without (it drops unknown ids); not requested again. */
    private val unknownIds = mutableSetOf<Int>()

    override fun observeCharacters(ids: List<Int>): Flow<List<Character>> {
        if (ids.isEmpty()) return flowOf(emptyList())
        val order = ids.withIndex().associate { (index, id) -> id to index }
        return localDataSource.observeCharacters(ids)
            .map { characters -> characters.sortedBy { order.getValue(it.id) } }
    }

    override suspend fun fetchMissing(ids: List<Int>) {
        val claimed = lock.withLock {
            val cached = localDataSource.getCachedIds(ids)
            ids.distinct()
                .filter { it !in cached && it !in inFlight && it !in unknownIds }
                .also { inFlight += it }
        }
        if (claimed.isEmpty()) return

        try {
            claimed.chunked(MAX_BATCH_SIZE).forEach { batch -> fetchBatch(batch) }
        } finally {
            // Releases every claimed id, also after a failure or cancellation, so a retry can claim it.
            lock.withLock { inFlight -= claimed.toSet() }
        }
    }

    private suspend fun fetchBatch(batch: List<Int>) {
        val dtos = try {
            remoteDataSource.getCharactersByIds(batch)
        } catch (e: NetworkException) {
            throw e.toDataError()
        }
        // Guards against characters that were not asked for.
        if (dtos.any { it.id !in batch }) throw DataError.InvalidData()

        localDataSource.saveFetchedCharacters(dtos.map { it.toDomain() })

        val found = dtos.map { it.id }.toSet()
        lock.withLock { unknownIds += batch.filterNot { it in found } }
    }
}
