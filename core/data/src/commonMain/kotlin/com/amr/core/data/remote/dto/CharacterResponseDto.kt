package com.amr.core.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CharacterResponseDto(
    val results: List<CharacterDto>
)