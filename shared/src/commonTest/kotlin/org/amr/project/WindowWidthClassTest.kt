package org.amr.project

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class WindowWidthClassTest {

    @Test
    fun widthIsBucketedByAvailableSpace() {
        assertEquals(WindowWidthClass.Compact, widthClassOf(0.dp))
        assertEquals(WindowWidthClass.Compact, widthClassOf(599.dp))
        assertEquals(WindowWidthClass.Medium, widthClassOf(600.dp))
        assertEquals(WindowWidthClass.Medium, widthClassOf(839.dp))
        assertEquals(WindowWidthClass.Expanded, widthClassOf(840.dp))
    }
}
