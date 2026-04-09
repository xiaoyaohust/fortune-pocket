package com.fortunepocket.core.ui.theme

import androidx.compose.ui.graphics.Color

object AppColors {
    // Background layers
    val backgroundDeep = Color(0xFF0E0A1F)
    val backgroundBase = Color(0xFF1A1035)
    val backgroundElevated = Color(0xFF251848)

    // Accent
    val accentGold = Color(0xFFC9A84C)
    val accentGoldLight = Color(0xFFE8D48B)
    val accentPurple = Color(0xFF7B5EA7)
    val accentRose = Color(0xFFC9748F)

    // Text
    val textPrimary = Color(0xFFF0EBE3)
    val textSecondary = Color(0xFF9E96B8)
    val textMuted = Color(0xFF5D5580)

    // UI
    val divider = Color(0xFF2E2456)
    val starGlow = Color(0xFFFFE566)

    // Semantic
    val success = Color(0xFF27AE60)
    val warning = Color(0xFFF39C12)
    val error = Color(0xFFE74C3C)

    // Gradient pairs (for use with Brush.linearGradient)
    val gradientGold = listOf(accentGold, accentGoldLight)
    val gradientBackground = listOf(backgroundDeep, backgroundBase)
}
