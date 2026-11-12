package com.amr.feature.character.component

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterPaneLayoutTest {

    @Test
    fun compactPhone_isSinglePane() {
        assertEquals(CharacterPaneLayout.SinglePane, characterPaneLayout(400.dp, 800.dp))
    }

    @Test
    fun mediumTabletPortrait_afterRail_isSinglePaneGrid() {
        // 800dp window minus an 80dp rail.
        assertEquals(CharacterPaneLayout.SinglePane, characterPaneLayout(720.dp, 1200.dp))
    }

    @Test
    fun expandedAndTallEnough_isTwoPane() {
        assertEquals(CharacterPaneLayout.TwoPane, characterPaneLayout(841.dp, 480.dp))
        assertEquals(CharacterPaneLayout.TwoPane, characterPaneLayout(1200.dp, 720.dp))
    }

    @Test
    fun wideButShort_phoneLandscape_staysSinglePane() {
        // 915dp window minus the rail, but only ~330dp of height.
        assertEquals(CharacterPaneLayout.SinglePane, characterPaneLayout(835.dp, 330.dp))
        assertEquals(CharacterPaneLayout.SinglePane, characterPaneLayout(1200.dp, 479.dp))
    }

    @Test
    fun widthJustBelowListPlusMinimumDetail_staysSinglePane() {
        // 840dp is "expanded", but the detail pane would be narrower than its minimum.
        assertEquals(CharacterPaneLayout.SinglePane, characterPaneLayout(840.dp, 900.dp))
    }
}
