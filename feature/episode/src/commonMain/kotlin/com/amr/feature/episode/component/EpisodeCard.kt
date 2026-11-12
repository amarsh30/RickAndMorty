package com.amr.feature.episode.component

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
import com.amr.core.domain.model.Episode

/** Outlined episode card: monospace code, title, and air date. */
@Composable
internal fun EpisodeCard(episode: Episode, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
            // Decorative; the text describes the episode.
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Movie, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                Text(text = episode.code, style = AppThemeTokens.mono, color = MaterialTheme.colorScheme.primary)
                // No line limit: long titles stay fully readable at large font scales.
                Text(text = episode.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = episode.airDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
