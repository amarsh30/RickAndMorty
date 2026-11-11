package com.amr.core.data.remote.datasource

import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.data.remote.dto.CharacterResponseDto
import com.amr.core.network.exception.NetworkException
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

    /**
     * `character/1` answers with an object and `character/1,2` with an array, so a
     * single id goes through [getCharacter]; its 404 means "unknown id", like the
     * ids the multi-id endpoint silently drops.
     */
    override suspend fun getCharactersByIds(ids: List<Int>): List<CharacterDto> {
        require(ids.size <= CharacterRemoteDataSource.MAX_IDS_PER_REQUEST) {
            "At most ${CharacterRemoteDataSource.MAX_IDS_PER_REQUEST} ids per request"
        }
        return when (ids.size) {
            0 -> emptyList()
            1 -> try {
                listOf(getCharacter(ids.single()))
            } catch (_: NetworkException.NotFound) {
                emptyList()
            }
            else -> safeApiCall<List<CharacterDto>> {
                // Plain string so the commas stay unencoded.
                client.get("character/${ids.joinToString(",")}")
            }
        }
    }
}
