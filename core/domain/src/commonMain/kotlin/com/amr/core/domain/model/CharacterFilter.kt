package com.amr.core.domain.model

/**
 * Search criteria supported by both the local cache and the API.
 * Blank text is treated as "no constraint".
 */
data class CharacterFilter(
    val name: String = "",
    val species: String = "",
    val status: Status? = null,
    val gender: Gender? = null
) {
    enum class Status { ALIVE, DEAD, UNKNOWN }

    enum class Gender { FEMALE, MALE, GENDERLESS, UNKNOWN }

    /** Same criteria with surrounding whitespace removed, so equal searches compare equal. */
    fun normalized(): CharacterFilter = copy(name = name.trim(), species = species.trim())

    val isEmpty: Boolean
        get() = name.isBlank() && species.isBlank() && status == null && gender == null
}
