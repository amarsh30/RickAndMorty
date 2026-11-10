package com.amr.core.data.fake

import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.data.remote.dto.CharacterResponseDto
import com.amr.core.network.exception.NetworkException

class FakeCharacterRemoteDataSource : CharacterRemoteDataSource {

    var response = CharacterResponseDto(
        results = listOf(
            CharacterDto(
                id = 1,
                name = "Rick Sanchez",
                status = "Alive",
                species = "Human",
                imageUrl = "https://example.com/rick.png"
            ),
            CharacterDto(
                id = 2,
                name = "Morty Smith",
                status = "Alive",
                species = "Human",
                imageUrl = "https://example.com/morty.png"
            )
        )
    )

    /** Jika diisi, getCharacters() akan melempar exception ini. */
    var errorToThrow: Exception? = null

    var callCount = 0
        private set

    override suspend fun getCharacters(): CharacterResponseDto {
        callCount++
        errorToThrow?.let { throw it }
        return response
    }

    fun failWithNoInternet() {
        errorToThrow = NetworkException.NoInternet()
    }
}
