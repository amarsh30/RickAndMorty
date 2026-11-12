
package com.amr.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.amr.core.designsystem.icon.AppIcons

/** Error state with retry; a thin wrapper over [AppMessage] so existing callers are unchanged. */
@Composable
fun AppError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryText: String = "Coba Lagi"
) {
    AppMessage(
        icon = AppIcons.ErrorOutline,
        title = message,
        modifier = modifier,
        tone = MessageTone.Error,
        actionText = retryText,
        onAction = onRetry
    )
}
