package com.fortunepocket.core.content

import com.fortunepocket.core.content.astrology.AstrologyChartSnapshot
import com.fortunepocket.core.model.AstrologyAspect
import com.fortunepocket.core.model.AstrologyAspectType
import com.fortunepocket.core.model.AstrologyPlanetID

data class AstrologyInsightBundle(
    val personalityCore: String,
    val relationshipPattern: String,
    val strengths: String,
    val growthEdge: String,
    val currentTheme: String
)

object AstrologyInsightBuilder {

    fun build(chart: AstrologyChartSnapshot, isZh: Boolean): AstrologyInsightBundle {
        val rising = chart.risingSign
        val venus = chart.planetPlacements.firstOrNull { it.planetId == AstrologyPlanetID.VENUS }
        val mars = chart.planetPlacements.firstOrNull { it.planetId == AstrologyPlanetID.MARS }
        val leadHouse = chart.houseFocus.firstOrNull()
        val hardAspect = chart.majorAspects.firstOrNull {
            it.type == AstrologyAspectType.SQUARE || it.type == AstrologyAspectType.OPPOSITION
        }

        val personalityCore: String
        val relationshipPattern: String
        val strengths: String
        val growthEdge: String
        val currentTheme: String

        if (isZh) {
            personalityCore =
                "你的核心驱动力来自${chart.sunSign.nameZh}的${chart.sunSign.traitsZh.firstOrNull() ?: "自我表达"}，内在情绪底色带着${chart.moonSign.nameZh}式的${chart.moonSign.traitsZh.firstOrNull() ?: "感受力"}。别人初见你时，往往会先接收到${rising.nameZh}那种${rising.traitsZh.firstOrNull() ?: "外在气场"}。"

            val venusPart = venus?.let {
                "金星落在${it.signNameZh}，说明你会被${it.signNameZh}式的回应方式打动。"
            } ?: "你的关系感受力，更多从金星与月亮的搭配里流露出来。"
            val marsPart = mars?.let {
                "火星落在${it.signNameZh}，也让你在靠近或退开时，带着${it.signNameZh}的节奏。"
            } ?: "行动层面则更多由火星节奏决定。"
            relationshipPattern =
                "$venusPart$marsPart 关系里你最需要的，不只是喜欢本身，而是被真正理解后的安心感。"

            val coreStrengths = listOfNotNull(chart.sunSign.strengthsZh.firstOrNull(), rising.strengthsZh.firstOrNull())
                .joinToString("、")
            strengths =
                "你最容易被别人感受到的优势，是$coreStrengths。再加上${chart.elementBalance.localizedDominantElement(true)}偏强，你一旦进入状态，往往能把气场、判断和执行力串成一条线。"

            val softSpot = chart.moonSign.weaknessesZh.firstOrNull() ?: "过度在意外界回应"
            val tension = hardAspect?.let { "${readableAspect(it, true)} " } ?: ""
            growthEdge =
                "${tension}这张盘真正的成长课题，不是变得更完美，而是看见自己何时会因为${softSpot}而过度消耗。越能及时收回注意力，你的能量就越稳定。"

            val houseLine = leadHouse?.localizedSummary(true)
                ?: "这段时间更适合把精力投向真正重要的主轴，而不是被零碎噪音带着走。"
            currentTheme =
                "你最近的主题像是在重新整理生活重心。$houseLine 所以现在最值得做的，不是多想一步，而是先把最重要的那一步走稳。"
        } else {
            personalityCore =
                "Your core drive carries the ${chart.sunSign.traitsEn.firstOrNull() ?: "self-expressive"} tone of ${chart.sunSign.nameEn}, while your emotional baseline moves more like ${chart.moonSign.nameEn}. First impressions often arrive through the ${rising.traitsEn.firstOrNull() ?: "distinct"} presence of your Rising sign in ${rising.nameEn}."

            val venusPart = venus?.let {
                "Venus in ${it.signNameEn} shows what kind of emotional response helps you feel chosen."
            } ?: "Your relational style is mostly revealed through the Moon and Venus blend in the chart."
            val marsPart = mars?.let {
                "Mars in ${it.signNameEn} also shapes how quickly you move toward or away from closeness."
            } ?: "Action and desire still follow a clear Mars rhythm in the background."
            relationshipPattern =
                "$venusPart $marsPart What you need in love is not just chemistry, but the calm of feeling genuinely understood."

            val coreStrengths = listOfNotNull(chart.sunSign.strengthsEn.firstOrNull(), rising.strengthsEn.firstOrNull())
                .joinToString(", ")
            strengths =
                "Your most visible strengths are $coreStrengths. With a chart that leans ${chart.elementBalance.localizedDominantElement(false).lowercase()}, you tend to bring presence, judgment, and execution into the same current once you commit."

            val softSpot = chart.moonSign.weaknessesEn.firstOrNull() ?: "over-reading external signals"
            val tension = hardAspect?.let { "${readableAspect(it, false)} " } ?: ""
            growthEdge =
                "${tension}Your growth edge is less about becoming flawless and more about noticing when $softSpot starts draining your energy. The sooner you reclaim your attention, the steadier you become."

            val houseLine = leadHouse?.localizedSummary(false)
                ?: "This season asks you to return to what matters most instead of scattering energy across noise."
            currentTheme =
                "Your current chapter feels like a realignment of priorities. $houseLine The most helpful move now is not adding more analysis, but steadying the one next step that truly matters."
        }

        return AstrologyInsightBundle(
            personalityCore = personalityCore,
            relationshipPattern = relationshipPattern,
            strengths = strengths,
            growthEdge = growthEdge,
            currentTheme = currentTheme
        )
    }

    private fun readableAspect(aspect: AstrologyAspect, isZh: Boolean): String {
        val first = aspect.firstPlanetId.localizedName(isZh)
        val second = aspect.secondPlanetId.localizedName(isZh)
        val aspectName = aspect.type.localizedName(isZh)
        return if (isZh) {
            "${first}与${second}形成${aspectName}，"
        } else {
            "$first forms a ${aspectName.lowercase()} with $second,"
        }
    }
}
