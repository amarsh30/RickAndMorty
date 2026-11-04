package com.amr.core.data.repository

import com.amr.core.data.fake.FakeCharacterLocalDataSource
import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.data.remote.dto.CharacterResponseDto
import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.network.exception.NetworkException
import kotlin.test.assertFailsWith

class CharacterRepositoryImplTest {

    @Test
    fun refreshCharacters_whenApiFails_keepsLocalCache() = runBlocking {

        // 1. Siapkan cache yang sudah ada
        val localDataSource = FakeCharacterLocalDataSource()

        val cachedCharacters = listOf(
            Character(
                id = 1,
                name = "Rick Sanchez",
                status = "Alive",
                species = "Human",
                imageUrl = "rick.png"
            ),
            Character(
                id = 2,
                name = "Morty Smith",
                status = "Alive",
                species = "Human",
                imageUrl = "morty.png"
            )
        )

        localDataSource.saveCharacters(cachedCharacters)

        // 2. Fake API yang selalu gagal
        val remoteDataSource = object : CharacterRemoteDataSource {

            override suspend fun getCharacters(): CharacterResponseDto {
                throw NetworkException.NoInternet()
            }
        }

        // 3. Buat Repository
        val repository = CharacterRepositoryImpl(
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource
        )

        // 4. Pastikan refresh melempar error
        assertFailsWith<NetworkException.NoInternet> {
            repository.refreshCharacters()
        }

        // 5. Periksa cache setelah refresh gagal
        val result = localDataSource.observeCharacters().first()

        assertEquals(cachedCharacters, result)
    }
}