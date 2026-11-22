package com.smartexam.academy.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium palette: deep indigo + royal gold.
 * Names (Orange / Navy) are kept so every screen picks up the new look automatically.
 */
object Brand {
    val Orange = Color(0xFFD99A1E)      // royal gold (accent)
    val OrangeDark = Color(0xFFA9720A)
    val OrangeSoft = Color(0xFFFFF5DD)
    val Gold = Color(0xFFF5C451)
    val GoldLight = Color(0xFFFFE29A)
    val Navy = Color(0xFF1B1F5E)        // deep indigo
    val NavyLight = Color(0xFF3442A8)
    val Indigo = Color(0xFF283593)
    val Violet = Color(0xFF5E35B1)
    val Green = Color(0xFF2E7D32)
    val GreenSoft = Color(0xFFE8F5E9)
    val Red = Color(0xFFD32F2F)
    val RedSoft = Color(0xFFFFEBEE)
    val Purple = Color(0xFF6A1B9A)
    val PurpleSoft = Color(0xFFF3E5F5)
    val Grey = Color(0xFF8A94A6)
    val Teal = Color(0xFF00897B)

    val HeaderGradient get() = Brush.linearGradient(listOf(Color(0xFF12164A), Navy, NavyLight))
    val GoldGradient get() = Brush.linearGradient(listOf(Gold, Orange, OrangeDark))
    val AiGradient get() = Brush.linearGradient(listOf(Violet, NavyLight, Color(0xFF00ACC1)))
}

private val Light = lightColorScheme(
    primary = Brand.Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E7FB),
    onPrimaryContainer = Brand.Navy,
    secondary = Brand.Orange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF1D2),
    onSecondaryContainer = Color(0xFF5A3D00),
    tertiary = Brand.Violet,
    background = Color(0xFFF4F5FB),
    onBackground = Color(0xFF14172B),
    surface = Color.White,
    onSurface = Color(0xFF14172B),
    surfaceVariant = Color(0xFFECEEF7),
    onSurfaceVariant = Color(0xFF5A6077),
    outline = Color(0xFFD6D9E8),
    error = Brand.Red,
)

private val Dark = darkColorScheme(
    primary = Color(0xFFB9C1FF),
    onPrimary = Color(0xFF0E1250),
    primaryContainer = Color(0xFF2A3180),
    onPrimaryContainer = Color(0xFFE0E3FF),
    secondary = Brand.Gold,
    onSecondary = Color(0xFF3A2600),
    secondaryContainer = Color(0xFF4A3608),
    onSecondaryContainer = Color(0xFFFFE6A8),
    tertiary = Color(0xFFCDB8FF),
    background = Color(0xFF0D0F1E),
    onBackground = Color(0xFFE6E8F4),
    surface = Color(0xFF171A2E),
    onSurface = Color(0xFFE6E8F4),
    surfaceVariant = Color(0xFF242842),
    onSurfaceVariant = Color(0xFFAAB0C8),
    outline = Color(0xFF3A3F5E),
    error = Color(0xFFFF8A80),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

/** themeMode: 0 = system, 1 = day, 2 = night */
@Composable
fun AppTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = AppTypography, shapes = AppShapes, content = content)
}
