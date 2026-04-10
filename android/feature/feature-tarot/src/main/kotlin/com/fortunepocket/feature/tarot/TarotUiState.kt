package com.fortunepocket.feature.tarot

import com.fortunepocket.core.model.TarotQuestionTheme
import com.fortunepocket.core.model.TarotReading
import com.fortunepocket.core.model.TarotSpreadStyle

data class TarotUiState(
    val selectedTheme: TarotQuestionTheme = TarotQuestionTheme.GENERAL,
    val selectedSpreadStyle: TarotSpreadStyle = TarotSpreadStyle.PATH_SPREAD,
    val isGenerating: Boolean = false,
    val reading: TarotReading? = null,
    val errorMessage: String? = null
)
