package com.fortunepocket.feature.home

import com.fortunepocket.core.content.ContentLoader
import com.fortunepocket.core.model.TarotCard
import com.fortunepocket.core.model.ZodiacSign
import java.time.Instant
import java.time.ZoneId

object DailyRitualBuilder {

    fun build(
        contentLoader: ContentLoader,
        savedBirthdayMillis: Long,
        referenceMillis: Long = System.currentTimeMillis()
    ): DailyRitual {
        val quotes = contentLoader.loadDailyQuotes().quotes
        val cards = contentLoader.loadTarotCards().cards
        val signs = contentLoader.loadZodiacSigns().signs

        val now = Instant.ofEpochMilli(referenceMillis).atZone(ZoneId.systemDefault())
        val dayOfYear = now.dayOfYear
        val quote = quotes[positive(dayOfYear - 1, quotes.size)]
        val card = cards[positive(dayOfYear * 7 + 11, cards.size)]
        val isUpright = dayOfYear % 4 != 0
        val sign = inferSign(savedBirthdayMillis, signs)
        val hasBirthday = sign != null
        val destination = recommendedDestination(dayOfYear, hasBirthday)

        return DailyRitual(
            generatedAtMillis = referenceMillis,
            ritualTitle = ritualTitle(destination),
            ritualSummary = ritualSummary(destination, sign, dayOfYear),
            destination = destination,
            destinationReason = destinationReason(destination, sign),
            energyTitle = RitualCopy(
                zh = "今日能量",
                en = "Today's Energy"
            ),
            energyBody = energyBody(quote, sign, dayOfYear),
            tarot = DailyTarotHighlight(
                title = RitualCopy(
                    zh = "今日单张塔罗",
                    en = "Today's Tarot Card"
                ),
                card = card,
                isUpright = isUpright,
                insight = tarotInsight(card, isUpright)
            ),
            transit = DailyTransitHighlight(
                title = RitualCopy(
                    zh = "今日行运摘要",
                    en = "Today's Transit Note"
                ),
                sign = sign,
                summary = transitSummary(sign, dayOfYear),
                needsBirthday = !hasBirthday
            ),
            promptTitle = RitualCopy(
                zh = "今晚可以先做什么",
                en = "What To Do First Tonight"
            ),
            promptBody = promptBody(destination),
            quote = quote
        )
    }

    private fun ritualTitle(destination: DailyRitualDestination): RitualCopy = when (destination) {
        DailyRitualDestination.TAROT -> RitualCopy(
            zh = "今晚先翻开一张牌，让答案自己靠近你。",
            en = "Start with a card tonight and let the answer move toward you."
        )
        DailyRitualDestination.ASTROLOGY -> RitualCopy(
            zh = "今晚适合回到你的星盘，看清内在节奏。",
            en = "Tonight is better for returning to your chart and reading your inner rhythm."
        )
        DailyRitualDestination.BAZI -> RitualCopy(
            zh = "今晚更适合慢一点，看你的底层能量如何发声。",
            en = "Tonight asks for a slower pace so your deeper energy can speak."
        )
    }

    private fun ritualSummary(
        destination: DailyRitualDestination,
        sign: ZodiacSign?,
        seed: Int
    ): RitualCopy {
        val signZh = sign?.nameZh ?: "你"
        val signEn = sign?.nameEn ?: "you"
        return when (destination) {
            DailyRitualDestination.TAROT -> RitualCopy(
                zh = "今天更适合先听直觉，再行动。先抽一张牌，会比直接找结论更容易看见真正的卡点。",
                en = "Today favors intuition before action. A card pull will reveal your real friction more gently than forcing an answer."
            )
            DailyRitualDestination.ASTROLOGY -> RitualCopy(
                zh = if (sign != null)
                    "对${signZh}来说，今天更像是回看自己的一天。先看本命盘，会比追着外界节奏跑更有收获。"
                else
                    "如果你愿意补充生日，今天很适合先看一眼本命盘，把注意力带回自己。",
                en = if (sign != null)
                    "For $signEn, today feels more reflective than reactive. Start with your chart before chasing the pace outside you."
                else
                    "If you add your birthday, today is ideal for starting with your natal chart and returning attention to yourself."
            )
            DailyRitualDestination.BAZI -> RitualCopy(
                zh = if (seed % 2 == 0)
                    "今天的课题更偏“稳定自己”，不是马上冲出去。先看八字，会更清楚什么在真正消耗你。"
                else
                    "今天更像一个整理底层节奏的窗口。先看八字，会比盲目推进更有效率。",
                en = if (seed % 2 == 0)
                    "Today's lesson is more about stabilizing yourself than rushing outward. A Bazi reading can show what is truly draining you."
                else
                    "Today feels like a window for reorganizing your deeper rhythm. A Bazi reading will be more useful than blind momentum."
            )
        }
    }

    private fun destinationReason(destination: DailyRitualDestination, sign: ZodiacSign?): RitualCopy {
        return when (destination) {
            DailyRitualDestination.TAROT -> RitualCopy(
                zh = "你的问题更像需要一个当下切口，而不是更多分析。",
                en = "Your question needs a clean opening right now, not more analysis."
            )
            DailyRitualDestination.ASTROLOGY -> RitualCopy(
                zh = if (sign != null)
                    "今天的重点在你如何理解自己，而不是如何立刻回应世界。"
                else
                    "补充生日后，本命盘会更适合帮你看清今天的主轴。",
                en = if (sign != null)
                    "The emphasis today is on understanding yourself before responding to the world."
                else
                    "Once your birthday is saved, the natal chart will become the clearest lens for today's focus."
            )
            DailyRitualDestination.BAZI -> RitualCopy(
                zh = "今天适合先看结构与节奏，再决定下一步要不要推进。",
                en = "Today is better for reading structure and rhythm before deciding how hard to push."
            )
        }
    }

