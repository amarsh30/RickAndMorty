package com.amr.core.data.mapper

import com.amr.core.database.entity.CharacterEntity
import com.amr.core.domain.model.Character

fun CharacterEntity.toDomain(): Character {
    return Character(
        id = id,
        name = name,
        status = status,
        species = species,
        imageUrl = imageUrl
    )
}

fun Character.toEntity(): CharacterEntity {
    return CharacterEntity(
        id = id,
        name = name,
        status = status,
        species = species,
        imageUrl = imageUrl
    )
}