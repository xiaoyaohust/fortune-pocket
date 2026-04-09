package com.fortunepocket.core.model

import kotlinx.serialization.Serializable

// Logical model (domain layer) — platform-agnostic
@Serializable
data class ReadingRecord(
    val id: String,
    val type: ReadingType,
    val createdAt: Long,          // epoch millis
    val title: String,
    val summary: String,
    val detailJson: String,       // full result serialized as JSON string
    val schemaVersion: Int = 1,
    val isPremium: Boolean = false // reserved for future
)

enum class ReadingType(val key: String) {
    TAROT("tarot"),
    ASTROLOGY("astrology"),
    BAZI("bazi");

    companion object {
        fun fromKey(key: String): ReadingType = entries.first { it.key == key }
    }
}

// Tarot reading detail — serialized into ReadingRecord.detailJson
@Serializable
data class TarotReadingDetail(
    val spreadId: String,
    val themeId: String? = null,
    val themeNameZh: String? = null,
    val themeNameEn: String? = null,
    val spreadDescription: String? = null,
    val drawnCards: List<DrawnCardDetail>,
    val positionReadings: List<TarotPositionReadingDetail> = emptyList(),
    val overallEnergy: String,
    val focusInsight: String? = null,
    val loveReading: String? = null,
    val careerReading: String? = null,
    val wealthReading: String? = null,
    val adviceTip: String,
    val luckyItem: String,
    val luckyColor: String? = null,
    val luckyNumber: Int? = null
)

@Serializable
data class DrawnCardDetail(
    val cardId: String,
    val cardNameZh: String,
    val cardNameEn: String,
    val isUpright: Boolean,
    val positionLabel: String,
    val positionAspect: String? = null,
    val meaning: String
)

@Serializable
data class TarotPositionReadingDetail(
    val title: String,
    val aspect: String,
    val body: String
)

// Horoscope reading detail
@Serializable
data class HoroscopeReadingDetail(
    val signId: String,
    val signNameZh: String,
    val signNameEn: String,
    val date: String,
    val birthTime: String? = null,
    val birthCityName: String? = null,
    val timeZoneId: String? = null,
    val moonSignId: String? = null,
    val moonSignNameZh: String? = null,
    val moonSignNameEn: String? = null,
    val risingSignId: String? = null,
    val risingSignNameZh: String? = null,
    val risingSignNameEn: String? = null,
    val chartSignature: String? = null,
    val chartSummary: String? = null,
    val overall: String,
    val love: String,
    val career: String,
    val wealth: String,
    val social: String,
    val advice: String,
    val luckyColor: String,
    val luckyNumber: Int,
    val planetPlacements: List<AstrologyPlanetPlacement> = emptyList(),
    val majorAspects: List<AstrologyAspect> = emptyList(),
    val elementBalance: AstrologyElementBalance? = null,
    val houseFocus: List<AstrologyHouseFocus> = emptyList()
)

// Bazi reading detail (legacy entertainment-text format)
@Serializable
data class BaziReadingDetail(
    val birthDate: String,
    val birthHour: String?,
    val dayStemId: String,
    val disclaimer: String,
    val overallPersonality: String,
    val loveTendency: String,
    val careerTendency: String,
    val wealthTendency: String,
    val phaseAdvice: String,
    val elementDescription: String
)

// Bazi chart snapshot (new engine format)
@Serializable
data class BaziChartSnapshot(
    val year: String,
    val month: String,
    val day: String,
    val hour: String? = null,
    val dayMasterElement: String,
    val cycleDirection: String,
    val startingAge: Int
)
