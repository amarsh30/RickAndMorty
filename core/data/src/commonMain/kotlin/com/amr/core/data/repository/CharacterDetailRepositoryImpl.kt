package com.amr.core.data.repository

import com.amr.core.data.concurrency.SingleFlight
import com.amr.core.data.error.toDataError
import com.amr.core.data.local.datasource.CharacterLookupLocalDataSource
import com.amr.core.data.mapper.toDomain
import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.data.remote.retry.RemoteCalls
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.domain.repository.CharacterDetailRepository
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.flow.Flow

class CharacterDetailRepositoryImpl(
    private val remoteDataSource: CharacterRemoteDataSource,
    private val localDataSource: CharacterLookupLocalDataSource,
    /** Required: production passes the app-scoped instance from Koin. */
    private val singleFlight: SingleFlight,
    private val remoteCalls: RemoteCalls = RemoteCalls()
) : CharacterDetailRepository {

    override fun observeCharacter(id: Int): Flow<Character?> =
        localDataSource.observeCharacter(id)

    override suspend fun refreshCharacter(id: Int) = singleFlight.run("character:detail:$id") {
        val dto = try {
            remoteCalls.get { remoteDataSource.getCharacter(id) }
        } catch (e: NetworkException) {
            throw e.toDataError()
        }

        // Guards against a response for a different resource.
        if (dto.id != id) throw DataError.InvalidData()

        localDataSource.saveFetchedCharacters(listOf(dto.toDomain()))
    }
}
