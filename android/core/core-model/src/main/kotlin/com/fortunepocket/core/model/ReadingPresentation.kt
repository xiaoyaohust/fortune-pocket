package com.fortunepocket.core.model

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.Json

data class ReadingPresentation(
    val title: String,
    val subtitle: String,
    val sections: List<ReadingPresentationSection>,
    val shareText: String
)

data class ReadingPresentationSection(
    val title: String,
    val body: String
)

object ReadingPresentationBuilder {

    private val json = Json { ignoreUnknownKeys = true }

    fun build(record: ReadingRecord, isZh: Boolean): ReadingPresentation? {
        return when (record.type) {
            ReadingType.TAROT -> tarotPresentation(record, isZh)
            ReadingType.ASTROLOGY -> horoscopePresentation(record, isZh)
            ReadingType.BAZI -> baziPresentation(record, isZh)
        }
    }

    fun shareText(reading: TarotReading, isZh: Boolean): String {
        val cardSummary = reading.drawnCards.joinToString("\n") { drawnCard ->
            val cardName = drawnCard.card.localizedName(isZh)
            val orientation = if (drawnCard.isUpright) {
                if (isZh) "正位" else "upright"
            } else {
                if (isZh) "逆位" else "reversed"
            }
            "${drawnCard.positionLabel}: $cardName ($orientation)"
        }

        val positionSummary = reading.positionReadings.joinToString("\n") { item ->
            "${item.positionLabel}: ${firstParagraph(item.interpretation)}"
        }

        return listOf(
            if (isZh) "Fortune Pocket · 今日塔罗" else "Fortune Pocket · Tarot Reading",
            reading.theme.localizedName(isZh),
            reading.spreadName,
            formatDate(reading.timestamp),
            "",
            cardSummary,
            "",
            "${if (isZh) "整体能量" else "Overall Energy"}: ${firstParagraph(reading.overallEnergy)}",
            "${reading.theme.localizedFocusTitle(isZh)}: ${firstParagraph(reading.focusInsight)}",
            positionSummary,
            "${if (isZh) "今日建议" else "Today's Guidance"}: ${firstParagraph(reading.advice)}",
            "${if (isZh) "幸运提示" else "Lucky Hint"}: ${reading.luckyItemName} / ${reading.luckyColorName} / ${reading.luckyNumber}"
        ).joinToString("\n")
    }

    fun shareText(reading: HoroscopeReading, isZh: Boolean): String {
        val placementSummary = reading.planetPlacements.take(5).joinToString("\n") { item ->
            val houseText = item.house?.let { if (isZh) "第${it}宫" else "House $it" }.orEmpty()
            "${item.planetId.localizedName(isZh)}: ${item.localizedSignName(isZh)} $houseText".trim()
        }
        val aspectSummary = reading.majorAspects.take(3).joinToString("\n") { aspect ->
            "${aspect.firstPlanetId.localizedName(isZh)} ${aspect.type.localizedName(isZh)} ${aspect.secondPlanetId.localizedName(isZh)}"
        }
        return listOf(
            if (isZh) "Fortune Pocket · 本命盘" else "Fortune Pocket · Natal Chart",
            reading.chartSignature,
            "${reading.birthDateText} · ${reading.birthTimeText}",
            reading.birthCityName,
            "",
            firstParagraph(reading.chartSummary),
            "",
            "${if (isZh) "行星落座" else "Placements"}:",
            placementSummary,
            if (aspectSummary.isNotEmpty()) "\n${if (isZh) "主要相位" else "Major Aspects"}:\n$aspectSummary" else "",
            "",
            "${if (isZh) "整体画像" else "Overall Profile"}: ${firstParagraph(reading.overall)}",
            "${if (isZh) "感情" else "Love"}: ${firstParagraph(reading.love)}",
            "${if (isZh) "事业" else "Career"}: ${firstParagraph(reading.career)}",
            "${if (isZh) "建议" else "Advice"}: ${firstParagraph(reading.advice)}",
            "${if (isZh) "幸运色" else "Lucky Color"}: ${reading.luckyColor}",
            "${if (isZh) "幸运数字" else "Lucky Number"}: ${reading.luckyNumber}"
        ).filter { it.isNotEmpty() }.joinToString("\n")
    }

