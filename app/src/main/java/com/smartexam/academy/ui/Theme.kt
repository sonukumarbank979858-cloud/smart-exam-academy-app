package com.smartexam.academy.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Brand {
    val Orange = Color(0xFFF57C00)
    val OrangeDark = Color(0xFFE65100)
    val OrangeSoft = Color(0xFFFFF3E0)
    val Navy = Color(0xFF0F2A4A)
    val NavyLight = Color(0xFF1E4A7A)
    val Green = Color(0xFF2E7D32)
    val GreenSoft = Color(0xFFE8F5E9)
    val Red = Color(0xFFD32F2F)
    val RedSoft = Color(0xFFFFEBEE)
    val Purple = Color(0xFF6A1B9A)
    val PurpleSoft = Color(0xFFF3E5F5)
    val Grey = Color(0xFF8A94A6)
    val Teal = Color(0xFF00897B)
}

private val Light = lightColorScheme(
    primary = Brand.Orange,
    onPrimary = Color.White,
    primaryContainer = Brand.OrangeSoft,
    onPrimaryContainer = Brand.OrangeDark,
    secondary = Brand.Navy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EAF4),
    onSecondaryContainer = Brand.Navy,
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF14181F),
    surface = Color.White,
    onSurface = Color(0xFF14181F),
    surfaceVariant = Color(0xFFEEF1F6),
    onSurfaceVariant = Color(0xFF5A6474),
    outline = Color(0xFFD5DAE3),
    error = Brand.Red,
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFF9A3D),
    onPrimary = Color(0xFF2A1600),
    primaryContainer = Color(0xFF4A2A00),
    onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Color(0xFF9EC3F0),
    onSecondary = Color(0xFF07203D),
    secondaryContainer = Color(0xFF1E3A5C),
    onSecondaryContainer = Color(0xFFD6E6FA),
    background = Color(0xFF0E1218),
    onBackground = Color(0xFFE6E9EF),
    surface = Color(0xFF171C24),
    onSurface = Color(0xFFE6E9EF),
    surfaceVariant = Color(0xFF232A35),
    onSurfaceVariant = Color(0xFFA8B1C0),
    outline = Color(0xFF3A4352),
    error = Color(0xFFFF8A80),
)

/** themeMode: 0 = system, 1 = day, 2 = night */
@Composable
fun AppTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
