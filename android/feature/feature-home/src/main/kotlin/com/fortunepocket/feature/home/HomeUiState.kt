package com.fortunepocket.feature.home

import com.fortunepocket.core.model.DailyQuote

data class HomeUiState(
    val dailyQuote: DailyQuote? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
