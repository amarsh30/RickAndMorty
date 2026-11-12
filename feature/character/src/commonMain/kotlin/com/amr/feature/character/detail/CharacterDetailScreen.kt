package com.amr.feature.character.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amr.core.designsystem.component.AppError
import com.amr.core.designsystem.component.AppLoading
import com.amr.core.designsystem.component.AppTopBar
import com.amr.core.designsystem.component.InfoBanner
import com.amr.core.designsystem.component.InfoBannerTone
import com.amr.core.designsystem.component.SectionHeader
import com.amr.core.designsystem.component.focusRing
import androidx.compose.foundation.shape.CircleShape
import com.amr.core.designsystem.icon.AppIcons
import com.amr.core.designsystem.theme.AppThemeTokens
import com.amr.core.domain.model.Character
import com.amr.feature.character.component.CharacterImage
import com.amr.feature.character.component.CharacterStatus
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Episode chips shown before collapsing the rest into a "+N" chip. */
private const val MAX_EPISODE_CHIPS = 24

@Composable
fun CharacterDetailScreen(
    characterId: Int,
    onBack: () -> Unit,
    onLocationClick: (Int) -> Unit = {},
    // False in a side pane: the list is already visible, so there is nothing to go back to.
    showBackButton: Boolean = true,
    viewModel: CharacterDetailViewModel = koinViewModel(
        key = "character-detail-$characterId",
        parameters = { parametersOf(characterId) }
    )
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Connectivity recovery only while this screen is started (visible).
    LifecycleStartEffect(viewModel) {
        viewModel.setVisible(true)
        onStopOrDispose { viewModel.setVisible(false) }
    }

    CharacterDetailContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onToggleFavorite = viewModel::toggleFavorite,
        onLocationClick = onLocationClick,
        showBackButton = showBackButton
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CharacterDetailContent(
    state: CharacterDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLocationClick: (Int) -> Unit = {},
    showBackButton: Boolean = true
) {
    Scaffold(
        topBar = {
            // The name is the body's heading (and may wrap there), so the bar names the screen type.
            AppTopBar(title = "Detail Karakter", onBack = if (showBackButton) onBack else null)
        }
    ) { padding ->
        val character = state.character
        val contentModifier = Modifier.fillMaxSize().padding(padding)

        when {
            state.isLoading -> AppLoading(modifier = contentModifier)

            character == null -> AppError(
                message = state.errorMessage ?: "Data karakter tidak ditemukan.",
                onRetry = onRetry,
                modifier = contentModifier
            )

            else -> Column(
                modifier = contentModifier
                    .verticalScroll(rememberScrollState())
                    .padding(AppThemeTokens.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.md)
            ) {
                if (state.isRefreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                // Cached detail is still shown when a refresh fails.
                state.errorMessage?.let { message ->
                    InfoBanner(
                        message = message,
                        tone = InfoBannerTone.Warning,
                        actionText = "Coba Lagi",
                        onAction = onRetry,
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }

                CharacterDetailBody(
                    character = character,
                    isFavorite = state.isFavorite,
                    onToggleFavorite = onToggleFavorite,
                    onLocationClick = onLocationClick
                )
            }
        }
    }
}

@Composable
private fun CharacterDetailBody(
    character: Character,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onLocationClick: (Int) -> Unit
) {
    val spacing = AppThemeTokens.spacing
    // Single column, capped so it stays readable on wide windows.
    Column(
        modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md)
    ) {
        CharacterImage(
            imageUrl = character.imageUrl,
            contentDescription = "Foto ${character.name}",
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f)
        )

        Text(
            text = character.name,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        CharacterStatus(status = character.status)

        FavoriteToggle(isFavorite = isFavorite, onToggle = onToggleFavorite)

        if (!character.hasDetails) {
            InfoBanner(message = "Sebagian data belum tersedia offline.")
        }

        InfoCard {
            InfoRow("Spesies", character.species)
            InfoRow("Tipe", character.type?.ifBlank { null })
            InfoRow("Gender", character.gender)
        }

        LocationCard("Asal", character.originName, character.originId, onLocationClick)
        LocationCard("Lokasi terakhir", character.locationName, character.locationId, onLocationClick)

        EpisodeSection(character.episodeIds)
    }
}

/** Full-width toggle; icon + the original labels so it is easy to find and to test. */
@Composable
private fun FavoriteToggle(isFavorite: Boolean, onToggle: () -> Unit) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).focusRing(CircleShape)
    if (isFavorite) {
        OutlinedButton(onClick = onToggle, modifier = modifier) {
            Icon(AppIcons.Favorite, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Text("Hapus dari Favorit", modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
        }
    } else {
        Button(onClick = onToggle, modifier = modifier) {
            Icon(AppIcons.FavoriteBorder, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Text("Tambah ke Favorit", modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(AppThemeTokens.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.sm)
        ) { content() }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.md)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        // Values may wrap (large font scale) instead of being cut off.
        Text(
            text = value ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Origin / last location; tappable (opens Location Detail) only when its id is known. */
@Composable
private fun LocationCard(label: String, name: String?, locationId: Int?, onLocationClick: (Int) -> Unit) {
    val colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    val content: @Composable () -> Unit = {
        LocationCardContent(label, name ?: "-", clickable = locationId != null)
    }
    if (locationId != null) {
        OutlinedCard(
            onClick = { onLocationClick(locationId) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            colors = colors
        ) { content() }
    } else {
        OutlinedCard(modifier = Modifier.fillMaxWidth(), colors = colors) { content() }
    }
}

@Composable
private fun LocationCardContent(label: String, name: String, clickable: Boolean) {
    val spacing = AppThemeTokens.spacing
    Row(
        modifier = Modifier.padding(spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = if (clickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
        if (clickable) {
            Icon(AppIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Episodes the character appears in, from ids already stored with the character
 * (no extra requests). Ids map to the API's global episode numbers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeSection(episodeIds: List<Int>?) {
    val ids = episodeIds.orEmpty()
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Episode", trailing = episodeIds?.size?.toString() ?: "-")
        when {
            episodeIds == null -> Text(
                text = "Daftar episode belum tersedia.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ids.isEmpty() -> Text(
                text = "Tidak muncul di episode mana pun.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.sm)
            ) {
                ids.take(MAX_EPISODE_CHIPS).forEach { EpisodeChip("Ep $it") }
                if (ids.size > MAX_EPISODE_CHIPS) EpisodeChip("+${ids.size - MAX_EPISODE_CHIPS} lainnya")
            }
        }
    }
}

@Composable
private fun EpisodeChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Box(modifier = Modifier.padding(horizontal = AppThemeTokens.spacing.sm, vertical = AppThemeTokens.spacing.xs)) {
            Text(text = text, style = AppThemeTokens.mono)
        }
    }
}
