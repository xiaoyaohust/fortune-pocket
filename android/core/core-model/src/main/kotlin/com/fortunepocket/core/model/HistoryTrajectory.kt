package com.fortunepocket.core.model

import kotlinx.serialization.json.Json

data class HistoryTrajectoryInsight(
    val title: String,
    val body: String
)

data class HistoryTrajectorySnapshot(
    val headline: String,
    val insights: List<HistoryTrajectoryInsight>
)

object HistoryTrajectoryBuilder {

    private val json = Json { ignoreUnknownKeys = true }

    fun build(records: List<ReadingRecord>, isZh: Boolean): HistoryTrajectorySnapshot? {
        if (records.isEmpty()) return null

        val recentRecords = records.take(18)
        val themeCounts = linkedMapOf<String, Int>()
        val cardCounts = linkedMapOf<String, Int>()
        var inwardSignals = 0
        var outwardSignals = 0
        var groundedSignals = 0
        var airySignals = 0
        val recordTypeCounts = linkedMapOf<ReadingType, Int>()

        recentRecords.forEach { record ->
            recordTypeCounts[record.type] = (recordTypeCounts[record.type] ?: 0) + 1
            when (record.type) {
                ReadingType.TAROT -> {
                    val detail = runCatching {
                        json.decodeFromString<TarotReadingDetail>(record.detailJson)
                    }.getOrNull() ?: return@forEach
                    detail.themeId?.let { raw ->
                        val name = when (raw.lowercase()) {
                            "general" -> if (isZh) "综合主题" else "General Focus"
                            "love", "relationship" -> if (isZh) "关系主题" else "Relationship Focus"
                            "career" -> if (isZh) "事业主题" else "Career Focus"
                            "wealth" -> if (isZh) "财富主题" else "Wealth Focus"
                            else -> raw
                        }
                        themeCounts[name] = (themeCounts[name] ?: 0) + 1
                    }
                    detail.drawnCards.forEach { card ->
                        val name = if (isZh) card.cardNameZh else card.cardNameEn
                        cardCounts[name] = (cardCounts[name] ?: 0) + 1
                    }
                    val reversedCount = detail.drawnCards.count { !it.isUpright }
                    if (reversedCount >= maxOf(1, detail.drawnCards.size / 2)) {
                        inwardSignals += 1
                    } else {
                        outwardSignals += 1
                    }
                }

                ReadingType.ASTROLOGY -> {
                    val detail = runCatching {
                        json.decodeFromString<HoroscopeReadingDetail>(record.detailJson)
                    }.getOrNull() ?: return@forEach
                    when (detail.elementBalance?.dominantElementKey) {
                        "water" -> inwardSignals += 1
                        "fire" -> outwardSignals += 1
                        "earth" -> groundedSignals += 1
                        "air" -> airySignals += 1
                    }
                }

                ReadingType.BAZI -> groundedSignals += 1
            }
        }

        val headline = if (isZh) {
            "最近 ${recentRecords.size} 次记录里，你更像是在反复确认自己当下真正关心什么。"
        } else {
            "Across your last ${recentRecords.size} readings, a clearer pattern is starting to repeat."
        }

        val insights = buildList {
            themeCounts.maxByOrNull { it.value }?.let { themePulse ->
                add(
                    HistoryTrajectoryInsight(
                        title = if (isZh) "最近最常出现的主题" else "Most Repeated Theme",
                        body = if (isZh) {
                            "你最近最常回到「${themePulse.key}」这个命题，说明这不是一次性的好奇，而是这段时间真正牵动你的主轴。"
                        } else {
                            "You keep returning to “${themePulse.key},” which suggests this is not a passing curiosity but a live theme in your life right now."
                        }
                    )
                )
            }

            val repeatedCards = cardCounts
                .filterValues { it >= 2 }
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .take(3)
                .map { it.key }

            if (repeatedCards.isNotEmpty()) {
                add(
                    HistoryTrajectoryInsight(
                        title = if (isZh) "反复出现的牌" else "Cards Echoing Back",
                        body = if (isZh) {
                            "${repeatedCards.joinToString("、")}已经不止一次出现，它们更像是在重复提醒你同一个核心功课。"
                        } else {
                            "${repeatedCards.joinToString(", ")} has shown up more than once, which usually means the same lesson keeps asking for your attention."
                        }
                    )
                )
            }

            add(
                HistoryTrajectoryInsight(
                    title = if (isZh) "最近的情绪天气" else "Recent Emotional Weather",
                    body = emotionalWeatherLine(
                        inwardSignals = inwardSignals,
                        outwardSignals = outwardSignals,
                        groundedSignals = groundedSignals,
                        airySignals = airySignals,
                        isZh = isZh
                    )
                )
            )

            recordTypeCounts.maxByOrNull { it.value }?.key?.let { type ->
                add(
                    HistoryTrajectoryInsight(
                        title = if (isZh) "接下来最适合的仪式" else "Best Next Ritual",
                        body = nextRitualLine(type, isZh)
                    )
                )
            }
        }

        return HistoryTrajectorySnapshot(headline = headline, insights = insights)
    }

    private fun emotionalWeatherLine(
        inwardSignals: Int,
        outwardSignals: Int,
        groundedSignals: Int,
        airySignals: Int,
        isZh: Boolean
    ): String {
        val dominant = listOf(
            "inward" to inwardSignals,
            "outward" to outwardSignals,
            "grounded" to groundedSignals,
            "airy" to airySignals
        ).maxByOrNull { it.second }?.first ?: "grounded"

        return when (dominant) {
            "inward" -> if (isZh) {
                "你最近的记录更偏向向内整理，像是在慢慢收拢情绪、看清自己真正要回应的感受。"
            } else {
                "Your recent readings lean inward, suggesting a period of emotional sorting and quieter self-honesty."
            }
            "outward" -> if (isZh) {
                "最近的能量更偏向向外推进，像是在把犹豫慢慢转成行动，事情会因为你更主动而松动。"
            } else {
                "The recent current leans outward, turning hesitation into movement and asking you to meet life more directly."
            }
            "airy" -> if (isZh) {
                "最近更像是思绪活跃期，很多问题需要先说清楚、看清楚，再决定往哪里走。"
            } else {
                "This looks like a mentally active phase where clarity in language and perspective matters more than speed."
            }
            else -> if (isZh) {
                "最近的主旋律是稳住节奏，把基础、边界和日常秩序慢慢重新摆正。"
            } else {
                "The strongest recent note is about steadiness: rebuilding rhythm, boundaries, and practical structure."
            }
        }
    }

    private fun nextRitualLine(type: ReadingType, isZh: Boolean): String {
        return when (type) {
            ReadingType.TAROT -> if (isZh) {
                "你最近最适合继续用塔罗追踪同一个主题，尤其适合改用不同牌阵去照亮同一件事。"
            } else {
                "Tarot looks like your clearest next ritual right now, especially if you revisit the same question through a different spread."
            }
            ReadingType.ASTROLOGY -> if (isZh) {
                "你最近更适合回到本命盘，把术语翻成人话，看看真正该被你长期经营的关系与方向。"
            } else {
                "A return to your natal chart may be most useful now, especially if you translate the symbols into everyday choices and relationship patterns."
            }
            ReadingType.BAZI -> if (isZh) {
                "你最近更适合看长期节奏和人生结构，慢一点，但会更稳。"
            } else {
                "A slower ritual around long-range structure seems most helpful now, even if it moves more quietly."
            }
        }
    }
}
