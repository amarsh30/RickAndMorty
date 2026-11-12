package com.amr.feature.character.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amr.core.domain.model.Character

/** Below this content width the list is a single column of horizontal cards. */
private val CompactMaxWidth = 600.dp

/** Grid tiles never get narrower than this; wide windows add columns instead of stretching. */
private val GridTileMinWidth = 180.dp

/**
 * Below this height (e.g. a phone in landscape) square tiles would fill the whole
 * viewport and hide name/status, so the grid uses horizontal cards instead.
 */
private val ShortMaxHeight = 480.dp

/** Minimum width of a horizontal card when several share a row. */
private val HorizontalCardMinWidth = 300.dp

internal const val CHARACTER_GRID_TAG = "character_grid"

/** Columns and card style for the *available* space (not device orientation). */
internal data class CharacterGridSpec(val columns: GridCells, val cardLayout: CharacterCardLayout) {
    companion object {
        fun forSize(width: Dp, height: Dp): CharacterGridSpec = when {
            width < CompactMaxWidth ->
                CharacterGridSpec(GridCells.Fixed(1), CharacterCardLayout.Horizontal)
            height < ShortMaxHeight ->
                CharacterGridSpec(GridCells.Adaptive(HorizontalCardMinWidth), CharacterCardLayout.Horizontal)
            else ->
                CharacterGridSpec(GridCells.Adaptive(GridTileMinWidth), CharacterCardLayout.Vertical)
        }
    }
}

/** Measures the space it gets and hands the matching [CharacterGridSpec] to [content]. */
@Composable
internal fun AdaptiveCharacterLayout(
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.(CharacterGridSpec) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        content(CharacterGridSpec.forSize(maxWidth, maxHeight))
    }
}

/** Card for one character, styled for the current grid. */
@Composable
internal fun CharacterGridCard(
    character: Character,
    spec: CharacterGridSpec,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    CharacterCard(
        name = character.name,
        status = character.status,
        imageUrl = character.imageUrl,
        species = character.species,
        layout = spec.cardLayout,
        selected = selected,
        onClick = onClick
    )
}

/** Non-paged adaptive list/grid of characters (search results, favorites). */
@Composable
internal fun CharacterGrid(
    characters: List<Character>,
    onCharacterClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
    AdaptiveCharacterLayout(modifier = modifier) { spec ->
        LazyVerticalGrid(
            columns = spec.columns,
            state = state,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = contentPadding,
            modifier = Modifier.testTag(CHARACTER_GRID_TAG)
        ) {
            items(items = characters, key = { it.id }) { character ->
                CharacterGridCard(character, spec) { onCharacterClick(character.id) }
            }
        }
    }
}
