package com.amr.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.icon.AppIcons
import com.amr.core.designsystem.theme.AppThemeTokens

const val AVATAR_FALLBACK_TAG = "avatar_fallback"

/** Section title, announced as a heading; [trailing] is optional secondary text (e.g. a count). */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val spacing = AppThemeTokens.spacing
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        trailing?.let {
            Text(text = it, style = AppThemeTokens.mono, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Neutral avatar used when there is no image or it failed to load. */
@Composable
fun AvatarFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .testTag(AVATAR_FALLBACK_TAG)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = AppIcons.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(0.5f)
        )
    }
}

/**
 * A row with a round avatar, title and optional subtitle. The image is a slot so
 * this module stays free of image-loading libraries; without one it shows
 * [AvatarFallback]. The image is decorative here, since the title names the item.
 */
@Composable
fun AvatarItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    avatarSize: Dp = 48.dp,
    onClick: (() -> Unit)? = null,
    avatar: (@Composable (Modifier) -> Unit)? = null
) {
    val spacing = AppThemeTokens.spacing
    val clickModifier = if (onClick != null) {
        Modifier.focusRing(RectangleShape).clickable(onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(clickModifier)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarModifier = Modifier.size(avatarSize).clip(CircleShape)
        if (avatar != null) avatar(avatarModifier) else AvatarFallback(avatarModifier)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (onClick != null) {
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
