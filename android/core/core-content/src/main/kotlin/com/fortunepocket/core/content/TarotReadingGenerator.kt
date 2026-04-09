package com.fortunepocket.core.content

import com.fortunepocket.core.model.DrawnCard
import com.fortunepocket.core.model.DrawnCardDetail
import com.fortunepocket.core.model.TarotPositionReading
import com.fortunepocket.core.model.TarotPositionReadingDetail
import com.fortunepocket.core.model.TarotQuestionTheme
import com.fortunepocket.core.model.TarotReading
import com.fortunepocket.core.model.TarotReadingDetail
import com.fortunepocket.core.model.TarotSpread
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TarotReadingGenerator @Inject constructor(
    private val contentLoader: ContentLoader
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun generate(theme: TarotQuestionTheme = TarotQuestionTheme.GENERAL): TarotReading =
        buildTarotReading(
            cardsData = contentLoader.loadTarotCards(),
            spreadsData = contentLoader.loadSpreads(),
            readingTemplatesRaw = contentLoader.loadReadingTemplatesRaw(),
            luckyItemsRaw = contentLoader.loadLuckyItemsRaw(),
            luckyColorsRaw = contentLoader.loadLuckyColorsRaw(),
            theme = theme
        )

    fun toDetailJson(reading: TarotReading): String =
        json.encodeToString(toTarotDetail(reading))

    fun toDetail(reading: TarotReading): TarotReadingDetail = toTarotDetail(reading)
}

// ---------------------------------------------------------------------------
// Internal: pure computation — no ContentLoader/Context dependency.
// Called by TarotReadingGenerator and by unit tests directly.
// ---------------------------------------------------------------------------

internal fun toTarotDetail(reading: TarotReading): TarotReadingDetail {
    val isZh = Locale.getDefault().language == "zh"
    return TarotReadingDetail(
        spreadId = reading.spreadId,
        themeId = reading.theme.name.lowercase(Locale.US),
        themeNameZh = null,
        themeNameEn = null,
        spreadDescription = reading.spreadDescription,
        drawnCards = reading.drawnCards.map { drawn ->
            DrawnCardDetail(
                cardId = drawn.card.id,
                cardNameZh = drawn.card.nameZh,
                cardNameEn = drawn.card.nameEn,
                isUpright = drawn.isUpright,
                positionLabel = drawn.positionLabel,
                positionAspect = drawn.positionAspect,
                meaning = drawn.card.localizedMeaning(drawn.isUpright, isZh)
            )
        },
        positionReadings = reading.positionReadings.map { item ->
            TarotPositionReadingDetail(
                title = item.positionLabel,
                aspect = item.aspect,
                body = item.interpretation
            )
        },
        overallEnergy = reading.overallEnergy,
        focusInsight = reading.focusInsight,
        adviceTip = reading.advice,
        luckyItem = reading.luckyItemName,
        luckyColor = reading.luckyColorName,
        luckyNumber = reading.luckyNumber
    )
}

