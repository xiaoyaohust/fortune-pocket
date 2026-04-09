package com.fortunepocket.feature.astrology

import com.fortunepocket.core.content.bazi.BirthCity
import com.fortunepocket.core.model.HoroscopeReading
import com.fortunepocket.core.model.ZodiacSign
import java.util.Calendar

data class AstrologyUiState(
    val isLoading: Boolean = true,
    val signs: List<ZodiacSign> = emptyList(),
    val cities: List<BirthCity> = emptyList(),
    val selectedBirthDateMillis: Long = defaultAstrologyBirthDateMillis(),
    val selectedBirthHour: Int = 12,
    val selectedBirthMinute: Int = 0,
    val selectedCity: BirthCity? = null,
    val reading: HoroscopeReading? = null,
    val isGenerating: Boolean = false,
    val hasSavedCurrentReading: Boolean = false,
    val errorMessage: String? = null
)

private fun defaultAstrologyBirthDateMillis(): Long {
    return Calendar.getInstance().apply {
        set(1998, Calendar.AUGUST, 8, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
