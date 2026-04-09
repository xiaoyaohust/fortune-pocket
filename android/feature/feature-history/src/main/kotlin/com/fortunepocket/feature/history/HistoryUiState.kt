package com.fortunepocket.feature.history

import com.fortunepocket.core.model.ReadingRecord

data class HistoryUiState(
    val records: List<ReadingRecord> = emptyList(),
    val selectedRecord: ReadingRecord? = null,
    val isLoading: Boolean = true
)
