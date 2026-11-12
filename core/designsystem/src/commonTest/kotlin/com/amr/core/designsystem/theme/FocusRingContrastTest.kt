package com.amr.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/** The focus ring is drawn in `primary`; WCAG 2.2 asks >= 3:1 against what it sits on. */
class FocusRingContrastTest {

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    @Test
    fun focusRing_hasAtLeast3to1_againstSurfacesItIsDrawnOn() {
        listOf(LightColors, DarkColors).forEach { scheme ->
            listOf(scheme.surface, scheme.background, scheme.surfaceContainer, scheme.secondaryContainer).forEach { bg ->
                val ratio = contrast(scheme.primary, bg)
                assertTrue(ratio >= 3f, "focus ring ${scheme.primary} on $bg is only $ratio:1")
            }
        }
    }
}
