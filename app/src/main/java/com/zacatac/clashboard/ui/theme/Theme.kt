package com.zacatac.clashboard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),
    primaryContainer = Color(0xFF1B5E20),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFFE8F5E9),
    secondary = Color(0xFFFF9800),
    secondaryContainer = Color(0xFFE65100),
    onSecondary = Color.Black,
    onSecondaryContainer = Color(0xFFFFF3E0),
    tertiary = Color(0xFF29B6F6),
    tertiaryContainer = Color(0xFF0277BD),
    onTertiary = Color.Black,
    onTertiaryContainer = Color(0xFFE1F5FE),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEEEEEE),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),
    surfaceContainer = Color(0xFF242424),
    surfaceContainerHigh = Color(0xFF2D2D2D),
    surfaceContainerHighest = Color(0xFF353535),
    error = Color(0xFFCF6679),
    onError = Color.Black,
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFF48484A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    primaryContainer = Color(0xFFC8E6C9),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFFE65100),
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondary = Color.White,
    onSecondaryContainer = Color(0xFFBF360C),
    tertiary = Color(0xFF0277BD),
    tertiaryContainer = Color(0xFFB3E5FC),
    onTertiary = Color.White,
    onTertiaryContainer = Color(0xFF01579B),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF202124),
    surface = Color.White,
    onSurface = Color(0xFF202124),
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = Color(0xFF5F6368),
    surfaceContainer = Color(0xFFF1F3F4),
    surfaceContainerHigh = Color(0xFFE8EAED),
    surfaceContainerHighest = Color(0xFFDADCE0),
    error = Color(0xFFB00020),
    onError = Color.White,
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

@Composable
fun Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
