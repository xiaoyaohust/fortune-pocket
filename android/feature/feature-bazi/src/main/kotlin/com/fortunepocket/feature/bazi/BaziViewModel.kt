package com.fortunepocket.feature.bazi

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortunepocket.core.content.bazi.BaziCalculator
import com.fortunepocket.core.content.bazi.BaziDataLoader
import com.fortunepocket.core.content.bazi.BaziInput
import com.fortunepocket.core.content.bazi.BirthCity
import com.fortunepocket.core.content.bazi.ElementNamesZh
import com.fortunepocket.core.content.bazi.Gender
import com.fortunepocket.core.content.bazi.StemElements
import com.fortunepocket.core.data.repository.ReadingRepository
import com.fortunepocket.core.model.ReadingRecord
import com.fortunepocket.core.model.ReadingType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BaziViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ReadingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BaziUiState())
    val uiState: StateFlow<BaziUiState> = _uiState.asStateFlow()

    init {
        loadCities()
    }

    private fun loadCities() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { BaziDataLoader.loadCities(context) }
                .onSuccess { cities ->
                    _uiState.update { it.copy(cities = cities, selectedCity = null) }
                }
        }
    }

    fun onBirthDateSelected(millis: Long) {
        _uiState.update {
            it.copy(selectedBirthDateMillis = normalizeDateMillis(millis), chart = null,
                hasSavedCurrentChart = false, errorMessage = null)
        }
    }

    fun onBirthHourSelected(hour: Int) {
        _uiState.update { it.copy(selectedBirthHour = hour, chart = null, hasSavedCurrentChart = false) }
    }

    fun onBirthMinuteSelected(minute: Int) {
        _uiState.update { it.copy(selectedBirthMinute = minute, chart = null, hasSavedCurrentChart = false) }
    }

    fun onIncludesBirthHourChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(includesBirthHour = enabled, chart = null, hasSavedCurrentChart = false, errorMessage = null)
        }
    }

    fun onCitySelected(city: BirthCity?) {
        _uiState.update {
            it.copy(
                selectedCity = city,
                useTrueSolarTime = if (city == null) false else it.useTrueSolarTime,
                chart = null,
                hasSavedCurrentChart = false
            )
        }
    }

    fun onGenderChanged(gender: Gender) {
        _uiState.update { it.copy(selectedGender = gender, chart = null, hasSavedCurrentChart = false) }
    }

    fun onTrueSolarTimeChanged(enabled: Boolean) {
        _uiState.update { it.copy(useTrueSolarTime = enabled, chart = null, hasSavedCurrentChart = false) }
    }

    fun onDistinguishLateZiChanged(enabled: Boolean) {
        _uiState.update { it.copy(distinguishLateZi = enabled, chart = null, hasSavedCurrentChart = false) }
    }

    fun generate() {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val cal = Calendar.getInstance().apply { timeInMillis = state.selectedBirthDateMillis }
            val input = BaziInput(
                birthYear   = cal.get(Calendar.YEAR),
                birthMonth  = cal.get(Calendar.MONTH) + 1,
                birthDay    = cal.get(Calendar.DAY_OF_MONTH),
                birthHour   = if (state.includesBirthHour) state.selectedBirthHour else null,
                birthMinute = if (state.includesBirthHour) state.selectedBirthMinute else null,
                city        = state.selectedCity,
                gender      = state.selectedGender,
                useTrueSolarTime     = state.useTrueSolarTime && state.includesBirthHour,
                distinguishLateZiHour = state.distinguishLateZi && state.includesBirthHour
            )

            runCatching { BaziCalculator.calculate(context, input) }
                .onSuccess { chart ->
                    _uiState.update {
                        it.copy(chart = chart, isLoading = false,
                            hasSavedCurrentChart = false, errorMessage = null)
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = err.message ?: "Unknown error")
                    }
                }
        }
    }

    fun saveReading() {
        val state = _uiState.value
        val chart = state.chart ?: return
        if (state.hasSavedCurrentChart) return

        viewModelScope.launch(Dispatchers.IO) {
            val isZh = Locale.getDefault().language == "zh"
            val dayMaster = if (isZh) chart.dayPillar.stemZh else chart.dayPillar.stemEn
            val title = if (isZh) "八字 · ${dayMaster}日主" else "Bazi · $dayMaster Day Master"
            val summary = if (isZh)
                "四柱：${chart.yearPillar.nameZh} ${chart.monthPillar.nameZh} ${chart.dayPillar.nameZh}"
            else
                "Four Pillars: ${chart.yearPillar.nameEn} ${chart.monthPillar.nameEn} ${chart.dayPillar.nameEn}"

            val elementIdx = StemElements[chart.dayPillar.normalizedStem]
            val detailJson = """{"year":"${chart.yearPillar.nameZh}","month":"${chart.monthPillar.nameZh}","day":"${chart.dayPillar.nameZh}","hour":"${chart.hourPillar?.nameZh ?: ""}","dayMasterElement":"${ElementNamesZh[elementIdx]}","cycleDirection":"${chart.cycleDirection}","startingAge":${chart.startingAge}}"""

            repository.save(
                ReadingRecord(
                    id            = UUID.randomUUID().toString(),
                    type          = ReadingType.BAZI,
                    createdAt     = System.currentTimeMillis(),
                    title         = title,
                    summary       = summary,
                    detailJson    = detailJson,
                    schemaVersion = 1,
                    isPremium     = false
                )
            )
            _uiState.update { it.copy(hasSavedCurrentChart = true) }
        }
    }

    private fun normalizeDateMillis(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
