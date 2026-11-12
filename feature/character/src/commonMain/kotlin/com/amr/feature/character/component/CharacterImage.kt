
package com.amr.feature.character.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.amr.core.designsystem.component.AvatarFallback

/**
 * Character portrait. Uses [AsyncImage] (no per-item subcomposition or spinner):
 * the tinted box shows while loading, and [AvatarFallback] replaces it when the
 * URL is missing or loading fails. Pass `contentDescription = null` when a
 * nearby text already names the character.
 */
@Composable
fun CharacterImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var failed by remember(imageUrl) { mutableStateOf(false) }
    val shaped = modifier.clip(MaterialTheme.shapes.small)

    if (imageUrl.isNullOrBlank() || failed) {
        AvatarFallback(shaped)
        return
    }
    Box(modifier = shaped.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            onError = { failed = true },
            modifier = Modifier.fillMaxSize()
        )
    }
}
