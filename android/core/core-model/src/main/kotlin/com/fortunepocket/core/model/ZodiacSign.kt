package com.fortunepocket.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZodiacSign(
    val id: String,
    val index: Int,
    @SerialName("name_zh") val nameZh: String,
    @SerialName("name_en") val nameEn: String,
    val symbol: String,
    val element: String,           // fire | earth | air | water
    val quality: String,           // cardinal | fixed | mutable
    @SerialName("ruling_planet_zh") val rulingPlanetZh: String,
    @SerialName("ruling_planet_en") val rulingPlanetEn: String,
    @SerialName("date_range") val dateRange: DateRange,
    @SerialName("color_primary") val colorPrimary: String,
    @SerialName("color_accent") val colorAccent: String,
    @SerialName("traits_zh") val traitsZh: List<String>,
    @SerialName("traits_en") val traitsEn: List<String>,
    @SerialName("strengths_zh") val strengthsZh: List<String>,
    @SerialName("strengths_en") val strengthsEn: List<String>,
    @SerialName("weaknesses_zh") val weaknessesZh: List<String>,
    @SerialName("weaknesses_en") val weaknessesEn: List<String>,
    @SerialName("description_zh") val descriptionZh: String,
    @SerialName("description_en") val descriptionEn: String,
    @SerialName("lucky_color_zh") val luckyColorZh: String,
    @SerialName("lucky_color_en") val luckyColorEn: String,
    @SerialName("lucky_number") val luckyNumber: Int
) {
    fun localizedName(isZh: Boolean): String = if (isZh) nameZh else nameEn
    fun localizedDescription(isZh: Boolean): String = if (isZh) descriptionZh else descriptionEn
    fun localizedTraits(isZh: Boolean): List<String> = if (isZh) traitsZh else traitsEn
}

@Serializable
data class DateRange(
    @SerialName("start_month") val startMonth: Int,
    @SerialName("start_day") val startDay: Int,
    @SerialName("end_month") val endMonth: Int,
    @SerialName("end_day") val endDay: Int
)

@Serializable
data class ZodiacSignsData(
    val version: String,
    val signs: List<ZodiacSign>
)
