package com.amr.feature.character.search

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.component.focusRing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amr.core.designsystem.component.AppBarAction
import com.amr.core.designsystem.component.AppMessage
import com.amr.core.designsystem.component.AppTopBar
import com.amr.core.designsystem.component.InfoBanner
import com.amr.core.designsystem.component.InfoBannerTone
import com.amr.core.designsystem.icon.AppIcons
import com.amr.core.designsystem.theme.AppThemeTokens
import com.amr.core.domain.model.CharacterFilter
import com.amr.feature.character.component.CharacterGrid
import org.koin.compose.viewmodel.koinViewModel

internal const val ACTIVE_FILTERS_TAG = "active_filters"
internal const val CHIP_SCROLL_CUE_TAG = "chip_scroll_cue"
internal const val CLEAR_NAME_LABEL = "Hapus nama"
internal const val CLEAR_SPECIES_LABEL = "Hapus spesies"
internal const val CLEAR_ALL_FILTERS_LABEL = "Hapus semua filter"

@Composable
fun CharacterSearchScreen(
    onBack: () -> Unit,
    onCharacterClick: (Int) -> Unit,
    viewModel: CharacterSearchViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Connectivity recovery only while this screen is started (visible).
    LifecycleStartEffect(viewModel) {
        viewModel.setVisible(true)
        onStopOrDispose { viewModel.setVisible(false) }
    }

    CharacterSearchContent(
        state = state,
        onBack = onBack,
        onFilterChange = viewModel::onFilterChange,
        onRetry = viewModel::retry,
        onCharacterClick = onCharacterClick
    )
}

