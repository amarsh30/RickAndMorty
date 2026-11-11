package com.amr.core.data.fake

import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.data.remote.dto.ResourceRefDto
import com.amr.core.domain.model.Character

/**
 * A complete API character, shaped like the real response, and the domain
 * character it maps to. Keeps tests in sync with the full DTO contract.
 */
fun testCharacterDto(
    id: Int,
    name: String = "Character $id",
    status: String = "Alive",
    species: String = "Human",
    imageUrl: String = "img$id.png",
    gender: String = "Male"
) = CharacterDto(
    id = id,
    name = name,
    status = status,
    species = species,
    imageUrl = imageUrl,
    type = "",
    gender = gender,
    origin = ResourceRefDto("Earth (C-137)", "https://rickandmortyapi.com/api/location/1"),
    location = ResourceRefDto("Citadel of Ricks", "https://rickandmortyapi.com/api/location/3"),
    episode = listOf(
        "https://rickandmortyapi.com/api/episode/1",
        "https://rickandmortyapi.com/api/episode/2"
    )
)

/** The domain result of mapping [testCharacterDto] with the same arguments. */
fun testCharacter(
    id: Int,
    name: String = "Character $id",
    status: String = "Alive",
    species: String = "Human",
    imageUrl: String = "img$id.png",
    gender: String = "Male"
) = Character(
    id = id,
    name = name,
    status = status,
    species = species,
    imageUrl = imageUrl,
    type = "",
    gender = gender,
    originName = "Earth (C-137)",
    originId = 1,
    locationName = "Citadel of Ricks",
    locationId = 3,
    episodeIds = listOf(1, 2)
)
