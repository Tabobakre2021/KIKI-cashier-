package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = KikiPrimary,
    onPrimary = KikiOnPrimary,
    primaryContainer = KikiPrimaryContainer,
    onPrimaryContainer = KikiOnPrimaryContainer,
    secondary = KikiSecondary,
    onSecondary = KikiOnSecondary,
    secondaryContainer = KikiSecondaryContainer,
    onSecondaryContainer = KikiOnSecondaryContainer,
    tertiary = KikiTertiary,
    onTertiary = KikiOnTertiary,
    tertiaryContainer = KikiTertiaryContainer,
    onTertiaryContainer = KikiOnTertiaryContainer,
    background = KikiSurface,
    onBackground = KikiInk,
    surface = KikiSurface,
    onSurface = KikiInk,
    surfaceVariant = KikiSurfaceContainerHigh,
    onSurfaceVariant = KikiInkMuted,
    surfaceContainerLowest = KikiSurfaceContainerLowest,
    surfaceContainerLow = KikiSurfaceContainerLow,
    surfaceContainer = KikiSurfaceContainer,
    surfaceContainerHigh = KikiSurfaceContainerHigh,
    surfaceContainerHighest = KikiSurfaceContainerHighest,
    outline = KikiOutline,
    outlineVariant = KikiOutlineVariant,
    error = KikiErrorText,
    onError = Color.White,
    errorContainer = KikiErrorBg,
    onErrorContainer = KikiErrorText
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB2C0),
    onPrimary = Color(0xFF560A23),
    primaryContainer = KikiPrimary,
    onPrimaryContainer = KikiPrimaryContainer,
    secondary = KikiGold,
    onSecondary = Color(0xFF261900),
    secondaryContainer = Color(0xFF5A4300),
    onSecondaryContainer = KikiSecondaryContainer,
    tertiary = Color(0xFFFFB3BA),
    onTertiary = Color(0xFF5F0817),
    tertiaryContainer = Color(0xFF8B172C),
    onTertiaryContainer = KikiTertiaryContainer,
    background = KikiDarkSurface,
    onBackground = KikiDarkText,
    surface = KikiDarkSurface,
    onSurface = KikiDarkText,
    surfaceVariant = KikiDarkSurfaceContainerHigh,
    onSurfaceVariant = KikiDarkTextMuted,
    surfaceContainerLowest = KikiDarkSurfaceContainerLowest,
    surfaceContainerLow = KikiDarkSurfaceContainerLow,
    surfaceContainer = KikiDarkSurfaceContainer,
    surfaceContainerHigh = KikiDarkSurfaceContainerHigh,
    surfaceContainerHighest = KikiDarkSurfaceContainerHighest,
    outline = KikiDarkOutline,
    outlineVariant = KikiDarkOutlineVariant,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

val KikiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KikiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KikiTypography,
        shapes = KikiShapes,
        content = content
    )
}
