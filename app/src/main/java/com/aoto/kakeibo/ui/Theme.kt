package com.aoto.kakeibo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6F5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9E7DA),
    onPrimaryContainer = Color(0xFF06281F),
    secondary = Color(0xFF4C8C7B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD9C7),
    onPrimary = Color(0xFF06281F),
    primaryContainer = Color(0xFF235547),
    onPrimaryContainer = Color(0xFFB9E7DA),
    secondary = Color(0xFFA7E0D2)
)

@Composable
fun KakeiboTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
