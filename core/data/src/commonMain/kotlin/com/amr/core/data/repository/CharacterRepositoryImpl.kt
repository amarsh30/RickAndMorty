package com.amr.core.data.repository

import com.amr.core.data.local.datasource.CharacterLocalDataSource
import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.data.mapper.toDomain
import com.amr.core.domain.model.Character
import com.amr.core.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow

class CharacterRepositoryImpl(
    private val remoteDataSource: CharacterRemoteDataSource,
    private val localDataSource: CharacterLocalDataSource
) : CharacterRepository {

    override fun observeCharacters(): Flow<List<Character>> {
        return localDataSource.observeCharacters()
    }

    override suspend fun refreshCharacters() {
        TODO("Waiting for LocalDataSource implementation")
    }

    private suspend fun fetchRemoteCharacters(): List<Character> {
        val response = remoteDataSource.getCharacters()

        return response.results.map { characterDto ->
            characterDto.toDomain()
        }
    }
}