package com.amr.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CharacterDto(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,

    @SerialName("image")
    val imageUrl: String,

    val type: String,
    val gender: String,
    val origin: ResourceRefDto,
    val location: ResourceRefDto,

    /** Episode resource URLs, e.g. "https://rickandmortyapi.com/api/episode/1". */
    val episode: List<String>
)

/** A named link to another resource; [url] is empty when the resource is unknown. */
@Serializable
data class ResourceRefDto(
    val name: String,
    val url: String
)
