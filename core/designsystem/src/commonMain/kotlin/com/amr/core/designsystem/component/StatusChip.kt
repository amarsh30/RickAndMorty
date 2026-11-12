package com.amr.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.icon.AppIcons
import com.amr.core.designsystem.theme.AppThemeTokens

/** Life status as shown in the UI; anything the API does not call alive/dead is unknown. */
enum class LifeStatus {
    Alive, Dead, Unknown;

    companion object {
        fun fromApi(status: String): LifeStatus = when (status.trim().lowercase()) {
            "alive" -> Alive
            "dead" -> Dead
            else -> Unknown
        }
    }
}

const val STATUS_CHIP_TAG = "status_chip"

/**
 * Status as icon + text on a tinted container, so it never relies on color alone.
 * [label] is shown as-is (e.g. the API's "Alive"), which keeps existing text lookups working.
 */
@Composable
fun StatusChip(
    label: String,
    modifier: Modifier = Modifier,
    status: LifeStatus = LifeStatus.fromApi(label)
) {
    val colors = AppThemeTokens.statusColors
    val (container, content, icon) = when (status) {
        LifeStatus.Alive -> Triple(colors.aliveContainer, colors.onAliveContainer, AppIcons.CheckCircle)
        LifeStatus.Dead -> Triple(colors.deadContainer, colors.onDeadContainer, AppIcons.Cancel)
        LifeStatus.Unknown -> Triple(colors.unknownContainer, colors.onUnknownContainer, AppIcons.Help)
    }
    StatusChipContent(label, icon, container, content, modifier)
}

@Composable
private fun StatusChipContent(
    label: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier
) {
    val spacing = AppThemeTokens.spacing
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        // One node for screen readers: the label; the icon is decorative.
        modifier = modifier.testTag(STATUS_CHIP_TAG).semantics(mergeDescendants = true) {}
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}
