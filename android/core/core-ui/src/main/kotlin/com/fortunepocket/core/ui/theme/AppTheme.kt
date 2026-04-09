package com.fortunepocket.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fortune Pocket always uses its own dark, mystical theme.
// We ignore system dark/light mode and always apply the brand palette.
private val FortunePocketColorScheme = darkColorScheme(
    primary = AppColors.accentGold,
    onPrimary = AppColors.backgroundDeep,
    primaryContainer = AppColors.backgroundElevated,
    onPrimaryContainer = AppColors.accentGoldLight,
    secondary = AppColors.accentPurple,
    onSecondary = AppColors.textPrimary,
    secondaryContainer = AppColors.backgroundElevated,
    onSecondaryContainer = AppColors.textSecondary,
    tertiary = AppColors.accentRose,
    onTertiary = AppColors.textPrimary,
    background = AppColors.backgroundDeep,
    onBackground = AppColors.textPrimary,
    surface = AppColors.backgroundBase,
    onSurface = AppColors.textPrimary,
    surfaceVariant = AppColors.backgroundElevated,
    onSurfaceVariant = AppColors.textSecondary,
    outline = AppColors.divider,
    outlineVariant = AppColors.textMuted,
    error = AppColors.error,
    onError = Color.White
)

@Composable
fun FortunePocketTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FortunePocketColorScheme,
        typography = FortunePocketTypography,
        content = content
    )
}
