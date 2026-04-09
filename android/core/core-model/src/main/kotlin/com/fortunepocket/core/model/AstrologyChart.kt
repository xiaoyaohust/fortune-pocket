package com.fortunepocket.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AstrologyPlanetID {
    SUN,
    MOON,
    MERCURY,
    VENUS,
    MARS,
    JUPITER,
    SATURN,
    URANUS,
    NEPTUNE,
    PLUTO;

    fun localizedName(isZh: Boolean): String = when (this) {
        SUN -> if (isZh) "太阳" else "Sun"
        MOON -> if (isZh) "月亮" else "Moon"
        MERCURY -> if (isZh) "水星" else "Mercury"
        VENUS -> if (isZh) "金星" else "Venus"
        MARS -> if (isZh) "火星" else "Mars"
        JUPITER -> if (isZh) "木星" else "Jupiter"
        SATURN -> if (isZh) "土星" else "Saturn"
        URANUS -> if (isZh) "天王星" else "Uranus"
        NEPTUNE -> if (isZh) "海王星" else "Neptune"
        PLUTO -> if (isZh) "冥王星" else "Pluto"
    }
}

@Serializable
enum class AstrologyAspectType(val angle: Double, val orb: Double) {
    CONJUNCTION(0.0, 8.0),
    SEXTILE(60.0, 4.5),
    SQUARE(90.0, 6.0),
    TRINE(120.0, 6.0),
    OPPOSITION(180.0, 8.0);

    fun localizedName(isZh: Boolean): String = when (this) {
        CONJUNCTION -> if (isZh) "合相" else "Conjunction"
        SEXTILE -> if (isZh) "六合" else "Sextile"
        SQUARE -> if (isZh) "刑克" else "Square"
        TRINE -> if (isZh) "拱相" else "Trine"
        OPPOSITION -> if (isZh) "冲相" else "Opposition"
    }
}

@Serializable
data class AstrologyPlanetPlacement(
    val planetId: AstrologyPlanetID,
    val longitude: Double,
    val signId: String,
    val signNameZh: String,
    val signNameEn: String,
    val house: Int? = null,
    val isRetrograde: Boolean = false
) {
    fun localizedSignName(isZh: Boolean): String = if (isZh) signNameZh else signNameEn
    val degreeInSign: Double get() = ((longitude % 30.0) + 30.0) % 30.0
}

@Serializable
data class AstrologyAspect(
    val firstPlanetId: AstrologyPlanetID,
    val secondPlanetId: AstrologyPlanetID,
    val type: AstrologyAspectType,
    val orbDegrees: Double
)

@Serializable
data class AstrologyElementBalance(
    val fire: Int,
    val earth: Int,
    val air: Int,
    val water: Int
) {
    val dominantElementKey: String
        get() = listOf(
            "fire" to fire,
            "earth" to earth,
            "air" to air,
            "water" to water
        ).maxWithOrNull(compareBy<Pair<String, Int>> { it.second }.thenByDescending { it.first })?.first ?: "fire"

    fun localizedDominantElement(isZh: Boolean): String = when (dominantElementKey) {
        "fire" -> if (isZh) "火象" else "Fire"
        "earth" -> if (isZh) "土象" else "Earth"
        "air" -> if (isZh) "风象" else "Air"
        "water" -> if (isZh) "水象" else "Water"
        else -> if (isZh) "火象" else "Fire"
    }
}

@Serializable
data class AstrologyHouseFocus(
    val house: Int,
    val titleZh: String,
    val titleEn: String,
    val summaryZh: String,
    val summaryEn: String
) {
    fun localizedTitle(isZh: Boolean): String = if (isZh) titleZh else titleEn
    fun localizedSummary(isZh: Boolean): String = if (isZh) summaryZh else summaryEn
}

data class HoroscopeReading(
    val sign: ZodiacSign,
    val moonSign: ZodiacSign,
    val risingSign: ZodiacSign?,
    val birthDateText: String,
    val birthTimeText: String,
    val birthCityName: String,
    val timeZoneId: String,
    val chartSummary: String,
    val chartSignature: String,
    val overall: String,
    val love: String,
    val career: String,
    val wealth: String,
    val social: String,
    val advice: String,
    val luckyColor: String,
    val luckyNumber: Int,
    val planetPlacements: List<AstrologyPlanetPlacement>,
    val majorAspects: List<AstrologyAspect>,
    val elementBalance: AstrologyElementBalance,
    val houseFocus: List<AstrologyHouseFocus>
)
