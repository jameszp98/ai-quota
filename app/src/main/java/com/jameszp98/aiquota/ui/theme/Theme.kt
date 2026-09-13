package com.jameszp98.aiquota.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF89B4FA),
    onPrimary = Color(0xFF1E1E2E),
    primaryContainer = Color(0xFF313244),
    onPrimaryContainer = Color(0xFFCDD6F4),
    secondary = Color(0xFFA6E3A1),
    onSecondary = Color(0xFF1E1E2E),
    secondaryContainer = Color(0xFF313244),
    onSecondaryContainer = Color(0xFFA6E3A1),
    tertiary = Color(0xFFF9E2AF),
    onTertiary = Color(0xFF1E1E2E),
    tertiaryContainer = Color(0xFF313244),
    onTertiaryContainer = Color(0xFFF9E2AF),
    error = Color(0xFFF38BA8),
    onError = Color(0xFF1E1E2E),
    errorContainer = Color(0xFF45475A),
    onErrorContainer = Color(0xFFF38BA8),
    background = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
    surface = Color(0xFF1E1E2E),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF313244),
    onSurfaceVariant = Color(0xFFBAC2DE),
    outline = Color(0xFF6C7086)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E66F5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE6E9EF),
    onPrimaryContainer = Color(0xFF1E66F5),
    secondary = Color(0xFF40A02B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E9EF),
    onSecondaryContainer = Color(0xFF40A02B),
    tertiary = Color(0xFFDF8E1D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE6E9EF),
    onTertiaryContainer = Color(0xFFDF8E1D),
    error = Color(0xFFD20F39),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFE6E9EF),
    onErrorContainer = Color(0xFFD20F39),
    background = Color(0xFFEFF1F5),
    onBackground = Color(0xFF4C4F69),
    surface = Color(0xFFEFF1F5),
    onSurface = Color(0xFF4C4F69),
    surfaceVariant = Color(0xFFE6E9EF),
    onSurfaceVariant = Color(0xFF6C6F85),
    outline = Color(0xFF9CA0B0)
)

@Composable
fun AIQuotaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
