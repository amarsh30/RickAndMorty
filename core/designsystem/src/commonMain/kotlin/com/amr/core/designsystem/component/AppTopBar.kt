package com.amr.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.amr.core.designsystem.icon.AppIcons

const val BACK_LABEL = "Kembali"
const val REFRESH_LABEL = "Muat ulang"

/**
 * Material 3 top app bar used by every screen: a single-line title (announced as a
 * heading), an optional back button and optional icon actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
        },
        navigationIcon = { onBack?.let { AppBackButton(onClick = it) } },
        actions = actions,
        modifier = modifier
    )
}

/** Back arrow; IconButton gives the 48dp target, and "Kembali" is its spoken label. */
@Composable
fun AppBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppBarAction(icon = AppIcons.ArrowBack, label = BACK_LABEL, onClick = onClick, modifier = modifier)
}

/**
 * Refresh button: the non-gesture alternative to pull-to-refresh for screen-reader,
 * keyboard and switch users. Disabled while a refresh is running, so it is never
 * announced as actionable when a tap would do nothing.
 */
@Composable
fun AppRefreshAction(isRefreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    AppBarAction(
        icon = AppIcons.Refresh,
        label = REFRESH_LABEL,
        onClick = onRefresh,
        enabled = !isRefreshing,
        modifier = modifier
    )
}

/** Icon-only app bar action; [label] is its content description. */
@Composable
fun AppBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // M3 IconButton lays out at 40dp; keep the whole 48dp target inside the button itself.
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).focusRing(CircleShape)
    ) {
        Icon(imageVector = icon, contentDescription = label)
    }
}
