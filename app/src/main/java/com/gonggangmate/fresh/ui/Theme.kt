package com.gonggangmate.fresh.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF173D32)
val Muted = Color(0xFF78867C)
val Green = Color(0xFF28634B)
val Paper = Color(0xFFF8F8F0)
val Mint = Color(0xFFE5EEDD)
val Line = Color(0xFFE1E7DC)
val Sun = Color(0xFFDFBA72)

@Composable fun MateTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White,
        secondary = Color(0xFF778B56), background = Paper, onBackground = Ink,
        surface = Color.White, onSurface = Ink, surfaceVariant = Mint, onSurfaceVariant = Muted,
        outline = Line, error = Color(0xFFAE4B38)),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 40.sp, letterSpacing = (-1).sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 25.sp, lineHeight = 34.sp, letterSpacing = (-.7).sp),
            titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp)), content = content)
}
