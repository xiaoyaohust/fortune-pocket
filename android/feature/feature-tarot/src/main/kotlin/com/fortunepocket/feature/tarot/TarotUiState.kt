package com.fortunepocket.feature.tarot

import com.fortunepocket.core.model.TarotQuestionTheme
import com.fortunepocket.core.model.TarotReading

data class TarotUiState(
    val selectedTheme: TarotQuestionTheme = TarotQuestionTheme.GENERAL,
    val isGenerating: Boolean = false,
    val reading: TarotReading? = null,
    val errorMessage: String? = null
)
