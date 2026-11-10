package com.amr.core.data.repository

import com.amr.core.data.error.toDataError
import com.amr.core.data.local.datasource.CharacterLocalDataSource
import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.data.mapper.toDomain
import com.amr.core.domain.model.Character
import com.amr.core.domain.repository.CharacterRepository
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.flow.Flow

class CharacterRepositoryImpl(
    private val remoteDataSource: CharacterRemoteDataSource,
    private val localDataSource: CharacterLocalDataSource
) : CharacterRepository {

    override fun observeCharacters(): Flow<List<Character>> {
        return localDataSource.observeCharacters()
    }

    override suspend fun refreshCharacters() {
        val characters = try {
            fetchRemoteCharacters()
        } catch (e: NetworkException) {
            throw e.toDataError()
        }

        localDataSource.saveCharacters(characters)
    }

    private suspend fun fetchRemoteCharacters(): List<Character> {
        val response = remoteDataSource.getCharacters()

        return response.results.map { characterDto ->
            characterDto.toDomain()
        }
    }
}
