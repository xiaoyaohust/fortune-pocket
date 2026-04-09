package com.fortunepocket.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TarotCard(
    val id: String,
    val number: Int,
    val arcana: String,
    val suit: String? = null,
    val rank: String? = null,
    @SerialName("name_zh") val nameZh: String,
    @SerialName("name_en") val nameEn: String,
    val symbol: String,
    @SerialName("color_primary") val colorPrimary: String,
    @SerialName("color_accent") val colorAccent: String,
    @SerialName("keywords_upright_zh") val keywordsUprightZh: List<String>,
    @SerialName("keywords_upright_en") val keywordsUprightEn: List<String>,
    @SerialName("keywords_reversed_zh") val keywordsReversedZh: List<String>,
    @SerialName("keywords_reversed_en") val keywordsReversedEn: List<String>,
    @SerialName("meaning_upright_zh") val meaningUprightZh: String,
    @SerialName("meaning_upright_en") val meaningUprightEn: String,
    @SerialName("meaning_reversed_zh") val meaningReversedZh: String,
    @SerialName("meaning_reversed_en") val meaningReversedEn: String,
    @SerialName("energy_upright") val energyUpright: String,
    @SerialName("energy_reversed") val energyReversed: String
) {
    fun localizedName(isZh: Boolean): String = if (isZh) nameZh else nameEn
    fun localizedMeaning(isUpright: Boolean, isZh: Boolean): String = when {
        isUpright && isZh -> meaningUprightZh
        isUpright && !isZh -> meaningUprightEn
        !isUpright && isZh -> meaningReversedZh
        else -> meaningReversedEn
    }
    fun localizedKeywords(isUpright: Boolean, isZh: Boolean): List<String> = when {
        isUpright && isZh -> keywordsUprightZh
        isUpright && !isZh -> keywordsUprightEn
        !isUpright && isZh -> keywordsReversedZh
        else -> keywordsReversedEn
    }

    val semanticSuit: String
        get() = suit ?: arcana
}

@Serializable
data class TarotCardsData(
    val version: String,
    val total: Int,
    val cards: List<TarotCard>
)

// A drawn card with orientation
data class DrawnCard(
    val card: TarotCard,
    val isUpright: Boolean,
    val positionLabel: String = "",
    val positionAspect: String = ""
)

@Serializable
enum class TarotQuestionTheme {
    @SerialName("general") GENERAL,
    @SerialName("love") LOVE,
    @SerialName("career") CAREER,
    @SerialName("wealth") WEALTH;

    fun spreadId(): String = when (this) {
        GENERAL -> "three_card"
        LOVE -> "love_three_card"
        CAREER -> "career_three_card"
        WEALTH -> "wealth_three_card"
    }

    fun localizedName(isZh: Boolean): String = when (this) {
        GENERAL -> if (isZh) "综合主题" else "General Focus"
        LOVE -> if (isZh) "爱情主题" else "Love Focus"
        CAREER -> if (isZh) "事业主题" else "Career Focus"
        WEALTH -> if (isZh) "财富主题" else "Wealth Focus"
    }

    fun localizedSpreadName(isZh: Boolean): String = when (this) {
        GENERAL -> if (isZh) "路径三牌阵" else "Path Spread"
        LOVE -> if (isZh) "爱情镜像阵" else "Love Mirror Spread"
        CAREER -> if (isZh) "事业路径阵" else "Career Path Spread"
        WEALTH -> if (isZh) "财富流动阵" else "Wealth Flow Spread"
    }

    fun localizedSpreadSubtitle(isZh: Boolean): String = when (this) {
        GENERAL -> if (isZh) "过去 · 现在 · 下一步" else "Past · Present · Next Step"
        LOVE -> if (isZh) "我在关系里 · 关系流动 · 温柔建议" else "My Place in Love · Connection Energy · Gentle Advice"
        CAREER -> if (isZh) "当前路径 · 隐藏阻力 · 机会入口" else "Current Path · Hidden Block · Opportunity"
        WEALTH -> if (isZh) "当前流动 · 资源模式 · 下一步动作" else "Current Flow · Resource Pattern · Next Move"
    }

    fun localizedPromptDescription(isZh: Boolean): String = when (this) {
        GENERAL -> if (isZh)
            "适合想看看整体处境、情绪走向和下一步方向的时候。"
        else
            "Best when you want a gentle read on your current path and next step."
        LOVE -> if (isZh)
            "围绕关系状态、情感互动和你此刻最需要的相处方式。"
        else
            "Focus on relationship dynamics, emotional flow, and the gentlest way to respond."
        CAREER -> if (isZh)
            "围绕学业、工作推进、卡点与机会入口来展开解读。"
        else
            "Focus on study or work momentum, hidden friction, and the clearest opening ahead."
        WEALTH -> if (isZh)
            "围绕金钱流动、资源使用方式与更稳妥的下一步。"
        else
            "Focus on money flow, resource patterns, and the steadier next move."
    }

    fun localizedFocusTitle(isZh: Boolean): String = when (this) {
        GENERAL -> if (isZh) "牌阵总览" else "Spread Synthesis"
        LOVE -> if (isZh) "爱情聚焦" else "Love Focus"
        CAREER -> if (isZh) "事业聚焦" else "Career Focus"
        WEALTH -> if (isZh) "财富聚焦" else "Wealth Focus"
    }
}

@Serializable
data class TarotPositionReading(
    val positionLabel: String,
    val aspect: String,
    val cardNameZh: String,
    val cardNameEn: String,
    val isUpright: Boolean,
    val interpretation: String
)

// MARK: - Spread models

@Serializable
data class SpreadPosition(
    val index: Int,
    @SerialName("label_zh") val labelZh: String,
    @SerialName("label_en") val labelEn: String,
    val aspect: String
)

@Serializable
data class TarotSpread(
    val id: String,
    @SerialName("name_zh") val nameZh: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("description_zh") val descriptionZh: String,
    @SerialName("description_en") val descriptionEn: String,
    @SerialName("card_count") val cardCount: Int,
    val positions: List<SpreadPosition>
)

@Serializable
data class SpreadsData(
    val version: String,
    val spreads: List<TarotSpread>
)

// MARK: - In-memory reading result (not persisted directly)

data class TarotReading(
    val theme: TarotQuestionTheme,
    val spreadId: String,
    val drawnCards: List<DrawnCard>,
    val positionReadings: List<TarotPositionReading>,
    val overallEnergy: String,
    val focusInsight: String,
    val advice: String,
    val luckyItemName: String,
    val luckyColorName: String,
    val luckyColorHex: String,
    val luckyNumber: Int,
    val spreadName: String,
    val spreadDescription: String,
    val timestamp: Long   // epoch millis
)
