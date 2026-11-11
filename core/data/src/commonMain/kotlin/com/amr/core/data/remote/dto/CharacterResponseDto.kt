package com.amr.core.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CharacterResponseDto(
    val info: PageInfoDto,
    val results: List<CharacterDto>
)

/**
 * Pagination metadata of a Rick and Morty API list response.
 * [count] and [pages] are required; [next] and [prev] are null on the last and first page.
 */
@Serializable
data class PageInfoDto(
    val count: Int,
    val pages: Int,
    val next: String?,
    val prev: String?
)
