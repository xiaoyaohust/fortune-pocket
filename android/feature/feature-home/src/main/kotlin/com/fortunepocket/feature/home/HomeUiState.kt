package com.fortunepocket.feature.home

data class HomeUiState(
    val dailyRitual: DailyRitual? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
