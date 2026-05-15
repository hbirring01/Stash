package com.example.creditcardapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = Accent,
    onSecondary = AccentOn,
    tertiary = Accent,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = Outline,
    outlineVariant = Outline,
    error = Danger
)

private val DarkColors = darkColorScheme(
    primary = InkDark,
    onPrimary = PaperDark,
    secondary = AccentDark,
    onSecondary = AccentOnDark,
    tertiary = AccentDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = InkMutedDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark,
    error = Danger
)

@Composable
fun CreditCardAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
