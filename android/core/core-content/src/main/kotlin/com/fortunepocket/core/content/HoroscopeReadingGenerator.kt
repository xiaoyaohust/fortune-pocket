package com.fortunepocket.core.content

import com.fortunepocket.core.content.astrology.AstrologyChartCalculator
import com.fortunepocket.core.content.astrology.AstrologyInput
import com.fortunepocket.core.model.AstrologyAspect
import com.fortunepocket.core.model.AstrologyHouseFocus
import com.fortunepocket.core.model.AstrologyPlanetID
import com.fortunepocket.core.model.AstrologyPlanetPlacement
import com.fortunepocket.core.model.HoroscopeReading
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HoroscopeReadingGenerator @Inject constructor(
    private val contentLoader: ContentLoader
) {

    fun generate(input: AstrologyInput): HoroscopeReading {
        val isZh = Locale.getDefault().language == "zh"
        val signs = contentLoader.loadZodiacSigns().signs
        val chart = AstrologyChartCalculator.calculate(input = input, signs = signs, isZh = isZh)

        val venus = placement(AstrologyPlanetID.VENUS, chart.planetPlacements)
        val mars = placement(AstrologyPlanetID.MARS, chart.planetPlacements)
        val mercury = placement(AstrologyPlanetID.MERCURY, chart.planetPlacements)
        val jupiter = placement(AstrologyPlanetID.JUPITER, chart.planetPlacements)
        val saturn = placement(AstrologyPlanetID.SATURN, chart.planetPlacements)

        val chartSummary = if (isZh) {
            "这张盘以${chart.sunSign.nameZh}的核心意志、${chart.moonSign.nameZh}的情绪底色，以及${chart.risingSign.nameZh}的外在气场为主轴。"
        } else {
            "This chart is anchored by the Sun in ${chart.sunSign.nameEn}, the Moon in ${chart.moonSign.nameEn}, and the Rising sign in ${chart.risingSign.nameEn}."
        }
        val chartSignature = if (isZh) {
            "太阳${chart.sunSign.nameZh} · 月亮${chart.moonSign.nameZh} · 上升${chart.risingSign.nameZh}"
        } else {
            "Sun ${chart.sunSign.nameEn} · Moon ${chart.moonSign.nameEn} · Rising ${chart.risingSign.nameEn}"
        }
        val dominantElement = chart.elementBalance.localizedDominantElement(isZh)
        val leadingHouse = chart.houseFocus.firstOrNull()
        val emotionalAspect = aspect(setOf(AstrologyPlanetID.MOON, AstrologyPlanetID.VENUS), chart.majorAspects)
        val careerAspect = aspect(setOf(AstrologyPlanetID.SUN, AstrologyPlanetID.SATURN, AstrologyPlanetID.MARS, AstrologyPlanetID.JUPITER), chart.majorAspects)
        val socialAspect = aspect(setOf(AstrologyPlanetID.MERCURY, AstrologyPlanetID.MOON), chart.majorAspects)

        val overall = if (isZh) {
            "$chartSummary 主导能量偏向$dominantElement，你通常会以${chart.risingSign.localizedTraits(true).firstOrNull() ?: chart.risingSign.nameZh}的方式走进世界，也会用${chart.moonSign.localizedTraits(true).firstOrNull() ?: chart.moonSign.nameZh}去消化感受。${leadingHouse?.localizedSummary(true).orEmpty()}"
        } else {
            "$chartSummary The chart leans ${dominantElement.lowercase(Locale.US)} overall, so you often enter the world through a ${chart.risingSign.localizedTraits(false).firstOrNull() ?: chart.risingSign.nameEn} tone and process feelings in a ${chart.moonSign.localizedTraits(false).firstOrNull() ?: chart.moonSign.nameEn} way. ${leadingHouse?.localizedSummary(false).orEmpty()}"
        }

        val love = if (isZh) {
            "感情层面，金星落在${venus.localizedSignName(true)}，说明你在关系里重视${signValueFocus(venus.signId, true)}；火星落在${mars.localizedSignName(true)}，也让你在靠近他人时带着${signActionFocus(mars.signId, true)}。${emotionalAspect?.let { readableAspect(it, true) } ?: "你的情感表达更受月亮与金星的位置牵引。"} ${houseLine(listOf(5, 7, 8), chart.houseFocus, true)}"
        } else {
            "In love, Venus in ${venus.localizedSignName(false)} shows that you value ${signValueFocus(venus.signId, false)}, while Mars in ${mars.localizedSignName(false)} adds ${signActionFocus(mars.signId, false)} to the way you pursue closeness. ${emotionalAspect?.let { readableAspect(it, false) } ?: "Your emotional style is strongly shaped by the Moon and Venus placements."} ${houseLine(listOf(5, 7, 8), chart.houseFocus, false)}"
        }

        val career = if (isZh) {
            "事业与学业上，太阳落在${chart.sunSign.localizedName(true)}，让你在长期方向上追求${signDriveFocus(chart.sunSign.id, true)}；木星在${jupiter.localizedSignName(true)}、土星在${saturn.localizedSignName(true)}，提示你扩张与稳住的方式并不相同。${careerAspect?.let { readableAspect(it, true) } ?: "事业节奏更多由太阳、火星、木星与土星的组合来决定。"} ${houseLine(listOf(6, 10, 11), chart.houseFocus, true)}"
        } else {
            "For career and study, the Sun in ${chart.sunSign.localizedName(false)} points toward ${signDriveFocus(chart.sunSign.id, false)}, while Jupiter in ${jupiter.localizedSignName(false)} and Saturn in ${saturn.localizedSignName(false)} show that growth and discipline do not move in exactly the same way. ${careerAspect?.let { readableAspect(it, false) } ?: "Career rhythms are largely shaped by how the Sun, Mars, Jupiter, and Saturn combine."} ${houseLine(listOf(6, 10, 11), chart.houseFocus, false)}"
        }

        val wealth = if (isZh) {
            "财务观更像是一种长期习惯，而不是单次运气。金星在${venus.localizedSignName(true)}、木星在${jupiter.localizedSignName(true)}，说明你会倾向通过${signValueFocus(venus.signId, true)}来建立安全感；土星在${saturn.localizedSignName(true)}则提醒你，真正的稳定来自持续结构而不是一时热度。${houseLine(listOf(2, 8, 10), chart.houseFocus, true)}"
        } else {
            "Money patterns in this chart are more about long-term habits than sudden luck. Venus in ${venus.localizedSignName(false)} and Jupiter in ${jupiter.localizedSignName(false)} suggest you build security through ${signValueFocus(venus.signId, false)}, while Saturn in ${saturn.localizedSignName(false)} reminds you that stability comes from structure rather than short bursts of intensity. ${houseLine(listOf(2, 8, 10), chart.houseFocus, false)}"
        }

        val social = if (isZh) {
            "社交互动里，水星落在${mercury.localizedSignName(true)}，说明你表达时偏向${signCommunicationFocus(mercury.signId, true)}；月亮在${chart.moonSign.localizedName(true)}也让你特别在意关系里的情绪气候。${socialAspect?.let { readableAspect(it, true) } ?: "你的沟通气质主要从水星与月亮的组合里流露出来。"} ${houseLine(listOf(3, 7, 11), chart.houseFocus, true)}"
        } else {
            "Socially, Mercury in ${mercury.localizedSignName(false)} suggests you communicate through ${signCommunicationFocus(mercury.signId, false)}, while the Moon in ${chart.moonSign.localizedName(false)} makes you especially sensitive to the emotional climate between people. ${socialAspect?.let { readableAspect(it, false) } ?: "Your communication style is mainly revealed through the blend of Mercury and the Moon."} ${houseLine(listOf(3, 7, 11), chart.houseFocus, false)}"
        }

        val advice = if (chart.majorAspects.any { it.type.name == "SQUARE" || it.type.name == "OPPOSITION" }) {
            if (isZh) {
                "你的星盘里存在几组推动成长的紧张相位，这并不意味着人生更难，而是说明你更适合在拉扯中提炼自己的节奏。建议你优先照顾${leadingHouse?.localizedTitle(true) ?: "当下最常被你忽略的主题"}，先把节奏稳住，再做决定。"
            } else {
                "This chart contains a few tension-building aspects. That does not mean life is harder by default; it means growth often comes through learning how to regulate contrast. Start with ${leadingHouse?.localizedTitle(false) ?: "the area that keeps asking for care"}, steady your pace there, and let decisions come afterward."
            }
        } else {
            if (isZh) {
                "这张盘的主要相位整体偏顺畅，说明你的很多力量可以自然流动。建议你把精力投入到${leadingHouse?.localizedTitle(true) ?: "真正重要的主题"}，让已经有优势的地方持续发光。"
            } else {
                "The major aspects here flow more naturally overall, so many of your strengths can move without excessive friction. Put your energy into ${leadingHouse?.localizedTitle(false) ?: "the themes that truly matter"}, and let your strongest patterns work for you."
            }
        }

        val luckyColor = luckyColor(chart.elementBalance.dominantElementKey, isZh, chart.sunSign)
        val luckyNumber = ((chart.sunSign.index + chart.moonSign.index + chart.risingSign.index) % 9) + 1

        return HoroscopeReading(
            sign = chart.sunSign,
            moonSign = chart.moonSign,
            risingSign = chart.risingSign,
            birthDateText = chart.birthDateText,
            birthTimeText = chart.birthTimeText,
            birthCityName = chart.birthCityName,
            timeZoneId = chart.timeZoneId,
            chartSummary = chartSummary,
            chartSignature = chartSignature,
            overall = overall,
            love = love,
            career = career,
            wealth = wealth,
            social = social,
            advice = advice,
            luckyColor = luckyColor,
            luckyNumber = luckyNumber,
            planetPlacements = chart.planetPlacements,
            majorAspects = chart.majorAspects,
            elementBalance = chart.elementBalance,
            houseFocus = chart.houseFocus
        )
    }

    private fun readableAspect(aspect: AstrologyAspect, isZh: Boolean): String {
        val first = aspect.firstPlanetId.localizedName(isZh)
        val second = aspect.secondPlanetId.localizedName(isZh)
        val aspectName = aspect.type.localizedName(isZh)
        return if (isZh) {
            "${first}与${second}形成${aspectName}，这会让相关主题更容易被你明确感受到。"
        } else {
            "$first forms a ${aspectName.lowercase(Locale.US)} with $second, making that dynamic easier to feel consciously."
        }
    }

    private fun houseLine(houses: List<Int>, focus: List<com.fortunepocket.core.model.AstrologyHouseFocus>, isZh: Boolean): String {
        return focus.firstOrNull { houses.contains(it.house) }?.localizedSummary(isZh).orEmpty()
    }

    private fun aspect(planets: Set<AstrologyPlanetID>, aspects: List<AstrologyAspect>): AstrologyAspect? {
        return aspects.firstOrNull {
            planets.contains(it.firstPlanetId) || planets.contains(it.secondPlanetId)
        }
    }

    private fun placement(id: AstrologyPlanetID, placements: List<AstrologyPlanetPlacement>): AstrologyPlanetPlacement {
        return placements.firstOrNull { it.planetId == id } ?: placements.first()
    }

    private fun signValueFocus(signId: String, isZh: Boolean): String = when (signId) {
        "taurus", "virgo", "capricorn" -> if (isZh) "稳定、具体、可落地的回应" else "stability, tangible gestures, and grounded consistency"
        "gemini", "libra", "aquarius" -> if (isZh) "交流、共识与精神层面的同步" else "conversation, shared perspective, and mental resonance"
        "cancer", "scorpio", "pisces" -> if (isZh) "情绪上的理解、深度与安全感" else "emotional understanding, depth, and a sense of safety"
        else -> if (isZh) "热度、主动性与清晰的回应" else "warmth, initiative, and clear signals"
    }

    private fun signActionFocus(signId: String, isZh: Boolean): String = when (signId) {
        "aries", "leo", "sagittarius" -> if (isZh) "直接而热烈的推进感" else "direct, fiery momentum"
        "taurus", "virgo", "capricorn" -> if (isZh) "慢热但持续的行动方式" else "a slower but sustained style of action"
        "gemini", "libra", "aquarius" -> if (isZh) "先沟通、再靠近的节奏" else "a communicate-first rhythm"
        else -> if (isZh) "情绪带动下的靠近方式" else "an emotionally led approach to closeness"
    }

    private fun signDriveFocus(signId: String, isZh: Boolean): String = when (signId) {
        "aries", "capricorn", "leo" -> if (isZh) "自己开路、承担主导角色" else "initiating, leading, and setting direction"
        "virgo", "taurus" -> if (isZh) "把复杂的事一步步做实" else "turning complexity into practical progress"
        "gemini", "aquarius", "libra" -> if (isZh) "连接信息、人脉与更大的视角" else "connecting ideas, people, and broader perspective"
        else -> if (isZh) "把情绪、愿景与长期意义融进工作里" else "weaving emotion, vision, and meaning into your work"
    }

    private fun signCommunicationFocus(signId: String, isZh: Boolean): String = when (signId) {
        "gemini", "libra", "aquarius" -> if (isZh) "轻盈、机敏、带有思辨感" else "lightness, agility, and an intellectual tone"
        "taurus", "virgo", "capricorn" -> if (isZh) "具体、谨慎、注重细节" else "clarity, caution, and practical detail"
        "cancer", "scorpio", "pisces" -> if (isZh) "感受先行、语气更具共情力" else "emotion-first language with strong empathy"
        else -> if (isZh) "直接表达、热情推进" else "direct expression and energetic momentum"
    }

    private fun luckyColor(dominantElementKey: String, isZh: Boolean, sunSign: com.fortunepocket.core.model.ZodiacSign): String = when (dominantElementKey) {
        "fire" -> if (isZh) "金红色" else "Golden Crimson"
        "earth" -> if (isZh) "苔绿色" else "Moss Green"
        "air" -> if (isZh) "雾蓝色" else "Mist Blue"
        "water" -> if (isZh) "月银色" else "Moon Silver"
        else -> if (isZh) sunSign.luckyColorZh else sunSign.luckyColorEn
    }
}
