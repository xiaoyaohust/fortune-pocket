package com.fortunepocket.feature.tarot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortunepocket.core.content.TarotReadingGenerator
import com.fortunepocket.core.data.repository.ReadingRepository
import com.fortunepocket.core.model.ReadingRecord
import com.fortunepocket.core.model.ReadingType
import com.fortunepocket.core.model.TarotQuestionTheme
import com.fortunepocket.core.model.TarotReading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TarotViewModel @Inject constructor(
    private val generator:  TarotReadingGenerator,
    private val repository: ReadingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TarotUiState())
    val uiState: StateFlow<TarotUiState> = _uiState.asStateFlow()

    // MARK: - Actions

    fun onThemeSelected(theme: TarotQuestionTheme) {
        _uiState.update {
            it.copy(selectedTheme = theme, reading = null, errorMessage = null, isGenerating = false)
        }
    }

    fun draw() {
        val state = _uiState.value
        if (state.isGenerating) return
        _uiState.update { it.copy(isGenerating = true, errorMessage = null, reading = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reading = generator.generate(state.selectedTheme)
                _uiState.update { it.copy(isGenerating = false, reading = reading, errorMessage = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, errorMessage = e.message ?: "Unknown error") }
            }
        }
    }

    fun saveAndReset() {
        val state = _uiState.value
        val reading = state.reading ?: return

        viewModelScope.launch(Dispatchers.IO) {
            saveReading(reading)
            _uiState.update { it.copy(reading = null, errorMessage = null, isGenerating = false) }
        }
    }

    fun reset() {
        _uiState.update { it.copy(reading = null, errorMessage = null, isGenerating = false) }
    }

    // MARK: - Persistence

    private suspend fun saveReading(reading: TarotReading) {
        val isZh   = Locale.getDefault().language == "zh"
        val detailJson = generator.toDetailJson(reading)

        val cardNames = reading.drawnCards
            .map { if (isZh) it.card.nameZh else it.card.nameEn }
            .joinToString(if (isZh) "、" else ", ")

        repository.save(
            ReadingRecord(
                id            = UUID.randomUUID().toString(),
                type          = ReadingType.TAROT,
                createdAt     = System.currentTimeMillis(),
                title         = if (isZh) "塔罗 · ${reading.theme.localizedName(true)}" else "Tarot · ${reading.theme.localizedName(false)}",
                summary       = cardNames,
                detailJson    = detailJson,
                schemaVersion = 1,
                isPremium     = false
            )
        )
    }
}