/**
 * Rendering only: every edit goes through [onFilterChange], so debounce,
 * cancellation and cache-first search stay in the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CharacterSearchContent(
    state: CharacterSearchUiState,
    onBack: () -> Unit,
    onFilterChange: (CharacterFilter) -> Unit,
    onRetry: () -> Unit,
    onCharacterClick: (Int) -> Unit
) {
    val filter = state.filter
    val spacing = AppThemeTokens.spacing

    Scaffold(
        topBar = { AppTopBar(title = "Cari Karakter", onBack = onBack) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                SearchField(
                    value = filter.name,
                    onValueChange = { onFilterChange(filter.copy(name = it)) },
                    label = "Nama",
                    clearLabel = CLEAR_NAME_LABEL,
                    leadingIcon = AppIcons.Search
                )
                SearchField(
                    value = filter.species,
                    onValueChange = { onFilterChange(filter.copy(species = it)) },
                    label = "Spesies",
                    clearLabel = CLEAR_SPECIES_LABEL,
                    leadingIcon = null
                )
            }

            ChipRow(
                options = CharacterFilter.Status.entries,
                selected = filter.status,
                label = { it.label },
                onSelect = { onFilterChange(filter.copy(status = it)) }
            )
            ChipRow(
                options = CharacterFilter.Gender.entries,
                selected = filter.gender,
                label = { it.label },
                onSelect = { onFilterChange(filter.copy(gender = it)) }
            )

            ActiveFilters(filter = filter, onFilterChange = onFilterChange)

            Column(
                modifier = Modifier.padding(horizontal = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.sm)
            ) {
                if (state.isSearchingRemote) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (state.isOfflineResult) {
                    InfoBanner(
                        message = "Offline: hasil dari data tersimpan dan mungkin belum lengkap.",
                        actionText = "Coba Lagi",
                        onAction = onRetry
                    )
                }
                state.errorMessage?.let { message ->
                    InfoBanner(message = message, tone = InfoBannerTone.Warning)
                }
            }

            when {
                filter.isEmpty -> AppMessage(
                    icon = AppIcons.Search,
                    title = "Masukkan nama, spesies, status, atau gender.",
                    modifier = Modifier.fillMaxWidth()
                )

                state.results.isEmpty() && !state.isSearchingRemote -> AppMessage(
                    icon = AppIcons.Info,
                    title = "Tidak ada karakter yang cocok.",
                    description = "Coba ubah atau hapus sebagian filter.",
                    modifier = Modifier.fillMaxWidth()
                )

                // Same cards and adaptive layout as the character list; result order is kept.
                else -> CharacterGrid(
                    characters = state.results,
                    onCharacterClick = onCharacterClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/** Text field with a clear button; the keyboard's search action just closes the keyboard. */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    clearLabel: String,
    leadingIcon: ImageVector?
) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        trailingIcon = if (value.isNotEmpty()) {
            { AppBarAction(icon = AppIcons.Close, label = clearLabel, onClick = { onValueChange("") }) }
        } else {
            null
        },
        // Search already runs as you type (debounced); the action only dismisses the keyboard.
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Single-choice chips; tapping the selected chip clears the choice. */
@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T?) -> Unit
) {
    val spacing = AppThemeTokens.spacing
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.horizontalScroll(scrollState).padding(horizontal = spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(if (isSelected) null else option) },
                    label = { Text(label(option)) },
                    leadingIcon = if (isSelected) {
                        { Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                    modifier = Modifier.focusRing(FilterChipDefaults.shape)
                )
            }
        }
        // More chips off-screen (e.g. at large font scales): fade + chevron at the end edge.
        // Not clickable, so taps pass through; hidden from screen readers, which can
        // reach every chip anyway; disappears once the row is scrolled to its end.
        // Only the trailing padding left = the last chip is fully visible: no cue.
        val endPaddingPx = with(LocalDensity.current) { spacing.lg.roundToPx() }
        if (scrollState.maxValue - scrollState.value > endPaddingPx) {
            val background = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .matchParentSize()
                    .wrapContentWidth(Alignment.End)
                    .width(40.dp)
                    .background(Brush.horizontalGradient(listOf(background.copy(alpha = 0f), background)))
                    .testTag(CHIP_SCROLL_CUE_TAG)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    AppIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Summary of what is filtering the results, each removable on its own. Labels
 * are prefixed ("Status: Dead") so they never clash with the chip labels.
 */
@Composable
private fun ActiveFilters(filter: CharacterFilter, onFilterChange: (CharacterFilter) -> Unit) {
    val active = buildList {
        if (filter.name.isNotBlank()) add("Nama: ${filter.name}" to filter.copy(name = ""))
        if (filter.species.isNotBlank()) add("Spesies: ${filter.species}" to filter.copy(species = ""))
        filter.status?.let { add("Status: ${it.label}" to filter.copy(status = null)) }
        filter.gender?.let { add("Gender: ${it.label}" to filter.copy(gender = null)) }
    }
    if (active.isEmpty()) return

    val spacing = AppThemeTokens.spacing
    Row(
        modifier = Modifier
            .testTag(ACTIVE_FILTERS_TAG)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        active.forEach { (text, without) ->
            InputChip(
                selected = true,
                onClick = { onFilterChange(without) },
                label = { Text(text) },
                trailingIcon = {
                    Icon(
                        AppIcons.Close,
                        // Names the filter, so each remove action is distinguishable to screen readers.
                        contentDescription = "Hapus filter $text",
                        modifier = Modifier.size(InputChipDefaults.IconSize)
                    )
                }
            )
        }
        if (active.size > 1) {
            TextButton(onClick = { onFilterChange(CharacterFilter()) }) {
                Text(CLEAR_ALL_FILTERS_LABEL, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private val CharacterFilter.Status.label: String
    get() = when (this) {
        CharacterFilter.Status.ALIVE -> "Alive"
        CharacterFilter.Status.DEAD -> "Dead"
        CharacterFilter.Status.UNKNOWN -> "Status unknown"
    }

private val CharacterFilter.Gender.label: String
    get() = when (this) {
        CharacterFilter.Gender.FEMALE -> "Female"
        CharacterFilter.Gender.MALE -> "Male"
        CharacterFilter.Gender.GENDERLESS -> "Genderless"
        CharacterFilter.Gender.UNKNOWN -> "Gender unknown"
    }
