package com.amr.core.data.mapper

import com.amr.core.data.remote.dto.CharacterDto
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.domain.model.Character
import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterMapperTest {

    private val character = Character(
        id = 1,
        name = "Rick Sanchez",
        status = "Alive",
        species = "Human",
        imageUrl = "rick.png"
    )

    @Test
    fun dto_toDomain_mapsAllFields() {
        val dto = CharacterDto(1, "Rick Sanchez", "Alive", "Human", "rick.png")

        assertEquals(character, dto.toDomain())
    }

    @Test
    fun entity_toDomain_mapsAllFields() {
        val entity = CharacterEntity(1, "Rick Sanchez", "Alive", "Human", "rick.png")

        assertEquals(character, entity.toDomain())
    }

    @Test
    fun domain_toEntity_andBack_isLossless() {
        assertEquals(character, character.toEntity().toDomain())
    }

    @Test
    fun nullImageUrl_isPreserved() {
        val withoutImage = character.copy(imageUrl = null)

        assertEquals(withoutImage, withoutImage.toEntity().toDomain())
    }
}
