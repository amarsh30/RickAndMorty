package com.amr.core.domain.usecase

import com.amr.core.domain.fake.FakeCharacterRepository
import com.amr.core.domain.model.Character
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Use case di domain layer murni Kotlin — tidak tahu soal Ktor, Room, atau Koin.
 * Cukup diuji dengan FakeCharacterRepository.
 */
class CharacterUseCasesTest {

    private lateinit var repository: FakeCharacterRepository

    private val rick = Character(1, "Rick Sanchez", "Alive", "Human", "rick.png")

    @BeforeTest
    fun setUp() {
        repository = FakeCharacterRepository()
    }

    @Test
    fun observeCharactersUseCase_emitsRepositoryData() = runTest {
        repository.characters.value = listOf(rick)

        assertEquals(listOf(rick), ObserveCharactersUseCase(repository)().first())
    }

    @Test
    fun getCharactersUseCase_emitsRepositoryData() = runTest {
        repository.characters.value = listOf(rick)

        assertEquals(listOf(rick), GetCharactersUseCase(repository)().first())
    }

    @Test
    fun refreshCharactersUseCase_delegatesToRepository() = runTest {
        repository.refreshResult = listOf(rick)

        RefreshCharactersUseCase(repository)()

        assertEquals(1, repository.refreshCount)
        assertEquals(listOf(rick), repository.characters.value)
    }

    @Test
    fun refreshCharactersUseCase_propagatesError() = runTest {
        repository.errorToThrow = IllegalStateException("boom")

        assertFailsWith<IllegalStateException> {
            RefreshCharactersUseCase(repository)()
        }
    }
}
