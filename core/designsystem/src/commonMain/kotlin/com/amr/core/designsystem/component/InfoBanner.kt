package com.amr.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.icon.AppIcons
import com.amr.core.designsystem.theme.AppThemeTokens

enum class InfoBannerTone { Info, Warning }

const val INFO_BANNER_TAG = "info_banner"

/**
 * Inline, screen-level notice (stale cache, refresh failed). It takes its own
 * space in the layout instead of overlaying content, and is separate from the
 * app-wide connectivity banner.
 */
@Composable
fun InfoBanner(
    message: String,
    modifier: Modifier = Modifier,
    tone: InfoBannerTone = InfoBannerTone.Info,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val spacing = AppThemeTokens.spacing
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        InfoBannerTone.Info -> colors.secondaryContainer to colors.onSecondaryContainer
        InfoBannerTone.Warning -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    Surface(
        color = container,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        // Polite live region: screen readers announce the notice when it appears/changes
        // (e.g. a refresh failing while cached data stays on screen).
        modifier = modifier
            .fillMaxWidth()
            .testTag(INFO_BANNER_TAG)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (tone == InfoBannerTone.Warning) AppIcons.ErrorOutline else AppIcons.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (actionText != null && onAction != null) {
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.heightIn(min = 48.dp).focusRing(ButtonDefaults.textShape)
                ) {
                    Text(text = actionText)
                }
            }
        }
    }
}
