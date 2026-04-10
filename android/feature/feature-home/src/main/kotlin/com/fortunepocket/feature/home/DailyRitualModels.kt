package com.fortunepocket.feature.home

import com.fortunepocket.core.model.DailyQuote
import com.fortunepocket.core.model.TarotCard
import com.fortunepocket.core.model.ZodiacSign

data class RitualCopy(
    val zh: String,
    val en: String
) {
    fun resolve(isZh: Boolean): String = if (isZh) zh else en
}

enum class DailyRitualDestination {
    TAROT,
    ASTROLOGY,
    BAZI;

    fun title(isZh: Boolean): String = when (this) {
        TAROT -> if (isZh) "抽塔罗" else "Tarot Reading"
        ASTROLOGY -> if (isZh) "本命盘占星" else "Natal Chart"
        BAZI -> if (isZh) "八字分析" else "Bazi Analysis"
    }
}

data class DailyTarotHighlight(
    val title: RitualCopy,
    val card: TarotCard,
    val isUpright: Boolean,
    val insight: RitualCopy
)

data class DailyTransitHighlight(
    val title: RitualCopy,
    val sign: ZodiacSign?,
    val summary: RitualCopy,
    val needsBirthday: Boolean
)

data class DailyRitual(
    val generatedAtMillis: Long,
    val ritualTitle: RitualCopy,
    val ritualSummary: RitualCopy,
    val destination: DailyRitualDestination,
    val destinationReason: RitualCopy,
    val energyTitle: RitualCopy,
    val energyBody: RitualCopy,
    val tarot: DailyTarotHighlight,
    val transit: DailyTransitHighlight,
    val promptTitle: RitualCopy,
    val promptBody: RitualCopy,
    val quote: DailyQuote
)
