package com.amr.core.designsystem.component

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/** Column policy for text-first cards (Location / Episode lists). */
class CardColumnsTest {

    @Test
    fun phonePortrait_isOneColumn() {
        assertEquals(GridCells.Fixed(1), cardColumnsFor(400.dp))
        assertEquals(GridCells.Fixed(1), cardColumnsFor(599.dp))
    }

    @Test
    fun phoneLandscape_tabletPortrait_andTabletLandscape_areAdaptive() {
        // Short phone landscape (after the rail), tablet portrait, wide tablet landscape.
        listOf(600.dp, 720.dp, 835.dp, 1200.dp).forEach { width ->
            assertEquals(GridCells.Adaptive(280.dp), cardColumnsFor(width))
        }
    }

    @Test
    fun minimumCardWidth_canBeTuned() {
        assertEquals(GridCells.Adaptive(320.dp), cardColumnsFor(900.dp, minCardWidth = 320.dp))
    }
}
