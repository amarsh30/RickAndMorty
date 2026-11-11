package com.amr.core.data.repository

import com.amr.core.data.error.toDataError
import com.amr.core.data.local.datasource.CharacterLookupLocalDataSource
import com.amr.core.data.mapper.toDomain
import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.model.CharacterFilter
import com.amr.core.domain.repository.CharacterSearchRepository
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Local-first search: [observeSearchResults] reads only Room, [searchRemote]
 * adds API matches to Room. Remote results never touch the paginated list's
 * membership, order or paging metadata. Only the first result page is fetched.
 */
class CharacterSearchRepositoryImpl(
    private val remoteDataSource: CharacterRemoteDataSource,
    private val localDataSource: CharacterLookupLocalDataSource
) : CharacterSearchRepository {

    override fun observeSearchResults(filter: CharacterFilter): Flow<List<Character>> {
        if (filter.isEmpty) return flowOf(emptyList())

        return localDataSource.observeSearch(
            name = filter.name.ifBlank { null },
            status = filter.status?.apiValue,
            species = filter.species.ifBlank { null },
            gender = filter.gender?.apiValue
        )
    }

    override suspend fun searchRemote(filter: CharacterFilter) {
        if (filter.isEmpty) return

        val response = try {
            remoteDataSource.searchCharacters(
                name = filter.name.ifBlank { null },
                status = filter.status?.apiValue,
                species = filter.species.ifBlank { null },
                gender = filter.gender?.apiValue
            )
        } catch (e: NetworkException) {
            // The API answers 404 when nothing matches: an empty result, not a failure.
            if (e is NetworkException.NotFound) return
            throw e.toDataError()
        }

        if (response.info.count < 0) throw DataError.InvalidData()

        localDataSource.saveFetchedCharacters(response.results.map { it.toDomain() })
    }
}

/** Query value understood by both the API and the cached `status` column (case-insensitive). */
internal val CharacterFilter.Status.apiValue: String
    get() = when (this) {
        CharacterFilter.Status.ALIVE -> "alive"
        CharacterFilter.Status.DEAD -> "dead"
        CharacterFilter.Status.UNKNOWN -> "unknown"
    }

internal val CharacterFilter.Gender.apiValue: String
    get() = when (this) {
        CharacterFilter.Gender.FEMALE -> "female"
        CharacterFilter.Gender.MALE -> "male"
        CharacterFilter.Gender.GENDERLESS -> "genderless"
        CharacterFilter.Gender.UNKNOWN -> "unknown"
    }
