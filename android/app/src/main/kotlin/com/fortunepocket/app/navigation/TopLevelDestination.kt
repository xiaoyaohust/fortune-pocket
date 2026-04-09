package com.fortunepocket.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.fortunepocket.R

enum class TopLevelDestination(
    val route: String,
    val titleResId: Int,
    val icon: ImageVector
) {
    HOME(
        route = "home",
        titleResId = R.string.nav_home,
        icon = Icons.Outlined.AutoStories
    ),
    HISTORY(
        route = "history",
        titleResId = R.string.nav_history,
        icon = Icons.Outlined.History
    ),
    SETTINGS(
        route = "settings",
        titleResId = R.string.nav_settings,
        icon = Icons.Outlined.Settings
    )
}
