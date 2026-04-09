package com.fortunepocket.feature.bazi

import com.fortunepocket.core.content.bazi.BaziChart
import com.fortunepocket.core.content.bazi.BirthCity
import com.fortunepocket.core.content.bazi.Gender
import java.util.Calendar

data class BaziUiState(
    val selectedBirthDateMillis: Long = defaultBaziBirthDateMillis(),
    val selectedBirthHour:    Int     = 12,
    val selectedBirthMinute:  Int     = 0,
    val includesBirthHour:    Boolean = false,
    val selectedCity:         BirthCity? = null,
    val selectedGender:       Gender  = Gender.MALE,
    val useTrueSolarTime:     Boolean = false,
    val distinguishLateZi:    Boolean = false,
    val cities:               List<BirthCity> = emptyList(),
    val chart:                BaziChart? = null,
    val isLoading:            Boolean = false,
    val hasSavedCurrentChart: Boolean = false,
    val errorMessage:         String? = null
)

private fun defaultBaziBirthDateMillis(): Long {
    return Calendar.getInstance().apply {
        set(1996, Calendar.JUNE, 18, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
