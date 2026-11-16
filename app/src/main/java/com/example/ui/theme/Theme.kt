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
    primary = KikiBurgundy,
    onPrimary = Color.White,
    primaryContainer = KikiCrimsonSoft,
    onPrimaryContainer = KikiBurgundyDeep,
    secondary = KikiGold,
    onSecondary = KikiInk,
    secondaryContainer = KikiGoldSoft,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = KikiCrimson,
    onTertiary = Color.White,
    background = KikiCreamBackground,
    onBackground = KikiInk,
    surface = KikiCreamSurface,
    onSurface = KikiInk,
    surfaceVariant = KikiSandContainer,
    onSurfaceVariant = KikiInkMuted,
    outline = KikiBorder,
    outlineVariant = KikiSandVariant,
    error = KikiExpenseRed,
    onError = Color.White,
    errorContainer = KikiExpenseRedSoft,
    onErrorContainer = KikiExpenseRed
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE86A8D),
    onPrimary = Color(0xFF47081E),
    primaryContainer = KikiBurgundy,
    onPrimaryContainer = Color(0xFFFFD9E2),
    secondary = KikiGold,
    onSecondary = KikiInk,
    secondaryContainer = Color(0xFF5C3D04),
    onSecondaryContainer = KikiGoldSoft,
    tertiary = Color(0xFFFF7A90),
    onTertiary = Color(0xFF500014),
    background = KikiDarkBackground,
    onBackground = KikiDarkText,
    surface = KikiDarkSurface,
    onSurface = KikiDarkText,
    surfaceVariant = KikiDarkContainer,
    onSurfaceVariant = KikiDarkTextMuted,
    outline = Color(0xFF4F3840),
    outlineVariant = Color(0xFF3A272E),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF5C0A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

val KikiShapes = Shapes(
    small = RoundedCornerShape(10.dp),
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
