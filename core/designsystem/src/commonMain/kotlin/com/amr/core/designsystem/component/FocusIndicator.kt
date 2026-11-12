package com.amr.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Width of the keyboard-focus ring. */
val FocusRingWidth = 3.dp

/**
 * Draws a primary-colored ring while this element (or a child, e.g. the clickable
 * inside a card) has keyboard focus. Material's default focus layer is only ~1.2:1
 * against the surface; `primary` is >= 5.8:1 in both themes, above the 3:1 needed
 * for a focus indicator. Nothing changes when unfocused, and the ring is drawn
 * inside the bounds, so layout and touch targets stay the same.
 *
 * Place it before the clickable/focusable modifier so it observes that focus.
 */
fun Modifier.focusRing(shape: Shape): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val color = MaterialTheme.colorScheme.primary
    onFocusChanged { focused = it.hasFocus }
        .then(if (focused) Modifier.border(FocusRingWidth, color, shape) else Modifier)
}
