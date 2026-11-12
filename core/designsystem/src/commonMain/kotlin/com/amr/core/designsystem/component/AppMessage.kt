package com.amr.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.theme.AppThemeTokens

enum class MessageTone { Neutral, Error }

/**
 * Full-area message for empty, error and offline states: icon, title, optional
 * description and an optional action (shown only when both text and handler exist).
 */
@Composable
fun AppMessage(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    tone: MessageTone = MessageTone.Neutral,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val spacing = AppThemeTokens.spacing
    val accent = when (tone) {
        MessageTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        MessageTone.Error -> MaterialTheme.colorScheme.error
    }
    Column(
        modifier = modifier.padding(spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Decorative: the title carries the meaning.
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(48.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = if (tone == MessageTone.Error) accent else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            // Errors replace the content without user action, so announce them politely.
            modifier = if (tone == MessageTone.Error) {
                Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            } else {
                Modifier
            }
        )
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (actionText != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = spacing.sm).heightIn(min = 48.dp)
            ) {
                Text(text = actionText)
            }
        }
    }
}
