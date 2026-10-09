package com.rudra.smartworktracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Accent choices offered on the Appearance screen; index 0 is the brand indigo. */
val AccentColors = listOf(
    Color(0xFF6366F1) to "Indigo",
    Color(0xFF10B981) to "Emerald",
    Color(0xFFF59E0B) to "Amber",
    Color(0xFFEF4444) to "Red",
    Color(0xFF3B82F6) to "Blue",
    Color(0xFF8B5CF6) to "Violet",
    Color(0xFFEC4899) to "Pink",
    Color(0xFF14B8A6) to "Teal"
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    // Container tones follow the slate palette instead of Material's purple-grey baseline
    surfaceContainerLowest = Color(0xFF0B1222),
    surfaceContainerLow = Color(0xFF162032),
    surfaceContainer = Color(0xFF1E293B),
    surfaceContainerHigh = Color(0xFF273449),
    surfaceContainerHighest = Color(0xFF334155),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE9EEF4),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0)
)

/** Re-tints the primary color roles of this scheme with [accent]. */
private fun ColorScheme.withAccent(accent: Color, dark: Boolean): ColorScheme {
    val primary = if (dark) lerp(accent, Color.White, 0.25f) else accent
    return copy(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = if (dark) lerp(accent, Color.Black, 0.55f) else lerp(accent, Color.White, 0.85f),
        onPrimaryContainer = if (dark) lerp(accent, Color.White, 0.8f) else lerp(accent, Color.Black, 0.6f),
        inversePrimary = if (dark) accent else lerp(accent, Color.White, 0.4f),
        surfaceTint = primary
    )
}

@Composable
fun SmartWorkTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    accentColorIndex: Int = 0,
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> {
            val accent = AccentColors.getOrNull(accentColorIndex)?.first ?: AccentColors.first().first
            (if (darkTheme) DarkColorScheme else LightColorScheme).withAccent(accent, darkTheme)
        }
    }

    // The Appearance "Text Size" slider multiplies the system font scale
    val density = LocalDensity.current
    val scaledDensity = Density(density.density, density.fontScale * fontScale.coerceIn(0.8f, 1.4f))

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
