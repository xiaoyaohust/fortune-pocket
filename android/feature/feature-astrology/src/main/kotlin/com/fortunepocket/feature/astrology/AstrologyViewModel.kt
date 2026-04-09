package com.fortunepocket.feature.astrology

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortunepocket.core.content.ContentLoader
import com.fortunepocket.core.content.HoroscopeReadingGenerator
import com.fortunepocket.core.content.astrology.AstrologyInput
import com.fortunepocket.core.content.bazi.BaziDataLoader
import com.fortunepocket.core.content.bazi.BirthCity
import com.fortunepocket.core.data.datastore.UserPreferencesDataStore
import com.fortunepocket.core.data.repository.ReadingRepository
import com.fortunepocket.core.model.HoroscopeReadingDetail
import com.fortunepocket.core.model.ReadingRecord
import com.fortunepocket.core.model.ReadingType
import com.fortunepocket.core.model.ZodiacSign
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AstrologyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentLoader: ContentLoader,
    private val generator: HoroscopeReadingGenerator,
    private val repository: ReadingRepository,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(AstrologyUiState())
    val uiState: StateFlow<AstrologyUiState> = _uiState.asStateFlow()

    init {
        loadReferenceData()
        restoreSavedBirthday()
    }

    fun onBirthDateSelected(millis: Long) {
        _uiState.update {
            it.copy(
                selectedBirthDateMillis = normalizeDateMillis(millis),
                reading = null,
                hasSavedCurrentReading = false,
                errorMessage = null
            )
        }
    }

    fun onBirthTimeSelected(hour: Int, minute: Int) {
        _uiState.update {
            it.copy(
                selectedBirthHour = hour,
                selectedBirthMinute = minute,
                reading = null,
                hasSavedCurrentReading = false,
                errorMessage = null
            )
        }
    }

    fun onCitySelected(city: BirthCity?) {
        _uiState.update {
            it.copy(
                selectedCity = city,
                reading = null,
                hasSavedCurrentReading = false,
                errorMessage = null
            )
        }
    }

    fun generate() {
        val state = _uiState.value
        val city = state.selectedCity
        if (city == null) {
            _uiState.update {
                it.copy(
                    errorMessage = if (isZh()) {
                        "请先选择出生城市，才能计算上升点、宫位与相位。"
                    } else {
                        "Choose a birth city first so we can calculate the rising sign, houses, and aspects."
                    }
                )
            }
            return
        }

        _uiState.update { it.copy(isGenerating = true, errorMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val calendar = Calendar.getInstance().apply { timeInMillis = state.selectedBirthDateMillis }
            runCatching {
                generator.generate(
                    AstrologyInput(
                        birthYear = calendar.get(Calendar.YEAR),
                        birthMonth = calendar.get(Calendar.MONTH) + 1,
                        birthDay = calendar.get(Calendar.DAY_OF_MONTH),
                        birthHour = state.selectedBirthHour,
                        birthMinute = state.selectedBirthMinute,
                        city = city
                    )
                )
            }
                .onSuccess { reading ->
                    _uiState.update {
                        it.copy(
                            reading = reading,
                            isGenerating = false,
                            hasSavedCurrentReading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            errorMessage = error.message ?: "Unknown error"
                        )
                    }
                }
        }
    }

    fun saveReading() {
        val state = _uiState.value
        val reading = state.reading ?: return
        if (state.hasSavedCurrentReading) return

        viewModelScope.launch(Dispatchers.IO) {
            val detail = HoroscopeReadingDetail(
                signId = reading.sign.id,
                signNameZh = reading.sign.nameZh,
                signNameEn = reading.sign.nameEn,
                date = reading.birthDateText,
                birthTime = reading.birthTimeText,
                birthCityName = reading.birthCityName,
                timeZoneId = reading.timeZoneId,
                moonSignId = reading.moonSign.id,
                moonSignNameZh = reading.moonSign.nameZh,
                moonSignNameEn = reading.moonSign.nameEn,
                risingSignId = reading.risingSign?.id,
                risingSignNameZh = reading.risingSign?.nameZh,
                risingSignNameEn = reading.risingSign?.nameEn,
                chartSignature = reading.chartSignature,
                chartSummary = reading.chartSummary,
                overall = reading.overall,
                love = reading.love,
                career = reading.career,
                wealth = reading.wealth,
                social = reading.social,
                advice = reading.advice,
                luckyColor = reading.luckyColor,
                luckyNumber = reading.luckyNumber,
                planetPlacements = reading.planetPlacements,
                majorAspects = reading.majorAspects,
                elementBalance = reading.elementBalance,
                houseFocus = reading.houseFocus
            )

            repository.save(
                ReadingRecord(
                    id = UUID.randomUUID().toString(),
                    type = ReadingType.ASTROLOGY,
                    createdAt = System.currentTimeMillis(),
                    title = if (isZh()) {
                        "星盘 · ${reading.sign.nameZh}"
                    } else {
                        "Natal Chart · ${reading.sign.nameEn}"
                    },
                    summary = firstSentence(reading.chartSummary),
                    detailJson = json.encodeToString(detail),
                    schemaVersion = 1,
                    isPremium = false
                )
            )

            _uiState.update { it.copy(hasSavedCurrentReading = true) }
        }
    }

    private fun loadReferenceData() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                Pair(contentLoader.loadZodiacSigns().signs, BaziDataLoader.loadCities(context))
            }
                .onSuccess { (signs, cities) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            signs = signs,
                            cities = cities,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unknown error"
                        )
                    }
                }
        }
    }

    private fun restoreSavedBirthday() {
        viewModelScope.launch(Dispatchers.IO) {
            val savedBirthday = runCatching {
                userPreferencesDataStore.getUserBirthdayValue()
            }.getOrDefault(-1L)

            if (savedBirthday > 0) {
                _uiState.update {
                    it.copy(selectedBirthDateMillis = normalizeDateMillis(savedBirthday))
                }
            }
        }
    }

    fun inferSunSign(state: AstrologyUiState = uiState.value): ZodiacSign? {
        val calendar = Calendar.getInstance().apply { timeInMillis = state.selectedBirthDateMillis }
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return state.signs.firstOrNull { sign ->
            val range = sign.dateRange
            when {
                range.startMonth == range.endMonth -> month == range.startMonth && day in range.startDay..range.endDay
                month == range.startMonth -> day >= range.startDay
                month == range.endMonth -> day <= range.endDay
                else -> false
            }
        }
    }

    private fun normalizeDateMillis(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun firstSentence(text: String): String {
        listOf("。", ". ", "！", "？").forEach { separator ->
            val index = text.indexOf(separator)
            if (index >= 0) {
                return text.substring(0, index + separator.length)
            }
        }
        return text
    }

    private fun isZh(): Boolean = Locale.getDefault().language == "zh"
}
