package org.mobile.growfy.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F7A55),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDEFD6),
    onPrimaryContainer = Color(0xFF00210F),
    secondaryContainer = Color(0xFFBDEFD6),
    onSecondaryContainer = Color(0xFF00210F),
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF2A1800),
    background = Color(0xFFF6FBF7),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF6FBF7),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDCE5DF),
    onSurfaceVariant = Color(0xFF404943),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F5F1),
    surfaceContainer = Color(0xFFEAF0EC),
    surfaceContainerHigh = Color(0xFFEAF0EC),
    surfaceContainerHighest = Color(0xFFDCE5DF),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFDCE5DF),
    inverseSurface = Color(0xFF2C322E),
    inverseOnSurface = Color(0xFFEEF3EF),
    inversePrimary = Color(0xFFA1D4BA),
    error = Color(0xFFB3261E),
)

/** Colors the design uses outside the Material roles. */
object GrowfyColors {
    val Star = Color(0xFFE8A200)
    val StarEmpty = Color(0xFFD3DDD6)
    val Pending = Color(0xFFB26B00)
    val Negative = Color(0xFFB3261E)
    val DraftContainer = Color(0xFFE4EAE6)
    val SelectedCard = Color(0xFFF0F8F3)
    val Disabled = Color(0xFFC9D1CB)
    val Ink = Color(0xFF111110)
    /** The API has no brand colour per location, so each gets a stable one from the design's palette. */
    fun location(id: String): Color = LocationPalette[(id.hashCode() and Int.MAX_VALUE) % LocationPalette.size]

    private val LocationPalette = listOf(
        Color(0xFF2563C9), Color(0xFFE2562B), Color(0xFF7B4BC4), Color(0xFF00838F), Color(0xFFC2185B), Color(0xFF5D7A1F),
    )
    val PhotoGradient = listOf(Color(0xFFD9EBE2), Color(0xFFB8D6C7))
    val AvatarPalettes = listOf(
        Color(0xFFE9DDFF) to Color(0xFF22005D),
        Color(0xFFD6E3FF) to Color(0xFF001B3E),
        Color(0xFFFFDAD6) to Color(0xFF410002),
        Color(0xFFFFDDB3) to Color(0xFF2A1800),
        Color(0xFFBDEFD6) to Color(0xFF00210F),
    )
}

// The design only defines a light theme; add a dark scheme here once it exists.
@Composable
fun GrowfyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