internal fun buildTarotReading(
    cardsData: com.fortunepocket.core.model.TarotCardsData,
    spreadsData: com.fortunepocket.core.model.SpreadsData,
    readingTemplatesRaw: String,
    luckyItemsRaw: String,
    luckyColorsRaw: String,
    theme: TarotQuestionTheme = TarotQuestionTheme.GENERAL
): TarotReading {
    val json = Json { ignoreUnknownKeys = true }
    val isZh = Locale.getDefault().language == "zh"

    val spread = spreadsData.spreads.firstOrNull { it.id == theme.spreadId() }
        ?: spreadsData.spreads.firstOrNull { it.id == "three_card" }
        ?: spreadsData.spreads.first()

    val positions = spread.positions.sortedBy { it.index }
    val shuffledCards = cardsData.cards.shuffled().take(spread.cardCount)
    val drawnCards = positions.mapIndexed { index, position ->
        DrawnCard(
            card = shuffledCards[index],
            isUpright = (0..1).random() == 1,
            positionLabel = if (isZh) position.labelZh else position.labelEn,
            positionAspect = position.aspect
        )
    }

    val energyKey = tarotAssessEnergy(drawnCards)
    val templates = json.decodeFromString<TarotReadingTemplates>(readingTemplatesRaw)
    val overallEnergy = tarotBuildOverall(drawnCards, spread, theme, energyKey, templates, isZh)
    val positionReadings = drawnCards.map { tarotBuildPositionReading(it, theme, isZh) }
    val focusInsight = tarotBuildFocusInsight(drawnCards, theme, energyKey, isZh)
    val advice = tarotBuildAdvice(drawnCards.lastOrNull() ?: drawnCards.first(), templates, isZh)

    val luckyItems = tarotParseLuckyItems(luckyItemsRaw, isZh, json)
    val luckyColors = tarotParseLuckyColors(luckyColorsRaw, isZh, json)
    val luckyItem = luckyItems.randomOrNull() ?: Pair(if (isZh) "水晶" else "Crystal", "")
    val luckyColor = luckyColors.randomOrNull() ?: Pair(if (isZh) "金色" else "Gold", "#C9A84C")
    val luckyNumber = (drawnCards.sumOf { it.card.number } % 9) + 1

    return TarotReading(
        theme = theme,
        spreadId = spread.id,
        drawnCards = drawnCards,
        positionReadings = positionReadings,
        overallEnergy = overallEnergy,
        focusInsight = focusInsight,
        advice = advice,
        luckyItemName = luckyItem.first,
        luckyColorName = luckyColor.first,
        luckyColorHex = luckyColor.second,
        luckyNumber = luckyNumber,
        spreadName = if (isZh) spread.nameZh else spread.nameEn,
        spreadDescription = if (isZh) spread.descriptionZh else spread.descriptionEn,
        timestamp = System.currentTimeMillis()
    )
}

internal fun tarotAssessEnergy(cards: List<DrawnCard>): String {
    val lights = cards.count {
        (it.isUpright && it.card.energyUpright == "light") ||
            (!it.isUpright && it.card.energyReversed == "light")
    }
    val shadows = cards.count {
        (it.isUpright && it.card.energyUpright == "shadow") ||
            (!it.isUpright && it.card.energyReversed == "shadow")
    }
    return when {
        lights >= 2 -> "light"
        shadows >= 2 -> "shadow"
        else -> "mixed"
    }
}

private fun tarotBuildOverall(
    cards: List<DrawnCard>,
    spread: TarotSpread,
    theme: TarotQuestionTheme,
    energyKey: String,
    templates: TarotReadingTemplates,
    isZh: Boolean
): String {
    val pool = if (isZh) {
        when (energyKey) {
            "light" -> templates.overallLightZh
            "shadow" -> templates.overallShadowZh
            else -> templates.overallMixedZh
        }
    } else {
        when (energyKey) {
            "light" -> templates.overallLightEn
            "shadow" -> templates.overallShadowEn
            else -> templates.overallMixedEn
        }
    }
    val energySentence = pool.randomOrNull() ?: ""
    val spreadDescription = if (isZh) spread.descriptionZh else spread.descriptionEn
    val dominantLine = tarotDominantEnergyLine(cards, theme, isZh)
    return listOf(energySentence, spreadDescription, dominantLine)
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
}

private fun tarotBuildPositionReading(
    card: DrawnCard,
    theme: TarotQuestionTheme,
    isZh: Boolean
): TarotPositionReading {
    val intro = tarotIntroText(card.positionAspect, theme, isZh)
    val meaning = card.card.localizedMeaning(card.isUpright, isZh)
    val keywords = card.card.localizedKeywords(card.isUpright, isZh).take(3)
        .joinToString(if (isZh) "、" else ", ")
    val content = if (isZh) {
        "$intro\n\n$meaning\n\n关键词：$keywords"
    } else {
        "$intro\n\n$meaning\n\nKeywords: $keywords"
    }
    return TarotPositionReading(
        positionLabel = card.positionLabel,
        aspect = card.positionAspect,
        cardNameZh = card.card.nameZh,
        cardNameEn = card.card.nameEn,
        isUpright = card.isUpright,
        interpretation = content
    )
}

