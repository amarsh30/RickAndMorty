package com.amr.core.data.fake

import com.amr.core.data.remote.datasource.CharacterRemoteDataSource
import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.data.remote.dto.CharacterResponseDto

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

    var shouldThrowError = false

    override suspend fun getCharacters(): CharacterResponseDto {
        if (shouldThrowError) {
            throw Exception("Network error")
        }

        return response
    }
}