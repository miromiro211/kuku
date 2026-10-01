package com.gonggangmate.fresh.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF302842)
val Muted = Color(0xFF82768F)
val Lilac = Color(0xFF7860B4)
val Paper = Color(0xFFF8F6FC)
val Mint = Color(0xFFE1F0DE)

@Composable fun MateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Lilac, onPrimary = Color.White, secondary = Color(0xFF648360),
        background = Paper, onBackground = Ink, surface = Color.White, onSurface = Ink,
        surfaceVariant = Color(0xFFF0EAF7), onSurfaceVariant = Muted
    ), content = content)
}
