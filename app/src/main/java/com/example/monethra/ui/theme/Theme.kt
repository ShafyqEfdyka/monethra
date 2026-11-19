package com.example.monethra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightColorScheme = lightColorScheme(
    primary = PowderBlue,
    onPrimary = PrimaryText,
    primaryContainer = SoftLavender,
    onPrimaryContainer = PrimaryText,
    secondary = SoftLavender,
    onSecondary = PrimaryText,
    secondaryContainer = ButterCream,
    onSecondaryContainer = PrimaryText,
    tertiary = SageGreen,
    onTertiary = PrimaryText,
    tertiaryContainer = DustyPink,
    onTertiaryContainer = PrimaryText,
    background = WarmIvory,
    onBackground = PrimaryText,
    surface = White,
    onSurface = PrimaryText,
    surfaceVariant = WarmIvory,
    onSurfaceVariant = SecondaryText,
    inverseSurface = DeepIndigo,
    inverseOnSurface = White,
    inversePrimary = PastelPeriwinkle,
    error = DustyPink,
    onError = PrimaryText,
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFF0F0F0),
)

@Composable
fun MonethraTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        shapes = Shapes,
        typography = Typography,
        content = content,
    )
}
