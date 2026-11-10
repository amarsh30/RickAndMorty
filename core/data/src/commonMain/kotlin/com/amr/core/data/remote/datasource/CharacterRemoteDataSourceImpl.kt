package com.amr.core.data.remote.datasource

import com.amr.core.data.remote.dto.CharacterResponseDto
import com.amr.core.network.util.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get

class CharacterRemoteDataSourceImpl(
    private val client: HttpClient
) : CharacterRemoteDataSource {

    override suspend fun getCharacters(): CharacterResponseDto {
        return safeApiCall<CharacterResponseDto> {
            client.get("character")
        }
    }
}