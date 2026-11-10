package com.amr.core.data.repository

import com.amr.core.data.fake.FakeCharacterLocalDataSource
import com.amr.core.data.fake.FakeCharacterRemoteDataSource
import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.data.remote.dto.CharacterResponseDto
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import com.amr.core.network.exception.NetworkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit test untuk [CharacterRepositoryImpl].
 *
 * Repository hanya bergantung pada abstraksi (CharacterRemoteDataSource &
 * CharacterLocalDataSource) — prinsip Dependency Inversion — sehingga kita
 * cukup menyuntikkan fake tanpa Ktor, Room, maupun Koin.
 */
class CharacterRepositoryImplTest {

    private lateinit var remoteDataSource: FakeCharacterRemoteDataSource
    private lateinit var localDataSource: FakeCharacterLocalDataSource
    private lateinit var repository: CharacterRepositoryImpl

    private val cachedCharacters = listOf(
        Character(1, "Rick Sanchez", "Alive", "Human", "rick.png"),
        Character(2, "Morty Smith", "Alive", "Human", "morty.png")
    )

    @BeforeTest
    fun setUp() {
        remoteDataSource = FakeCharacterRemoteDataSource()
        localDataSource = FakeCharacterLocalDataSource()
        repository = CharacterRepositoryImpl(
            remoteDataSource = remoteDataSource,
            localDataSource = localDataSource
        )
    }

    @Test
    fun observeCharacters_initially_emitsEmptyList() = runTest {
        assertTrue(repository.observeCharacters().first().isEmpty())
    }

    @Test
    fun refreshCharacters_whenApiSucceeds_savesMappedDataToLocal() = runTest {
        repository.refreshCharacters()

        val expected = listOf(
            Character(1, "Rick Sanchez", "Alive", "Human", "https://example.com/rick.png"),
            Character(2, "Morty Smith", "Alive", "Human", "https://example.com/morty.png")
        )
        assertEquals(expected, repository.observeCharacters().first())
        assertEquals(1, remoteDataSource.callCount)
    }

    @Test
    fun refreshCharacters_whenApiSucceeds_replacesOldCache() = runTest {
        localDataSource.saveCharacters(cachedCharacters)
        remoteDataSource.response = CharacterResponseDto(
            results = listOf(
                CharacterDto(3, "Summer Smith", "Alive", "Human", "summer.png")
            )
        )

        repository.refreshCharacters()

        assertEquals(
            listOf(Character(3, "Summer Smith", "Alive", "Human", "summer.png")),
            repository.observeCharacters().first()
        )
    }

    @Test
    fun refreshCharacters_whenApiFails_throwsAndKeepsLocalCache() = runTest {
        localDataSource.saveCharacters(cachedCharacters)
        remoteDataSource.failWithNoInternet()

        val error = assertFailsWith<DataError.NoConnection> {
            repository.refreshCharacters()
        }

        assertIs<NetworkException.NoInternet>(error.cause)
        assertEquals(cachedCharacters, repository.observeCharacters().first())
    }

    @Test
    fun refreshCharacters_whenResponseInvalid_throwsInvalidDataAndKeepsLocalCache() = runTest {
        localDataSource.saveCharacters(cachedCharacters)
        val source = NetworkException.Serialization()
        remoteDataSource.errorToThrow = source

        val error = assertFailsWith<DataError.InvalidData> {
            repository.refreshCharacters()
        }

        assertSame(source, error.cause)
        assertEquals(cachedCharacters, repository.observeCharacters().first())
    }

    @Test
    fun refreshCharacters_whenCancelled_propagatesCancellationAndKeepsLocalCache() = runTest {
        localDataSource.saveCharacters(cachedCharacters)
        remoteDataSource.errorToThrow = CancellationException("cancelled")

        // CancellationException and DataError are disjoint types, so a passing
        // assertFailsWith here also proves cancellation was not mapped.
        assertFailsWith<CancellationException> {
            repository.refreshCharacters()
        }

        assertEquals(cachedCharacters, repository.observeCharacters().first())
    }
}
