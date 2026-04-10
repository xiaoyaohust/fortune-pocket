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

    fun defaultSpreadStyle(): TarotSpreadStyle = when (this) {
        GENERAL -> TarotSpreadStyle.PATH_SPREAD
        LOVE -> TarotSpreadStyle.RELATIONSHIP_SPREAD
        CAREER -> TarotSpreadStyle.CAREER_SPREAD
        WEALTH -> TarotSpreadStyle.WEALTH_SPREAD
    }

    fun availableSpreadStyles(): List<TarotSpreadStyle> = TarotSpreadStyle.availableFor(this)

    fun localizedName(isZh: Boolean): String = when (this) {
        GENERAL -> if (isZh) "综合主题" else "General Focus"
        LOVE -> if (isZh) "关系主题" else "Relationship Focus"
        CAREER -> if (isZh) "事业主题" else "Career Focus"
        WEALTH -> if (isZh) "财富主题" else "Wealth Focus"
    }

    fun localizedEntryTitle(isZh: Boolean): String = when (this) {
        GENERAL -> if (isZh) "今晚适合先看整体脉络" else "Tonight is best for reading the whole path"
        LOVE -> if (isZh) "今晚适合看关系里的真实流动" else "Tonight is best for reading relationship currents"
        CAREER -> if (isZh) "今晚适合看事业节奏与机会" else "Tonight is best for reading career momentum"
        WEALTH -> if (isZh) "今晚适合看资源与金钱节奏" else "Tonight is best for reading money and resources"
    }

    fun localizedEntrySubtitle(isZh: Boolean): String = when (this) {
        GENERAL -> if (isZh) "先抽一张得到方向，或用三张牌看清整段过程。" else "Start with a single card for direction, or use three cards to see the whole arc."
        LOVE -> if (isZh) "适合关系、暧昧、靠近与疏离，也适合照见自己在关系中的位置。" else "For romance, attachment, distance, and the role you are playing in a connection."
        CAREER -> if (isZh) "适合工作推进、学业压力、卡点和机会窗口。" else "For work momentum, study pressure, friction, and the opening ahead."
        WEALTH -> if (isZh) "适合看资源流动、消费冲动和下一步更稳的选择。" else "For resource flow, spending impulses, and the steadier next move."
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
        LOVE -> if (isZh) "关系聚焦" else "Relationship Focus"
        CAREER -> if (isZh) "事业聚焦" else "Career Focus"
        WEALTH -> if (isZh) "财富聚焦" else "Wealth Focus"
    }
}

@Serializable
enum class TarotSpreadStyle {
    @SerialName("single_card") SINGLE_CARD,
    @SerialName("three_card") PATH_SPREAD,
    @SerialName("love_three_card") RELATIONSHIP_SPREAD,
    @SerialName("career_three_card") CAREER_SPREAD,
    @SerialName("wealth_three_card") WEALTH_SPREAD,
    @SerialName("mind_body_spirit") MIND_BODY_SPIRIT;

    fun spreadId(): String = when (this) {
        SINGLE_CARD -> "single_card"
        PATH_SPREAD -> "three_card"
        RELATIONSHIP_SPREAD -> "love_three_card"
        CAREER_SPREAD -> "career_three_card"
        WEALTH_SPREAD -> "wealth_three_card"
        MIND_BODY_SPIRIT -> "mind_body_spirit"
    }

    fun cardCount(): Int = when (this) {
        SINGLE_CARD -> 1
        else -> 3
    }

    fun localizedName(isZh: Boolean): String = when (this) {
        SINGLE_CARD -> if (isZh) "单张指引" else "Single Card"
        PATH_SPREAD -> if (isZh) "路径三牌阵" else "Path Spread"
        RELATIONSHIP_SPREAD -> if (isZh) "关系镜像阵" else "Relationship Mirror"
        CAREER_SPREAD -> if (isZh) "事业路径阵" else "Career Path"
        WEALTH_SPREAD -> if (isZh) "财富流动阵" else "Wealth Flow"
        MIND_BODY_SPIRIT -> if (isZh) "身心灵三牌阵" else "Mind Body Spirit"
    }

    fun localizedSubtitle(isZh: Boolean): String = when (this) {
        SINGLE_CARD -> if (isZh) "一张牌，给今晚一个清晰方向" else "One card for tonight's clearest direction"
        PATH_SPREAD -> if (isZh) "过去 · 现在 · 下一步" else "Past · Present · Next Step"
        RELATIONSHIP_SPREAD -> if (isZh) "我在关系里 · 关系流动 · 温柔建议" else "My Place · Connection · Gentle Advice"
        CAREER_SPREAD -> if (isZh) "当前路径 · 隐藏阻力 · 机会入口" else "Current Path · Hidden Block · Opportunity"
        WEALTH_SPREAD -> if (isZh) "当前流动 · 资源模式 · 下一步动作" else "Current Flow · Resource Pattern · Next Move"
        MIND_BODY_SPIRIT -> if (isZh) "身体 · 心绪 · 灵魂讯号" else "Body · Mind · Spirit"
    }

    fun localizedDescription(isZh: Boolean): String = when (this) {
        SINGLE_CARD -> if (isZh)
            "适合当下只想要一个方向感、不想被太多信息淹没的时候。"
        else
            "Best when you want one clear direction without being overwhelmed."
        PATH_SPREAD -> if (isZh)
            "适合梳理你一路走来的情绪脉络、现状位置和下一步动作。"
        else
            "Best for understanding the arc from what shaped you to what comes next."
        RELATIONSHIP_SPREAD -> if (isZh)
            "适合关系主题，帮助你同时看见自己、关系流动和最温柔的回应方式。"
        else
            "Best for relationship questions, showing you, the connection, and the gentlest response."
        CAREER_SPREAD -> if (isZh)
            "适合工作与学业，帮你看清正在发生的节奏、阻力和机会入口。"
        else
            "Best for work and study, revealing momentum, friction, and the opening ahead."
        WEALTH_SPREAD -> if (isZh)
            "适合资源与金钱问题，看见流向、惯性和更稳的下一步。"
        else
            "Best for money and resources, showing flow, habits, and a steadier next move."
        MIND_BODY_SPIRIT -> if (isZh)
            "适合想听见更深层状态时，看看身体、心绪与灵魂当前分别在说什么。"
        else
            "Best when you want a deeper scan of what body, mind, and spirit are each saying."
    }

    companion object {
        fun availableFor(theme: TarotQuestionTheme): List<TarotSpreadStyle> = when (theme) {
            TarotQuestionTheme.GENERAL -> listOf(SINGLE_CARD, PATH_SPREAD, MIND_BODY_SPIRIT)
            TarotQuestionTheme.LOVE -> listOf(SINGLE_CARD, RELATIONSHIP_SPREAD)
            TarotQuestionTheme.CAREER -> listOf(SINGLE_CARD, CAREER_SPREAD)
            TarotQuestionTheme.WEALTH -> listOf(SINGLE_CARD, WEALTH_SPREAD)
        }
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
