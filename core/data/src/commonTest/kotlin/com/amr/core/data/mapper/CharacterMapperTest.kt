package com.amr.core.data.mapper

import com.amr.core.data.fake.testCharacter
import com.amr.core.data.fake.testCharacterDto
import com.amr.core.data.remote.dto.ResourceRefDto
import com.amr.core.database.entity.CharacterEntity
import com.amr.core.domain.error.DataError
import com.amr.core.domain.model.Character
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

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
        val dto = testCharacterDto(1, "Rick Sanchez", imageUrl = "rick.png")

        assertEquals(testCharacter(1, "Rick Sanchez", imageUrl = "rick.png"), dto.toDomain())
    }

    @Test
    fun dto_withUnknownOrigin_mapsToNullId() {
        val dto = testCharacterDto(1).copy(origin = ResourceRefDto("unknown", ""))

        val domain = dto.toDomain()

        assertEquals("unknown", domain.originName)
        assertNull(domain.originId)
    }

    @Test
    fun dto_withMalformedResourceUrl_isInvalidData() {
        assertFailsWith<DataError.InvalidData> {
            testCharacterDto(1).copy(location = ResourceRefDto("x", "https://x/api/location/abc")).toDomain()
        }
        assertFailsWith<DataError.InvalidData> {
            testCharacterDto(1).copy(episode = listOf("https://x/api/episode/")).toDomain()
        }
    }

    @Test
    fun entity_toDomain_mapsAllFields() {
        val entity = CharacterEntity(1, "Rick Sanchez", "Alive", "Human", "rick.png")

        assertEquals(character, entity.toDomain())
    }

    @Test
    fun domain_toEntity_andBack_isLossless() {
        assertEquals(character, character.toEntity().toDomain())
        val full = testCharacter(1)
        assertEquals(full, full.toEntity().toDomain())
        assertEquals(full, full.toSearchEntity(updatedAt = 42L).toDomain())
        assertEquals(42L, full.toSearchEntity(updatedAt = 42L).updatedAt)
        assertEquals(full, full.toFavoriteEntity().toDomain())
        val noEpisodes = full.copy(episodeIds = emptyList())
        assertEquals(noEpisodes, noEpisodes.toEntity().toDomain())
    }

    @Test
    fun nullImageUrl_isPreserved() {
        val withoutImage = character.copy(imageUrl = null)

        assertEquals(withoutImage, withoutImage.toEntity().toDomain())
    }

    @Test
    fun malformedStoredEpisodeIds_isInvalidData() {
        assertFailsWith<DataError.InvalidData> {
            CharacterEntity(1, "Rick", "Alive", "Human", null, episodeIds = "1,x").toDomain()
        }
    }
}