private fun tarotBuildFocusInsight(
    cards: List<DrawnCard>,
    theme: TarotQuestionTheme,
    energyKey: String,
    isZh: Boolean
): String {
    val anchor = tarotAnchorCard(theme, cards)
    val anchorName = anchor.card.localizedName(isZh)
    val anchorMeaning = tarotFirstSentence(anchor.card.localizedMeaning(anchor.isUpright, isZh))
    val suitLine = tarotSuitInsight(theme, tarotDominantSuit(cards), energyKey, isZh)
    val anchorLine = if (isZh) {
        "$anchorName 是这次最贴近主题的一张牌，它提醒你：$anchorMeaning"
    } else {
        "$anchorName is the card closest to your focus right now, and its message is: $anchorMeaning"
    }
    return listOf(suitLine, anchorLine).filter { it.isNotBlank() }.joinToString("\n\n")
}

private fun tarotDominantEnergyLine(cards: List<DrawnCard>, theme: TarotQuestionTheme, isZh: Boolean): String {
    val majorCount = cards.count { it.card.arcana == "major" }
    if (majorCount >= 2) {
        return if (isZh)
            "两张以上的大牌同时出现，说明这不是一个小小情绪波动，而更像一次需要认真看见的阶段转折。"
        else
            "With two or more Major Arcana appearing together, this feels less like a passing mood and more like a meaningful turning point."
    }

    return when (tarotDominantSuit(cards)) {
        "cups" -> if (isZh)
            "这组三牌更偏向情感与关系层面，提醒你先照顾内在感受，再决定如何回应外部变化。"
        else
            "These cards lean toward emotion and relationship dynamics, asking you to honor inner feeling before reacting outwardly."
        "wands" -> if (isZh)
            "这组三牌的火元素更强，说明行动力、欲望与推进感会是这次占卜里的主旋律。"
        else
            "The fire of the spread is strong here, making action, desire, and forward movement the main current of the reading."
        "swords" -> if (isZh)
            "这组三牌强调认知与沟通，你越愿意说清楚、看清楚，局面就越容易松动。"
        else
            "The spread highlights thought and communication. The clearer you become, the more the situation can loosen and change."
        "pentacles" -> if (isZh)
            "这组三牌更关心现实基础，节奏、资源和安全感会比表面情绪更值得优先处理。"
        else
            "These cards care about practical foundations. Pace, resources, and steadiness deserve more attention than surface emotion."
        else -> if (theme == TarotQuestionTheme.GENERAL) {
            if (isZh)
                "这次牌阵没有单一元素压过其他声音，更适合把它当成一面整体观察自己的镜子。"
            else
                "No single suit dominates this spread, which makes it better read as a full mirror of your current state."
        } else ""
    }
}

