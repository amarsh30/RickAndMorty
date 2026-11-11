package com.amr.core.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationDto(
    val id: Int,
    val name: String,
    val type: String,
    val dimension: String,
    /** Character URLs, e.g. "https://rickandmortyapi.com/api/character/8"; may be empty. */
    val residents: List<String>
)

@Serializable
data class LocationResponseDto(
    val info: PageInfoDto,
    val results: List<LocationDto>
)
