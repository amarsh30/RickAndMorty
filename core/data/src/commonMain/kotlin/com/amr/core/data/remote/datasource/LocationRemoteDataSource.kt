package com.amr.core.data.remote.datasource

import com.amr.core.data.remote.dto.LocationDto
import com.amr.core.data.remote.dto.LocationResponseDto
import com.amr.core.network.util.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.appendPathSegments

interface LocationRemoteDataSource {
    suspend fun getLocations(page: Int): LocationResponseDto

    suspend fun getLocation(id: Int): LocationDto
}

class LocationRemoteDataSourceImpl(
    private val client: HttpClient
) : LocationRemoteDataSource {

    override suspend fun getLocations(page: Int): LocationResponseDto =
        safeApiCall<LocationResponseDto> {
            client.get("location") { parameter("page", page) }
        }

    override suspend fun getLocation(id: Int): LocationDto =
        safeApiCall<LocationDto> {
            client.get("location") { url { appendPathSegments(id.toString()) } }
        }
}