private fun tarotIntroText(aspect: String, theme: TarotQuestionTheme, isZh: Boolean): String {
    return when (aspect) {
        "past" -> if (isZh)
            "过去位显示，哪些经历仍在悄悄影响你现在的感受与判断。"
        else
            "The past position shows which earlier experiences are still shaping your current responses."
        "present" -> if (isZh)
            "现在位揭示，你此刻最需要面对的核心能量正在这里。"
        else
            "The present position names the core energy that most needs your attention right now."
        "next_step" -> if (isZh)
            "下一步位不强调结果，而提醒你眼下最值得采取的温柔动作。"
        else
            "The next-step position focuses less on destiny and more on the gentlest move worth taking now."
        "self_in_love" -> if (isZh)
            "这一位描摹你在关系中的姿态，也映照你目前最真实的情感需求。"
        else
            "This position reflects how you are showing up in love and what your heart truly needs."
        "connection" -> if (isZh)
            "这一位观察关系本身的流动，帮助你看见彼此之间真正发生了什么。"
        else
            "This position looks at the relationship current itself and what is really moving between you."
        "gentle_advice" -> if (isZh)
            "这一位给的是关系里的温柔建议，不是命令，而是更适合靠近的方式。"
        else
            "This position offers gentle relational advice, not a command but a softer way to approach things."
        "current_path" -> if (isZh)
            "这一位呈现你当下的工作或学业路径，说明你正走在怎样的节奏里。"
        else
            "This position describes your current path in work or study and the rhythm you are already in."
        "hidden_block" -> if (isZh)
            "这一位提醒你，真正拖慢推进的，往往不是表面的忙，而是更深一层的阻力。"
        else
            "This position points to the hidden friction underneath the obvious busyness."
        "opportunity" -> if (isZh)
            "这一位不是空泛好运，而是告诉你最值得回应的机会入口在哪里。"
        else
            "This position highlights not vague luck, but the opening that is genuinely worth responding to."
        "current_flow" -> if (isZh)
            "这一位显示金钱与资源目前的流向，帮你看清收支和安全感的底色。"
        else
            "This position shows the current flow of money and resources, revealing the tone beneath your sense of security."
        "resource_pattern" -> if (isZh)
            "这一位映照你一贯使用资源的方式，也暴露了最常见的循环与惯性。"
        else
            "This position reflects your deeper pattern around handling resources and the loops you repeat most often."
        "next_move" -> if (isZh)
            "这一位提醒你，财富议题最有效的改变，通常来自下一步的小而准的动作。"
        else
            "This position suggests that the most effective shift in wealth often comes from one precise next move."
        else -> if (isZh) {
            "${theme.localizedName(true)}里，这张牌正在给你一面更具体的镜子。"
        } else {
            "Within this focus, the card offers a more specific mirror."
        }
    }
}

