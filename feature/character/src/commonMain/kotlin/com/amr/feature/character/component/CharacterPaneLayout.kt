package com.amr.feature.character.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Whether the character list shows its detail beside it or on its own. */
enum class CharacterPaneLayout { SinglePane, TwoPane }

/** Width of the list pane in two-pane mode: one column of horizontal cards. */
val CharacterListPaneWidth = 380.dp

/** Narrowest detail pane that still fits the hero image, info card and location cards. */
private val DetailPaneMinWidth = 460.dp

/** Space taken by the divider between the panes. */
private val PaneGap = 1.dp

/** Below this height (phone landscape) a side-by-side detail would be a sliver; stay single. */
private val TwoPaneMinHeight = 480.dp

/**
 * Picks the layout from the space the screen actually gets (after the navigation
 * rail), not from device type or orientation: two panes only when both the list
 * and a usable detail fit, and there is enough height to read the detail.
 */
fun characterPaneLayout(availableWidth: Dp, availableHeight: Dp): CharacterPaneLayout =
    if (availableWidth >= CharacterListPaneWidth + PaneGap + DetailPaneMinWidth && availableHeight >= TwoPaneMinHeight) {
        CharacterPaneLayout.TwoPane
    } else {
        CharacterPaneLayout.SinglePane
    }
