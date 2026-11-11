package com.amr.core.data.remote.datasource

import com.amr.core.data.remote.dto.EpisodeDto
import com.amr.core.data.remote.dto.EpisodeResponseDto
import com.amr.core.network.util.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments

interface EpisodeRemoteDataSource {
    suspend fun getEpisodes(page: Int): EpisodeResponseDto

    suspend fun getEpisode(id: Int): EpisodeDto
}

class EpisodeRemoteDataSourceImpl(
    private val client: HttpClient
) : EpisodeRemoteDataSource {

    override suspend fun getEpisodes(page: Int): EpisodeResponseDto =
        safeApiCall<EpisodeResponseDto> {
            client.get("episode") { parameter("page", page) }
        }

    override suspend fun getEpisode(id: Int): EpisodeDto =
        safeApiCall<EpisodeDto> {
            client.get("episode") { url { appendPathSegments(id.toString()) } }
        }
}