    private fun energyBody(quote: com.fortunepocket.core.model.DailyQuote, sign: ZodiacSign?, seed: Int): RitualCopy {
        val quoteZh = quote.textZh
        val quoteEn = quote.textEn
        val signToneZh = sign?.traitsZh?.firstOrNull()?.let { "你的${it}会是今天的钥匙。" } ?: "先照顾自己的心，再去处理外部世界。"
        val signToneEn = sign?.traitsEn?.firstOrNull()?.let { "Your $it nature is the key today." } ?: "Care for your inner weather before handling the outside world."
        return RitualCopy(
            zh = if (seed % 2 == 0) "$quoteZh $signToneZh" else "$signToneZh $quoteZh",
            en = if (seed % 2 == 0) "$quoteEn $signToneEn" else "$signToneEn $quoteEn"
        )
    }

    private fun tarotInsight(card: TarotCard, isUpright: Boolean): RitualCopy {
        val zhKeywords = card.localizedKeywords(isUpright = isUpright, isZh = true)
            .take(2)
            .joinToString("、")
        val enKeywords = card.localizedKeywords(isUpright = isUpright, isZh = false)
            .take(2)
            .joinToString(" and ")
        return RitualCopy(
            zh = "这张牌今天带来的讯号更接近“$zhKeywords”。先读懂情绪，再决定动作，会更顺。",
            en = "This card leans toward $enKeywords today. Read the feeling first, then decide on action."
        )
    }

    private fun transitSummary(sign: ZodiacSign?, seed: Int): RitualCopy {
        if (sign == null) {
            return RitualCopy(
                zh = "补充生日后，这里会根据你的太阳星座生成更贴近你的今日节奏摘要。",
                en = "Add your birthday and this note will become more personal to your daily rhythm."
            )
        }

        val elementZh = when (sign.element) {
            "fire" -> "火象"
            "earth" -> "土象"
            "air" -> "风象"
            else -> "水象"
        }
        val elementEn = sign.element.replaceFirstChar { it.uppercase() }
        val toneZh = when (seed % 3) {
            0 -> "适合收拢注意力，把力气放回最重要的一件事。"
            1 -> "适合先确认感受，再决定要不要回应他人。"
            else -> "适合整理边界，给真正重要的人和事留出空间。"
        }
        val toneEn = when (seed % 3) {
            0 -> "Concentrate your attention and return your energy to the one thing that matters most."
            1 -> "Name your feeling first, then decide whether you really need to respond."
            else -> "Tidy your boundaries and save your best energy for what truly matters."
        }
        return RitualCopy(
            zh = "${sign.nameZh}今天更像在走一段${elementZh}节奏。$toneZh",
            en = "${sign.nameEn} is moving through a more $elementEn-led rhythm today. $toneEn"
        )
    }

    private fun promptBody(destination: DailyRitualDestination): RitualCopy = when (destination) {
        DailyRitualDestination.TAROT -> RitualCopy(
            zh = "如果你只做一件事，就先抽牌。把问题缩成一句最想知道的话，答案会更清晰。",
            en = "If you do only one thing, draw a card. Reduce your question to a single sentence and the answer will sharpen."
        )
        DailyRitualDestination.ASTROLOGY -> RitualCopy(
            zh = "如果你只做一件事，就先看本命盘。重点不在预测，而在看清你今天最自然的发力方式。",
            en = "If you do only one thing, open the natal chart. The point is not prediction but understanding how you move best today."
        )
        DailyRitualDestination.BAZI -> RitualCopy(
            zh = "如果你只做一件事，就先看八字。先看结构，再谈行动，你会更稳。",
            en = "If you do only one thing, start with Bazi. Read the structure before deciding on action."
        )
    }

    private fun recommendedDestination(seed: Int, hasBirthday: Boolean): DailyRitualDestination {
        val rolling = seed % 3
        return when {
            !hasBirthday && rolling == 1 -> DailyRitualDestination.TAROT
            !hasBirthday -> DailyRitualDestination.BAZI
            rolling == 0 -> DailyRitualDestination.TAROT
            rolling == 1 -> DailyRitualDestination.ASTROLOGY
            else -> DailyRitualDestination.BAZI
        }
    }

    private fun inferSign(birthdayMillis: Long, signs: List<ZodiacSign>): ZodiacSign? {
        if (birthdayMillis <= 0L) return null
        val date = Instant.ofEpochMilli(birthdayMillis).atZone(ZoneId.systemDefault())
        return signs.firstOrNull { sign ->
            val range = sign.dateRange
            val startValue = range.startMonth * 100 + range.startDay
            val endValue = range.endMonth * 100 + range.endDay
            val currentValue = date.monthValue * 100 + date.dayOfMonth
            if (startValue <= endValue) {
                currentValue in startValue..endValue
            } else {
                currentValue >= startValue || currentValue <= endValue
            }
        }
    }

    private fun positive(value: Int, modulus: Int): Int {
        if (modulus == 0) return 0
        return ((value % modulus) + modulus) % modulus
    }
}