    fun shareText(reading: BaziReading, isZh: Boolean): String {
        val dayMasterTitle = if (isZh) {
            "${reading.stemNameZh}日主"
        } else {
            "${reading.stemNameEn} Day Master"
        }

        return listOf(
            if (isZh) "Fortune Pocket · 八字能量" else "Fortune Pocket · Bazi Energy",
            dayMasterTitle,
            formatDate(reading.birthDate),
            "",
            "${if (isZh) "性格倾向" else "Personality"}: ${firstParagraph(reading.overallPersonality)}",
            "${if (isZh) "感情倾向" else "Love Tendencies"}: ${firstParagraph(reading.love)}",
            "${if (isZh) "事业倾向" else "Career Tendencies"}: ${firstParagraph(reading.career)}",
            "${if (isZh) "财运倾向" else "Wealth Tendencies"}: ${firstParagraph(reading.wealth)}",
            "${if (isZh) "当前阶段建议" else "Current Phase"}: ${firstParagraph(reading.phaseAdvice)}",
            "",
            reading.disclaimer
        ).joinToString("\n")
    }

    private fun tarotPresentation(record: ReadingRecord, isZh: Boolean): ReadingPresentation? {
        val detail = runCatching {
            json.decodeFromString<TarotReadingDetail>(record.detailJson)
        }.getOrNull() ?: return null

        val theme = detail.themeId?.let {
            TarotQuestionTheme.entries.firstOrNull { entry -> entry.name.equals(it, ignoreCase = true) }
        } ?: TarotQuestionTheme.GENERAL
        val cardsText = detail.drawnCards.joinToString("\n") { drawnCard ->
            val cardName = if (isZh) drawnCard.cardNameZh else drawnCard.cardNameEn
            val orientation = if (drawnCard.isUpright) {
                if (isZh) "正位" else "Upright"
            } else {
                if (isZh) "逆位" else "Reversed"
            }
            "${drawnCard.positionLabel} · $cardName ($orientation)"
        }

        if (detail.focusInsight != null && detail.positionReadings.isNotEmpty()) {
            val luckyParts = listOfNotNull(detail.luckyItem, detail.luckyColor, detail.luckyNumber?.toString())
                .joinToString(" / ")
            val sections = buildList {
                add(ReadingPresentationSection(if (isZh) "抽到的牌" else "Cards Drawn", cardsText))
                add(ReadingPresentationSection(if (isZh) "整体能量" else "Overall Energy", detail.overallEnergy))
                add(ReadingPresentationSection(theme.localizedFocusTitle(isZh), detail.focusInsight))
                detail.positionReadings.forEach { item ->
                    add(ReadingPresentationSection(item.title, item.body))
                }
                add(ReadingPresentationSection(if (isZh) "今日建议" else "Today's Guidance", detail.adviceTip))
                add(ReadingPresentationSection(if (isZh) "幸运提示" else "Lucky Hint", luckyParts))
            }

            return ReadingPresentation(
                title = record.title,
                subtitle = formatDate(record.createdAt),
                sections = sections,
                shareText = listOf(
                    if (isZh) "Fortune Pocket · 历史塔罗" else "Fortune Pocket · Saved Tarot Reading",
                    record.title,
                    formatDate(record.createdAt),
                    "",
                    cardsText,
                    "",
                    "${if (isZh) "整体能量" else "Overall Energy"}: ${firstParagraph(detail.overallEnergy)}",
                    "${theme.localizedFocusTitle(isZh)}: ${firstParagraph(detail.focusInsight)}",
                    "${if (isZh) "今日建议" else "Today's Guidance"}: ${firstParagraph(detail.adviceTip)}"
                ).joinToString("\n")
            )
        }

        val sections = listOf(
            ReadingPresentationSection(if (isZh) "抽到的牌" else "Cards Drawn", cardsText),
            ReadingPresentationSection(if (isZh) "整体能量" else "Overall Energy", detail.overallEnergy),
            ReadingPresentationSection(if (isZh) "感情解读" else "Love", detail.loveReading.orEmpty()),
            ReadingPresentationSection(if (isZh) "事业解读" else "Career", detail.careerReading.orEmpty()),
            ReadingPresentationSection(if (isZh) "财运解读" else "Wealth", detail.wealthReading.orEmpty()),
            ReadingPresentationSection(if (isZh) "今日建议" else "Today's Guidance", detail.adviceTip),
            ReadingPresentationSection(if (isZh) "幸运提示" else "Lucky Hint", detail.luckyItem)
        )

        return ReadingPresentation(
            title = record.title,
            subtitle = formatDate(record.createdAt),
            sections = sections,
            shareText = listOf(
                if (isZh) "Fortune Pocket · 历史塔罗" else "Fortune Pocket · Saved Tarot Reading",
                record.title,
                formatDate(record.createdAt),
                "",
                cardsText,
                "",
                "${if (isZh) "整体能量" else "Overall Energy"}: ${firstParagraph(detail.overallEnergy)}",
                "${if (isZh) "今日建议" else "Today's Guidance"}: ${firstParagraph(detail.adviceTip)}"
            ).joinToString("\n")
        )
    }

