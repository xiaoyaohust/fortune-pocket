package com.fortunepocket.feature.settings

data class SettingsUiState(
    val notificationEnabled: Boolean = false,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val birthdayMillis: Long = -1L,
    val appLanguage: String = "system"
)
