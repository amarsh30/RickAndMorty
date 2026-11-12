package com.amr.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Modern Sci-Fi Minimalist palette.
 * Portal Green is the primary accent, Cosmic Blue the secondary one.
 * Light text/surface pairs are chosen for >= 4.5:1 contrast (WCAG AA body text).
 */

// Portal Green
private val PortalGreen10 = Color(0xFF0B2000)
private val PortalGreen30 = Color(0xFF285500)
private val PortalGreen40 = Color(0xFF356F00)
private val PortalGreen80 = Color(0xFF9BDB5B)
private val PortalGreen90 = Color(0xFFB7F874)

// Cosmic Blue
private val CosmicBlue10 = Color(0xFF001C38)
private val CosmicBlue30 = Color(0xFF00497F)
private val CosmicBlue40 = Color(0xFF0061A6)
private val CosmicBlue80 = Color(0xFF9ECAFF)
private val CosmicBlue90 = Color(0xFFD1E4FF)

// Nebula (tertiary, used sparingly)
private val Nebula30 = Color(0xFF4F3B7A)
private val Nebula40 = Color(0xFF675293)
private val Nebula80 = Color(0xFFD3BBFF)
private val Nebula90 = Color(0xFFEBDDFF)

// Neutrals with a cold blue tint ("space")
private val Space4 = Color(0xFF080D14)
private val Space6 = Color(0xFF0D131B)
private val Space10 = Color(0xFF161C24)
private val Space12 = Color(0xFF1A2028)
private val Space17 = Color(0xFF242A33)
private val Space22 = Color(0xFF2F353E)
private val Space30 = Color(0xFF41474F)
private val Space60 = Color(0xFF8B919A)
private val Space80 = Color(0xFFC1C7D0)
private val Space90 = Color(0xFFDDE3EC)
private val Space94 = Color(0xFFEAEEF5)
private val Space96 = Color(0xFFF0F3F9)
private val Space98 = Color(0xFFF7F9FE)
private val Space100 = Color(0xFFFFFFFF)

private val ErrorRed40 = Color(0xFFBA1A1A)
private val ErrorRed80 = Color(0xFFFFB4AB)
private val ErrorRed90 = Color(0xFFFFDAD6)
private val ErrorRed10 = Color(0xFF410002)
private val ErrorRed30 = Color(0xFF93000A)

internal val LightColors = lightColorScheme(
    primary = PortalGreen40,
    onPrimary = Space100,
    primaryContainer = PortalGreen90,
    onPrimaryContainer = PortalGreen10,
    secondary = CosmicBlue40,
    onSecondary = Space100,
    secondaryContainer = CosmicBlue90,
    onSecondaryContainer = CosmicBlue10,
    tertiary = Nebula40,
    onTertiary = Space100,
    tertiaryContainer = Nebula90,
    onTertiaryContainer = Nebula30,
    error = ErrorRed40,
    onError = Space100,
    errorContainer = ErrorRed90,
    onErrorContainer = ErrorRed10,
    background = Space98,
    onBackground = Space10,
    surface = Space98,
    onSurface = Space10,
    surfaceVariant = Space90,
    onSurfaceVariant = Space30,
    surfaceContainerLowest = Space100,
    surfaceContainerLow = Space96,
    surfaceContainer = Space94,
    surfaceContainerHigh = Space90,
    surfaceContainerHighest = Space80,
    outline = Space60,
    outlineVariant = Space80
)

internal val DarkColors = darkColorScheme(
    primary = PortalGreen80,
    onPrimary = PortalGreen10,
    primaryContainer = PortalGreen30,
    onPrimaryContainer = PortalGreen90,
    secondary = CosmicBlue80,
    onSecondary = CosmicBlue10,
    secondaryContainer = CosmicBlue30,
    onSecondaryContainer = CosmicBlue90,
    tertiary = Nebula80,
    onTertiary = Nebula30,
    tertiaryContainer = Nebula30,
    onTertiaryContainer = Nebula90,
    error = ErrorRed80,
    onError = ErrorRed10,
    errorContainer = ErrorRed30,
    onErrorContainer = ErrorRed90,
    background = Space6,
    onBackground = Space90,
    surface = Space6,
    onSurface = Space90,
    surfaceVariant = Space30,
    onSurfaceVariant = Space80,
    surfaceContainerLowest = Space4,
    surfaceContainerLow = Space10,
    surfaceContainer = Space12,
    surfaceContainerHigh = Space17,
    surfaceContainerHighest = Space22,
    outline = Space60,
    outlineVariant = Space30
)

/** Semantic colors Material 3 has no slot for: character life status. */
@Immutable
data class StatusColors(
    val alive: Color,
    val onAliveContainer: Color,
    val aliveContainer: Color,
    val dead: Color,
    val onDeadContainer: Color,
    val deadContainer: Color,
    val unknown: Color,
    val onUnknownContainer: Color,
    val unknownContainer: Color
)

internal val LightStatusColors = StatusColors(
    alive = PortalGreen40,
    onAliveContainer = PortalGreen10,
    aliveContainer = PortalGreen90,
    dead = ErrorRed40,
    onDeadContainer = ErrorRed10,
    deadContainer = ErrorRed90,
    unknown = Space30,
    onUnknownContainer = Space10,
    unknownContainer = Space90
)

internal val DarkStatusColors = StatusColors(
    alive = PortalGreen80,
    onAliveContainer = PortalGreen90,
    aliveContainer = PortalGreen30,
    dead = ErrorRed80,
    onDeadContainer = ErrorRed90,
    deadContainer = ErrorRed30,
    unknown = Space80,
    onUnknownContainer = Space90,
    unknownContainer = Space30
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
