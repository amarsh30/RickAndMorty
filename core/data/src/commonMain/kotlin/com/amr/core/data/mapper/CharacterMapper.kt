package com.amr.core.data.mapper

import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.domain.model.Character

fun CharacterDto.toDomain(): Character {
    return Character(
        id = id,
        name = name,
        status = status,
        species = species,
        imageUrl = imageUrl
    )
}