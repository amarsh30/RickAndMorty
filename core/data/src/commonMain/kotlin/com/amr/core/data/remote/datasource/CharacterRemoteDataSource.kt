package com.amr.core.data.remote.datasource

import com.amr.core.data.remote.dto.CharacterResponseDto

interface CharacterRemoteDataSource {
    suspend fun getCharacters(): CharacterResponseDto
}