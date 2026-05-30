package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SophisticatedDarkColorScheme = darkColorScheme(
    primary = SophPrimary,
    onPrimary = SophOnPrimary,
    secondary = SophActiveSelection,
    onSecondary = SophTextMain,
    background = SophBackground,
    onBackground = SophTextMain,
    surface = SophSurface,
    onSurface = SophTextMain,
    surfaceVariant = SophActiveSelection,
    onSurfaceVariant = SophTextSecondary,
    error = SophError,
    onError = SophOnPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force sophisticated dark mode
    dynamicColor: Boolean = false, // Disable dynamic content mapping to retain unique look
    content: @Composable () -> Unit,
) {
    // We enforce our premium "Sophisticated Dark" palette
    MaterialTheme(
        colorScheme = SophisticatedDarkColorScheme,
        typography = Typography,
        content = content
    )
}
