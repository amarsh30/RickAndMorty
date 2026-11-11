package com.amr.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeDto(
    val id: Int,
    val name: String,
    @SerialName("air_date")
    val airDate: String,
    /** Season/episode code, e.g. "S01E01". */
    @SerialName("episode")
    val code: String,
    /** Character URLs, e.g. "https://rickandmortyapi.com/api/character/1". */
    val characters: List<String>
)

@Serializable
data class EpisodeResponseDto(
    val info: PageInfoDto,
    val results: List<EpisodeDto>
)
