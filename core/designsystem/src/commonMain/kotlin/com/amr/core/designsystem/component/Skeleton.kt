package com.amr.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.theme.AppThemeTokens

const val SKELETON_LIST_TAG = "skeleton_list"

/** A flat placeholder block; no shimmer, to keep it cheap and calm. */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.small
        )
    )
}

/** Card-shaped placeholder sized like a list card; [avatarSize] adds an image block. */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier, avatarSize: Dp? = null) {
    val spacing = AppThemeTokens.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(spacing.md),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        avatarSize?.let { SkeletonBox(Modifier.size(it)) }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            SkeletonBox(Modifier.fillMaxWidth(0.6f).height(18.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.35f).height(14.dp))
        }
    }
}

/**
 * Initial-loading placeholder list. Screen readers get a single "loading"
 * node (an indeterminate progress) instead of the fake rows.
 */
@Composable
fun SkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 6,
    avatarSize: Dp? = null,
    contentPadding: PaddingValues = PaddingValues(AppThemeTokens.spacing.lg),
    loadingDescription: String = "Memuat"
) {
    Column(
        modifier = modifier
            .testTag(SKELETON_LIST_TAG)
            .clearAndSetSemantics {
                contentDescription = loadingDescription
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(AppThemeTokens.spacing.md)
    ) {
        repeat(count) { SkeletonCard(avatarSize = avatarSize) }
    }
}
