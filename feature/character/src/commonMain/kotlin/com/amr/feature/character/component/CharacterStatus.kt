
package com.amr.feature.character.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.amr.core.designsystem.component.StatusChip

/** Character life status, shown with the design-system [StatusChip] (icon + text + semantic color). */
@Composable
fun CharacterStatus(
    status: String,
    modifier: Modifier = Modifier
) {
    StatusChip(label = status, modifier = modifier)
}
