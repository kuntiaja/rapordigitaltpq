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
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = JadeSecondary,
    onSecondary = Color.White,
    secondaryContainer = JadeContainer,
    onSecondaryContainer = OnJadeContainer,
    tertiary = GoldTertiary,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = CreamBackground,
    onBackground = Color(0xFF101C19),
    surface = PureSurface,
    onSurface = Color(0xFF101C19),
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = Color(0xFF42544E),
    outline = OutlineMist,
    outlineVariant = Color(0xFFDCE5E1),
    error = AlphaRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = Color(0xFF00382E),
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkOnEmeraldContainer,
    secondary = DarkJadeSecondary,
    onSecondary = Color(0xFF05382E),
    secondaryContainer = Color(0xFF154E43),
    onSecondaryContainer = Color(0xFFB3F3E4),
    tertiary = DarkGoldTertiary,
    onTertiary = Color(0xFF3E2900),
    tertiaryContainer = Color(0xFF5A3D00),
    onTertiaryContainer = Color(0xFFFFE2AA),
    background = DarkBackground,
    onBackground = Color(0xFFE6F0EC),
    surface = DarkSurface,
    onSurface = Color(0xFFE6F0EC),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB3C7C0),
    outline = Color(0xFF3D564E),
    outlineVariant = Color(0xFF233832),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RaporTpqTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
