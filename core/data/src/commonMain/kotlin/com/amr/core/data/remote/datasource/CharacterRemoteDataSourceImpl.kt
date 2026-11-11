package com.amr.core.data.remote.datasource

import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.data.remote.dto.CharacterResponseDto
import com.amr.core.network.util.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments

class CharacterRemoteDataSourceImpl(
    private val client: HttpClient
) : CharacterRemoteDataSource {

    override suspend fun getCharacters(page: Int): CharacterResponseDto {
        return safeApiCall<CharacterResponseDto> {
            client.get("character") {
                parameter("page", page)
            }
        }
    }

    override suspend fun searchCharacters(
        name: String?,
        status: String?,
        species: String?,
        gender: String?
    ): CharacterResponseDto {
        return safeApiCall<CharacterResponseDto> {
            client.get("character") {
                // Ktor skips parameters whose value is null.
                parameter("name", name)
                parameter("status", status)
                parameter("species", species)
                parameter("gender", gender)
            }
        }
    }

    override suspend fun getCharacter(id: Int): CharacterDto {
        return safeApiCall<CharacterDto> {
            client.get("character") {
                url { appendPathSegments(id.toString()) }
            }
        }
    }
}