private fun tarotSuitInsight(
    theme: TarotQuestionTheme,
    dominantSuit: String,
    energyKey: String,
    isZh: Boolean
): String {
    return when (theme) {
        TarotQuestionTheme.LOVE -> when (dominantSuit) {
            "cups" -> if (isZh)
                "爱情主题里，圣杯占优说明真正关键的不是技巧，而是情绪有没有被认真接住。"
            else
                "With Cups leading in a love reading, the real question is whether feelings are being genuinely held and met."
            "swords" -> if (isZh)
                "爱情主题里，宝剑偏多往往表示需要更诚实的沟通，而不是继续靠猜测维持平静。"
            else
                "In a love reading, a strong Swords presence usually asks for clearer honesty instead of keeping the peace through guesswork."
            "pentacles" -> if (isZh)
                "爱情主题里，星币偏多说明安全感、现实安排与长期承诺比表面浪漫更重要。"
            else
                "In a love reading, Pentacles emphasize safety, practical arrangements, and reliable commitment over surface romance."
            "wands" -> if (isZh)
                "爱情主题里，权杖偏多说明吸引力与主动性都很强，但也要留意热度是否跑在理解前面。"
            else
                "In a love reading, Wands bring strong attraction and momentum, but ask whether heat is moving faster than understanding."
            else -> tarotDefaultEnergyInsight(energyKey, isZh)
        }
        TarotQuestionTheme.CAREER -> when (dominantSuit) {
            "wands" -> if (isZh)
                "事业主题里，权杖占优代表行动窗口已经打开，关键在于立刻把想法推进成节奏。"
            else
                "In a career reading, Wands suggest the action window is open and ideas now need movement and rhythm."
            "swords" -> if (isZh)
                "事业主题里，宝剑偏多说明判断、沟通和策略会比蛮干更重要。"
            else
                "In a career reading, Swords suggest that discernment, communication, and strategy matter more than brute effort."
            "pentacles" -> if (isZh)
                "事业主题里，星币偏多说明结果来自稳步积累，系统感与执行力会比灵感更值钱。"
            else
                "In a career reading, Pentacles point to steady accumulation, where structure and follow-through outperform raw inspiration."
            "cups" -> if (isZh)
                "事业主题里，圣杯偏多提醒你，团队关系和内在认同感会直接影响输出质量。"
            else
                "In a career reading, Cups show that team dynamics and emotional alignment directly affect performance."
            else -> tarotDefaultEnergyInsight(energyKey, isZh)
        }
        TarotQuestionTheme.WEALTH -> when (dominantSuit) {
            "pentacles" -> if (isZh)
                "财富主题里，星币占优说明这件事更适合靠结构、预算与长期安排来优化。"
            else
                "In a wealth reading, Pentacles suggest the answer lies in structure, budgeting, and longer-range planning."
            "swords" -> if (isZh)
                "财富主题里，宝剑偏多说明先把模糊账目、焦虑决策或错误判断看清楚，钱才会听话。"
            else
                "In a wealth reading, Swords ask you to clear the fog around numbers, anxious decisions, or mistaken assumptions."
            "cups" -> if (isZh)
                "财富主题里，圣杯偏多时，金钱往往和情绪、照顾他人或安全感需求绑得很紧。"
            else
                "In a wealth reading, Cups often show money tied closely to emotion, care-taking, or the need for reassurance."
            "wands" -> if (isZh)
                "财富主题里，权杖偏多说明你不是没有机会，而是需要更稳地处理冲劲与风险。"
            else
                "In a wealth reading, Wands suggest opportunity is present, but momentum and risk need steadier handling."
            else -> tarotDefaultEnergyInsight(energyKey, isZh)
        }
        TarotQuestionTheme.GENERAL -> tarotDefaultEnergyInsight(energyKey, isZh)
    }
}

private fun tarotDefaultEnergyInsight(energyKey: String, isZh: Boolean): String {
    return when (energyKey) {
        "light" -> if (isZh)
            "整体来看，这次的主题能量并不封闭，更多是在邀请你顺势整理并继续向前。"
        else
            "Overall, the energy is not closed off; it is inviting you to organize yourself and keep moving."
        "shadow" -> if (isZh)
            "整体来看，这次更像一次提醒你慢下来、看清楚再行动的抽牌，而不是催促结果立刻发生。"
        else
            "Overall, this feels more like a prompt to slow down and see clearly than a push for instant results."
        else -> if (isZh)
            "整体来看，这组牌没有给出单一答案，而是在提醒你接住复杂性，再从中挑出最重要的一条线。"
        else
            "Overall, the spread avoids one single answer and instead asks you to hold the complexity before choosing the thread that matters most."
    }
}

private fun tarotAnchorCard(theme: TarotQuestionTheme, cards: List<DrawnCard>): DrawnCard {
    return cards.maxByOrNull { tarotRelevance(it.card, theme) } ?: cards.first()
}

private fun tarotRelevance(card: com.fortunepocket.core.model.TarotCard, theme: TarotQuestionTheme): Int {
    val suit = card.semanticSuit
    return when (theme) {
        TarotQuestionTheme.GENERAL -> if (card.arcana == "major") 3 else 1
        TarotQuestionTheme.LOVE -> when (suit) {
            "cups" -> 4; "pentacles" -> 3; "wands" -> 2; "swords" -> 1
            else -> if (card.arcana == "major") 3 else 1
        }
        TarotQuestionTheme.CAREER -> when (suit) {
            "wands" -> 4; "pentacles" -> 3; "swords" -> 3; "cups" -> 1
            else -> if (card.arcana == "major") 3 else 1
        }
        TarotQuestionTheme.WEALTH -> when (suit) {
            "pentacles" -> 4; "swords" -> 2; "wands" -> 2; "cups" -> 1
            else -> if (card.arcana == "major") 3 else 1
        }
    }
}

