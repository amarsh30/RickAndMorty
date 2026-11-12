
package com.amr.feature.character.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.component.focusRing
import com.amr.core.designsystem.theme.AppThemeTokens

/** Horizontal rows on compact lists, vertical tiles in grids. */
enum class CharacterCardLayout { Horizontal, Vertical }

/**
 * Outlined character card: portrait, name, species (secondary) and status chip.
 * The image is decorative here since the name sits right next to it.
 */
@Composable
fun CharacterCard(
    name: String,
    status: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    species: String? = null,
    layout: CharacterCardLayout = CharacterCardLayout.Horizontal,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    // Selected (shown in the detail pane): tinted container + primary outline, and
    // announced as selected, so it never relies on color alone.
    val cardModifier = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .then(if (selected) Modifier.semantics { this.selected = true } else Modifier)
        .focusRing(MaterialTheme.shapes.medium)
    val colors = CardDefaults.outlinedCardColors(
        containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
    )
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        CardDefaults.outlinedCardBorder()
    }
    val content: @Composable ColumnScope.() -> Unit = {
        when (layout) {
            CharacterCardLayout.Horizontal -> HorizontalContent(name, status, imageUrl, species)
            CharacterCardLayout.Vertical -> VerticalContent(name, status, imageUrl, species)
        }
    }

    // A clickable card is announced as a button and has a 48dp+ touch target.
    if (onClick != null) {
        OutlinedCard(onClick = onClick, modifier = cardModifier, colors = colors, border = border, content = content)
    } else {
        OutlinedCard(modifier = cardModifier, colors = colors, border = border, content = content)
    }
}

@Composable
private fun HorizontalContent(name: String, status: String, imageUrl: String?, species: String?) {
    val spacing = AppThemeTokens.spacing
    Row(
        modifier = Modifier.padding(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CharacterImage(imageUrl = imageUrl, contentDescription = null, modifier = Modifier.size(80.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xs)
        ) {
            CardTexts(name, species)
            CharacterStatus(status = status)
        }
    }
}

@Composable
private fun VerticalContent(name: String, status: String, imageUrl: String?, species: String?) {
    val spacing = AppThemeTokens.spacing
    CharacterImage(
        imageUrl = imageUrl,
        contentDescription = null,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
    )
    Column(
        modifier = Modifier.padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs)
    ) {
        CardTexts(name, species)
        CharacterStatus(status = status)
    }
}

@Composable
private fun CardTexts(name: String, species: String?) {
    // Two lines so long names stay readable at large font scales.
    Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    species?.takeIf { it.isNotBlank() }?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