    private fun horoscopePresentation(record: ReadingRecord, isZh: Boolean): ReadingPresentation? {
        val detail = runCatching {
            json.decodeFromString<HoroscopeReadingDetail>(record.detailJson)
        }.getOrNull() ?: return null

        val signName = if (isZh) detail.signNameZh else detail.signNameEn
        val coreSigns = listOfNotNull(
            if (isZh) "太阳：$signName" else "Sun: $signName",
            detail.moonSignNameZh?.let { if (isZh) "月亮：$it" else "Moon: ${detail.moonSignNameEn ?: it}" },
            detail.risingSignNameZh?.let { if (isZh) "上升：$it" else "Rising: ${detail.risingSignNameEn ?: it}" }
        ).joinToString("\n")
        val placementText = detail.planetPlacements.takeIf { it.isNotEmpty() }?.joinToString("\n") { item ->
            val houseText = item.house?.let { if (isZh) "第${it}宫" else "House $it" }.orEmpty()
            "${item.planetId.localizedName(isZh)}: ${item.localizedSignName(isZh)} $houseText".trim()
        }
        val aspectText = detail.majorAspects.takeIf { it.isNotEmpty() }?.joinToString("\n") { item ->
            "${item.firstPlanetId.localizedName(isZh)} · ${item.type.localizedName(isZh)} · ${item.secondPlanetId.localizedName(isZh)}"
        }

        val sections = buildList {
            add(ReadingPresentationSection(if (isZh) "本命盘" else "Natal Chart", detail.chartSignature ?: coreSigns))
            detail.chartSummary?.let {
                add(ReadingPresentationSection(if (isZh) "图谱摘要" else "Chart Summary", it))
            }
            add(ReadingPresentationSection(if (isZh) "核心星位" else "Core Signs", coreSigns))
            if (!placementText.isNullOrEmpty()) {
                add(ReadingPresentationSection(if (isZh) "行星落座" else "Placements", placementText))
            }
            if (!aspectText.isNullOrEmpty()) {
                add(ReadingPresentationSection(if (isZh) "主要相位" else "Major Aspects", aspectText))
            }
            add(ReadingPresentationSection(if (isZh) "整体画像" else "Overall Profile", detail.overall))
            add(ReadingPresentationSection(if (isZh) "感情与亲密关系" else "Love", detail.love))
            add(ReadingPresentationSection(if (isZh) "事业与成长方向" else "Career", detail.career))
            add(ReadingPresentationSection(if (isZh) "财富与安全感" else "Wealth", detail.wealth))
            add(ReadingPresentationSection(if (isZh) "社交与沟通" else "Social", detail.social))
            add(ReadingPresentationSection(if (isZh) "给你的建议" else "Guidance", detail.advice))
            add(ReadingPresentationSection(if (isZh) "幸运提示" else "Lucky Hint", "${detail.luckyColor} / ${detail.luckyNumber}"))
        }

        return ReadingPresentation(
            title = record.title,
            subtitle = formatDate(record.createdAt),
            sections = sections,
            shareText = listOf(
                if (isZh) "Fortune Pocket · 已保存本命盘" else "Fortune Pocket · Saved Natal Chart",
                detail.chartSignature ?: signName,
                listOfNotNull(detail.date, detail.birthTime, detail.birthCityName).joinToString(" · "),
                "",
                detail.chartSummary.orEmpty(),
                "${if (isZh) "整体画像" else "Overall Profile"}: ${firstParagraph(detail.overall)}",
                "${if (isZh) "感情" else "Love"}: ${firstParagraph(detail.love)}",
                "${if (isZh) "事业" else "Career"}: ${firstParagraph(detail.career)}",
                "${if (isZh) "建议" else "Advice"}: ${firstParagraph(detail.advice)}"
            ).filter { it.isNotEmpty() }.joinToString("\n")
        )
    }

