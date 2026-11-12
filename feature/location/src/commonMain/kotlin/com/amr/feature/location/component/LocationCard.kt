package com.amr.feature.location.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.component.focusRing
import com.amr.core.designsystem.icon.AppIcons
import com.amr.core.designsystem.theme.AppThemeTokens
import com.amr.core.domain.model.Location

/**
 * Outlined location card: planet icon, name, and "type · dimension" metadata.
 * The list data has no resident ids, so no resident count is shown here; the
 * accurate count lives on Location Detail.
 */
@Composable
internal fun LocationCard(location: Location, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = AppThemeTokens.spacing
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).focusRing(MaterialTheme.shapes.medium),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Decorative; the card's text names the location.
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Public, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                // No line limit: long names stay fully readable at large font scales.
                Text(text = location.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${location.type} · ${location.dimension}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
