package com.zacatac.clashboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),      // Green
    primaryContainer = Color(0xFF2E7D32),
    secondary = Color(0xFFFF9800),    // Orange
    secondaryContainer = Color(0xFFE65100),
    tertiary = Color(0xFF2196F3),     // Blue
    tertiaryContainer = Color(0xFF0D47A1),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    error = Color(0xFFCF6679),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black,
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFF49454F)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),      // Dark Green
    primaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFE65100),    // Dark Orange
    secondaryContainer = Color(0xFFFFE0B2),
    tertiary = Color(0xFF0D47A1),     // Dark Blue
    tertiaryContainer = Color(0xFFBBDEFB),
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF5F5F5),
    error = Color(0xFFB00020),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onError = Color.White,
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0)
)

@Composable
fun Theme(
    darkTheme: Boolean = false, // Will be overridden by system or preference
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
