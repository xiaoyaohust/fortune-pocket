package com.fortunepocket.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HeavenlyStem(
    val id: String,
    val index: Int,
    @SerialName("name_zh") val nameZh: String,
    @SerialName("name_en") val nameEn: String,
    val element: String,
    val polarity: String,
    @SerialName("image_zh") val imageZh: String,
    @SerialName("image_en") val imageEn: String,
    @SerialName("color_primary") val colorPrimary: String,
    @SerialName("color_accent") val colorAccent: String,
    @SerialName("personality_zh") val personalityZh: String,
    @SerialName("personality_en") val personalityEn: String,
    @SerialName("love_zh") val loveZh: String,
    @SerialName("love_en") val loveEn: String,
    @SerialName("career_zh") val careerZh: String,
    @SerialName("career_en") val careerEn: String,
    @SerialName("wealth_zh") val wealthZh: String,
    @SerialName("wealth_en") val wealthEn: String
)

@Serializable
data class EarthlyBranch(
    val id: String,
    val index: Int,
    @SerialName("name_zh") val nameZh: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("animal_zh") val animalZh: String,
    @SerialName("animal_en") val animalEn: String,
    val element: String,
    @SerialName("color_primary") val colorPrimary: String,
    @SerialName("color_accent") val colorAccent: String,
    val month: Int,
    @SerialName("nuance_zh") val nuanceZh: String,
    @SerialName("nuance_en") val nuanceEn: String
)

@Serializable
data class StemsData(
    val version: String,
    val stems: List<HeavenlyStem>
)

@Serializable
data class BranchesData(
    val version: String,
    val branches: List<EarthlyBranch>
)

@Serializable
data class BaziPersonalityTemplates(
    val version: String,
    val disclaimer_zh: String,
    val disclaimer_en: String,
    @SerialName("phase_advice_zh") val phaseAdviceZh: List<String>,
    @SerialName("phase_advice_en") val phaseAdviceEn: List<String>,
    @SerialName("overall_intro_zh") val overallIntroZh: List<String>,
    @SerialName("overall_intro_en") val overallIntroEn: List<String>,
    @SerialName("element_personality") val elementPersonality: ElementPersonality
)

@Serializable
data class ElementPersonality(
    val wood_zh: String, val wood_en: String,
    val fire_zh: String, val fire_en: String,
    val earth_zh: String, val earth_en: String,
    val metal_zh: String, val metal_en: String,
    val water_zh: String, val water_en: String
)

@Serializable
data class DailyQuote(
    val id: String,
    @SerialName("text_zh") val textZh: String,
    @SerialName("text_en") val textEn: String,
    val category: String
)

@Serializable
data class DailyQuotesData(
    val version: String,
    val quotes: List<DailyQuote>
)

// In-memory result of a Bazi reading
data class BaziReading(
    val birthDate:          Long,    // epoch millis
    val birthHour:          Int?,    // 0–23, null if not specified
    val stemId:             String,
    val stemNameZh:         String,
    val stemNameEn:         String,
    val stemColorPrimary:   String,
    val disclaimer:         String,
    val overallPersonality: String,
    val love:               String,
    val career:             String,
    val wealth:             String,
    val phaseAdvice:        String,
    val elementDescription: String
)
