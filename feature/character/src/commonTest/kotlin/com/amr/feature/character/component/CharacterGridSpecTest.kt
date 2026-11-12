package com.amr.feature.character.component

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterGridSpecTest {

    @Test
    fun compactWidth_isOneColumnOfHorizontalCards() {
        val spec = CharacterGridSpec.forSize(width = 599.dp, height = 800.dp)
        assertEquals(GridCells.Fixed(1), spec.columns)
        assertEquals(CharacterCardLayout.Horizontal, spec.cardLayout)
    }

    @Test
    fun wideAndTall_isAdaptiveGridOfVerticalTiles() {
        listOf(600.dp, 840.dp, 1200.dp).forEach { width ->
            val spec = CharacterGridSpec.forSize(width = width, height = 800.dp)
            assertEquals(GridCells.Adaptive(180.dp), spec.columns)
            assertEquals(CharacterCardLayout.Vertical, spec.cardLayout)
        }
    }

    @Test
    fun wideButShort_likePhoneLandscape_usesHorizontalCardsInColumns() {
        val spec = CharacterGridSpec.forSize(width = 835.dp, height = 330.dp)
        assertEquals(GridCells.Adaptive(300.dp), spec.columns)
        assertEquals(CharacterCardLayout.Horizontal, spec.cardLayout)
    }
}
