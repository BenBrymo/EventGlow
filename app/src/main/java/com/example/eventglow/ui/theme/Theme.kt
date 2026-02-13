package com.example.eventglow.ui.theme

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

    // 🔥 Brand
    primary = BrandPrimary,
    onPrimary = TextPrimary,
    primaryContainer = BrandPrimaryDark,
    onPrimaryContainer = TextPrimary,

    // 🎨 Secondary Accents
    secondary = AccentBlue,
    onSecondary = TextPrimary,
    tertiary = AccentPurple,
    onTertiary = TextPrimary,

    // 🌑 Background & Surface
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceLevel1,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLevel2,
    onSurfaceVariant = TextSecondary,

    // 🚦 States
    error = Error,
    onError = TextPrimary,

    // 🧱 Outline / Borders
    outline = BorderDefault,
    outlineVariant = BorderSubtle
)


private val LightColorScheme = lightColorScheme(

    // 🔥 Brand
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryLight,
    onPrimaryContainer = Color.Black,

    // 🎨 Secondary
    secondary = AccentBlue,
    onSecondary = Color.White,
    tertiary = AccentPurple,
    onTertiary = Color.White,

    // 🌤 Background
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,

    // 🚦 States
    error = Error,
    onError = Color.White,

    // 🧱 Outline
    outline = LightBorder,
    outlineVariant = LightBorder
)


@Composable
fun EventGlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 🔥 Brand first
    content: @Composable () -> Unit
) {

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme)
                dynamicDarkColorScheme(context)
            else
                dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // Status bar
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme

            // Navigation bar
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