    private fun baziPresentation(record: ReadingRecord, isZh: Boolean): ReadingPresentation? {
        // Try new chart-snapshot format first
        runCatching { json.decodeFromString<BaziChartSnapshot>(record.detailJson) }
            .getOrNull()?.let { snapshot ->
                val pillarsText = listOfNotNull(snapshot.year, snapshot.month, snapshot.day, snapshot.hour)
                    .joinToString(" · ")
                val cycleText = if (isZh)
                    "${snapshot.cycleDirection}，起运 ${snapshot.startingAge} 岁"
                else
                    "${snapshot.cycleDirection}, starts at age ${snapshot.startingAge}"
                val sections = listOf(
                    ReadingPresentationSection(if (isZh) "四柱" else "Four Pillars", pillarsText),
                    ReadingPresentationSection(if (isZh) "日主五行" else "Day Master Element", snapshot.dayMasterElement),
                    ReadingPresentationSection(if (isZh) "大运信息" else "Major Cycle", cycleText)
                )
                return ReadingPresentation(
                    title = record.title,
                    subtitle = formatDate(record.createdAt),
                    sections = sections,
                    shareText = listOf(
                        if (isZh) "Fortune Pocket · 八字排盘" else "Fortune Pocket · Four Pillars Chart",
                        record.title,
                        "",
                        "${if (isZh) "四柱" else "Four Pillars"}: $pillarsText",
                        "${if (isZh) "日主五行" else "Day Master"}: ${snapshot.dayMasterElement}",
                        cycleText
                    ).joinToString("\n")
                )
            }

        // Fallback: legacy entertainment-text format
        val detail = runCatching {
            json.decodeFromString<BaziReadingDetail>(record.detailJson)
        }.getOrNull() ?: return null

        val birthSummary = listOfNotNull(detail.birthDate, detail.birthHour).joinToString(" · ")
        val sections = listOf(
            ReadingPresentationSection(if (isZh) "出生信息" else "Birth Info", birthSummary),
            ReadingPresentationSection(if (isZh) "性格倾向" else "Personality", detail.overallPersonality),
            ReadingPresentationSection(if (isZh) "感情倾向" else "Love Tendencies", detail.loveTendency),
            ReadingPresentationSection(if (isZh) "事业倾向" else "Career Tendencies", detail.careerTendency),
            ReadingPresentationSection(if (isZh) "财运倾向" else "Wealth Tendencies", detail.wealthTendency),
            ReadingPresentationSection(if (isZh) "当前阶段建议" else "Current Phase", detail.phaseAdvice),
            ReadingPresentationSection(if (isZh) "五行气质" else "Element Vibe", detail.elementDescription)
        )
        return ReadingPresentation(
            title = record.title,
            subtitle = formatDate(record.createdAt),
            sections = sections,
            shareText = listOf(
                if (isZh) "Fortune Pocket · 历史八字" else "Fortune Pocket · Saved Bazi Reading",
                record.title, birthSummary, "",
                "${if (isZh) "性格倾向" else "Personality"}: ${firstParagraph(detail.overallPersonality)}",
                "${if (isZh) "当前阶段建议" else "Current Phase"}: ${firstParagraph(detail.phaseAdvice)}"
            ).joinToString("\n")
        )
    }

    private fun formatDate(timestamp: Long): String {
        return DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault())
            .format(Date(timestamp))
    }

    private fun firstParagraph(text: String): String {
        val idx = text.indexOf("\n\n")
        return if (idx >= 0) text.substring(0, idx) else firstSentence(text)
    }

    private fun firstSentence(text: String): String {
        listOf("。", ". ", "！", "？").forEach { separator ->
            val idx = text.indexOf(separator)
            if (idx >= 0) return text.substring(0, idx + separator.length)
        }
        return text
    }
}
