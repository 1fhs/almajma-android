package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AlMajmaPrimary,
    secondary = AlMajmaSecondary,
    tertiary = AlMajmaTertiary,
    background = AlMajmaDarkBg,
    surface = AlMajmaSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = AlMajmaTextPrimary,
    onSurface = AlMajmaTextPrimary,
    surfaceVariant = AlMajmaSurfaceCard,
    onSurfaceVariant = AlMajmaTextSecondary,
    outline = AlMajmaBorderColor,
    error = AlMajmaError
)

private val LightColorScheme = lightColorScheme(
    primary = AlMajmaLightPrimary,
    secondary = AlMajmaLightSecondary,
    tertiary = AlMajmaLightTertiary,
    background = AlMajmaLightBg,
    surface = AlMajmaLightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = AlMajmaLightError
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force energy-saving dark theme by default
    dynamicPrimary: Color? = null,
    dynamicSecondary: Color? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = if (dynamicPrimary != null || dynamicSecondary != null) {
        baseScheme.copy(
            primary = dynamicPrimary ?: baseScheme.primary,
            secondary = dynamicSecondary ?: baseScheme.secondary
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