private fun tarotDominantSuit(cards: List<DrawnCard>): String {
    return cards.groupingBy { it.card.semanticSuit }.eachCount()
        .maxByOrNull { it.value }?.key ?: "major"
}

private fun tarotBuildAdvice(card: DrawnCard, templates: TarotReadingTemplates, isZh: Boolean): String {
    val pool = if (isZh) templates.adviceZh else templates.adviceEn
    val template = pool.randomOrNull() ?: "{kw1}"
    val keywords = card.card.localizedKeywords(card.isUpright, isZh)
    val kw1 = keywords.getOrNull(0) ?: if (isZh) "觉察" else "awareness"
    val kw2 = keywords.getOrNull(1) ?: if (isZh) "平静" else "calm"
    val name = card.card.localizedName(isZh)
    return template
        .replace("{kw1}", kw1)
        .replace("{kw2}", kw2)
        .replace("{card_name}", name)
}

private fun tarotFirstSentence(text: String): String {
    listOf("。", ". ", "！", "？").forEach { separator ->
        val idx = text.indexOf(separator)
        if (idx >= 0) return text.substring(0, idx + separator.length)
    }
    return text
}

private fun tarotParseLuckyItems(raw: String, isZh: Boolean, json: Json): List<Pair<String, String>> {
    val wrapper = json.decodeFromString<TarotLuckyItemsWrapper>(raw)
    return wrapper.items.map { item ->
        Pair(if (isZh) item.nameZh else item.nameEn, if (isZh) item.energyZh else item.energyEn)
    }
}

private fun tarotParseLuckyColors(raw: String, isZh: Boolean, json: Json): List<Pair<String, String>> {
    val wrapper = json.decodeFromString<TarotLuckyColorsWrapper>(raw)
    return wrapper.colors.map { color -> Pair(if (isZh) color.nameZh else color.nameEn, color.hex) }
}

// ---------------------------------------------------------------------------
// Private serialization models
// ---------------------------------------------------------------------------

@Serializable
private data class TarotReadingTemplates(
    @SerialName("overall_light_zh") val overallLightZh: List<String>,
    @SerialName("overall_shadow_zh") val overallShadowZh: List<String>,
    @SerialName("overall_mixed_zh") val overallMixedZh: List<String>,
    @SerialName("overall_light_en") val overallLightEn: List<String>,
    @SerialName("overall_shadow_en") val overallShadowEn: List<String>,
    @SerialName("overall_mixed_en") val overallMixedEn: List<String>,
    @SerialName("love_prefix_zh") val lovePrefixZh: List<String>,
    @SerialName("love_prefix_en") val lovePrefixEn: List<String>,
    @SerialName("career_prefix_zh") val careerPrefixZh: List<String>,
    @SerialName("career_prefix_en") val careerPrefixEn: List<String>,
    @SerialName("wealth_prefix_zh") val wealthPrefixZh: List<String>,
    @SerialName("wealth_prefix_en") val wealthPrefixEn: List<String>,
    @SerialName("advice_zh") val adviceZh: List<String>,
    @SerialName("advice_en") val adviceEn: List<String>
)

@Serializable
private data class TarotLuckyItemsWrapper(val items: List<TarotLuckyItemJson>)

@Serializable
private data class TarotLuckyItemJson(
    @SerialName("name_zh") val nameZh: String,
    @SerialName("name_en") val nameEn: String,
    @SerialName("energy_zh") val energyZh: String,
    @SerialName("energy_en") val energyEn: String
)

@Serializable
private data class TarotLuckyColorsWrapper(val colors: List<TarotLuckyColorJson>)

@Serializable
private data class TarotLuckyColorJson(
    @SerialName("name_zh") val nameZh: String,
    @SerialName("name_en") val nameEn: String,
    val hex: String
)
