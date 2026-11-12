
package com.amr.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.amr.core.designsystem.icon.AppIcons

/** Empty state; kept as a thin wrapper over [AppMessage] so existing callers are unchanged. */
@Composable
fun AppEmpty(
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    AppMessage(
        icon = AppIcons.Info,
        title = message,
        modifier = modifier,
        actionText = actionText,
        onAction = onAction
    )
}
