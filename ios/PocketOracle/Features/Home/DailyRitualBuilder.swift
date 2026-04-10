import Foundation

enum DailyRitualBuilder {

    static func build(referenceDate: Date = Date()) throws -> DailyRitual {
        let loader = ContentLoader.shared
        let quotes = try loader.loadDailyQuotes().quotes
        let cards = try loader.loadTarotCards().cards
        let signs = try? loader.loadZodiacSigns().signs

        let seed = stableSeed(for: referenceDate)
        let quote = quotes[safe: seed % quotes.count] ?? quotes[0]
        let tarotCard = cards[safe: seed % cards.count] ?? cards[0]
        let isUpright = ((seed / 3) % 2) == 0
        let tarotMeaning = firstParagraph(of: tarotCard.localizedMeaning(isUpright: isUpright))
        let tarotKeywords = Array(tarotCard.localizedKeywords(isUpright: isUpright).prefix(3))
        let zodiac = savedZodiacSign(from: signs)
        let destination = recommendedDestination(seed: seed, hasBirthday: zodiac != nil)

        return DailyRitual(
            date: referenceDate,
            quote: quote,
            ritualTitle: ritualTitle(for: destination),
            ritualSummary: ritualSummary(
                quote: quote,
                tarotCard: tarotCard,
                isUpright: isUpright,
                zodiac: zodiac
            ),
            destination: destination,
            destinationReason: destinationReason(
                destination: destination,
                zodiac: zodiac,
                tarotCard: tarotCard,
                isUpright: isUpright
            ),
            energyTitle: AppLanguageOption.isChinese ? "今日能量" : "Today's Energy",
            energyBody: energyBody(quote: quote, tarotCard: tarotCard, isUpright: isUpright),
            tarot: DailyTarotHighlight(
                card: tarotCard,
                isUpright: isUpright,
                title: AppLanguageOption.isChinese ? "今日单张塔罗" : "Daily Single Card",
                insight: tarotMeaning,
                keywords: tarotKeywords
            ),
            transit: transitSummary(sign: zodiac, seed: seed),
            promptTitle: AppLanguageOption.isChinese ? "今天可以从哪里开始？" : "Where should today begin?",
            promptBody: promptBody(for: destination)
        )
    }

    private static func savedZodiacSign(from signs: [ZodiacSign]?) -> ZodiacSign? {
        guard let birthday = AppPreferences.savedBirthday,
              let signs else {
            return nil
        }
        let calendar = Calendar.current
        let month = calendar.component(.month, from: birthday)
        let day = calendar.component(.day, from: birthday)
        return ZodiacSign.sign(for: month, day: day, in: signs)
    }

    private static func ritualTitle(for destination: DailyRitualDestination) -> String {
        if AppLanguageOption.isChinese {
            switch destination {
            case .tarot:
                return "今晚适合先让一张牌替你开场"
            case .astrology:
                return "今晚更适合先看看星盘给你的提醒"
            case .bazi:
                return "今晚适合先慢一点，整理自己的能量感"
            }
        }

        switch destination {
        case .tarot:
            return "Tonight begins best with a single card"
        case .astrology:
            return "Tonight is better for reading the sky first"
        case .bazi:
            return "Tonight asks for a slower read of your energy"
        }
    }

    private static func ritualSummary(
        quote: DailyQuote,
        tarotCard: TarotCard,
        isUpright: Bool,
        zodiac: ZodiacSign?
    ) -> String {
        if AppLanguageOption.isChinese {
            let cardLine = "今日牌面落在\(tarotCard.nameZh)\(isUpright ? "正位" : "逆位")，主题偏向\(tarotCard.localizedKeywords(isUpright: isUpright).first ?? "觉察")."
            if let zodiac {
                return "\(cardLine) 你的太阳星座是\(zodiac.nameZh)，今天更适合从感受与节奏入手，而不是一口气把所有问题都解决。"
            }
            return "\(cardLine) 今天先从一件最想确认的小事开始，会比一次做很多更有仪式感。"
        }

        let cardLine = "Today's card is \(tarotCard.nameEn) \(isUpright ? "upright" : "reversed"), leaning toward \(tarotCard.localizedKeywords(isUpright: isUpright).first ?? "awareness")."
        if let zodiac {
            return "\(cardLine) With your Sun in \(zodiac.nameEn), it is better to start from feeling and pacing instead of trying to solve everything at once."
        }
        return "\(cardLine) Start with one question that matters most and let the rest unfold afterward."
    }

    private static func destinationReason(
        destination: DailyRitualDestination,
        zodiac: ZodiacSign?,
        tarotCard: TarotCard,
        isUpright: Bool
    ) -> String {
        if AppLanguageOption.isChinese {
            switch destination {
            case .tarot:
                return "牌面的第一关键词是“\(tarotCard.localizedKeywords(isUpright: isUpright).first ?? "觉察")”，很适合先抽一张牌，让情绪先落地。"
            case .astrology:
                if let zodiac {
                    return "你保存了生日，今天很适合先看\(zodiac.nameZh)的节奏提醒，再决定把注意力放在哪一块。"
                }
                return "今天更适合先确认自己的节奏与边界，再进入更深入的解读。"
            case .bazi:
                return "今天的能量更适合整理结构与节奏，八字页会比直接追答案更适合慢慢看。"
            }
        }

        switch destination {
        case .tarot:
            return "The first keyword on today's card is “\(tarotCard.localizedKeywords(isUpright: isUpright).first ?? "awareness")”, so tarot is the gentlest place to begin."
        case .astrology:
            if let zodiac {
                return "You already saved a birthday, so today's transit-style note can start from your \(zodiac.nameEn) rhythm."
            }
            return "Today's pace is better for checking your rhythm first before going deeper."
        case .bazi:
            return "Today's tone is more about structure and pacing, so Bazi makes a steadier doorway."
        }
    }

    private static func energyBody(quote: DailyQuote, tarotCard: TarotCard, isUpright: Bool) -> String {
        let keyword = tarotCard.localizedKeywords(isUpright: isUpright).first ?? ""
        if AppLanguageOption.isChinese {
            return "首页这句「\(quote.localizedText)」和\(tarotCard.nameZh)\(isUpright ? "正位" : "逆位")的\(keyword)气质放在一起看，今天更适合先感受、再判断。"
        }
        return "Set today's quote “\(quote.localizedText)” beside \(tarotCard.nameEn) \(isUpright ? "upright" : "reversed") and its \(keyword) tone. Feeling first will work better than rushing to conclusions."
    }

    private static func transitSummary(sign: ZodiacSign?, seed: Int) -> DailyTransitHighlight {
        guard let sign else {
            return DailyTransitHighlight(
                title: AppLanguageOption.isChinese ? "今日行运摘要" : "Today's Transit Note",
                summary: AppLanguageOption.isChinese
                    ? "保存生日后，这里会出现更贴近你的今日星象摘要。现在先把这块当成一个温柔提醒：今天更适合留一点空间给情绪和直觉。"
                    : "Once you save your birthday, this section will turn into a more personal transit-style note. For now, treat it as a gentle reminder to leave room for feeling and intuition today.",
                signName: nil,
                needsProfileCompletion: true
            )
        }

        let actionPoolZh = [
            "把注意力收回到真正重要的那一件事上",
            "用更柔软的方式回应外界",
            "先整理感受，再推进计划",
            "先确认边界，再表达善意"
        ]
        let actionPoolEn = [
            "bring your attention back to the one thing that truly matters",
            "respond with more softness than force",
            "sort your feelings before pushing the plan",
            "define your boundaries before offering more energy"
        ]
        let focus = AppLanguageOption.isChinese
            ? actionPoolZh[seed % actionPoolZh.count]
            : actionPoolEn[seed % actionPoolEn.count]
        let title = AppLanguageOption.isChinese ? "今日行运摘要" : "Today's Transit Note"
        let signName = sign.localizedName

        if AppLanguageOption.isChinese {
            let quality = sign.localizedTraits.first ?? sign.nameZh
            return DailyTransitHighlight(
                title: title,
                summary: "以\(signName)的节奏来看，今天更容易把焦点带回\(quality)。适合\(focus)，别急着给所有关系和任务同时下结论。",
                signName: signName,
                needsProfileCompletion: false
            )
        }

        let quality = sign.localizedTraits.first ?? sign.nameEn
        return DailyTransitHighlight(
            title: title,
            summary: "From a \(signName) rhythm, today pulls focus back toward \(quality). It is better to \(focus) than to force every relationship and task into clarity all at once.",
            signName: signName,
            needsProfileCompletion: false
        )
    }

    private static func promptBody(for destination: DailyRitualDestination) -> String {
        if AppLanguageOption.isChinese {
            switch destination {
            case .tarot:
                return "先抽一张牌，确认你最在意的问题，再决定要不要进入完整三牌阵。"
            case .astrology:
                return "先看今天的星象摘要，再决定要不要进入完整本命盘与相位解读。"
            case .bazi:
                return "先整理今天的节奏与能量，再进入更完整的命盘与结构解读。"
            }
        }

        switch destination {
        case .tarot:
            return "Start with one card, name the question that matters most, then decide whether you want the full spread."
        case .astrology:
            return "Begin with today's sky note, then step into the fuller chart if you want more structure."
        case .bazi:
            return "Start by checking today's pacing and energy, then move into the deeper chart when you want more context."
        }
    }

    private static func recommendedDestination(seed: Int, hasBirthday: Bool) -> DailyRitualDestination {
        if hasBirthday {
            switch seed % 3 {
            case 0: return .tarot
            case 1: return .astrology
            default: return .bazi
            }
        }
        return seed.isMultiple(of: 2) ? .tarot : .bazi
    }

    private static func firstParagraph(of text: String) -> String {
        for separator in ["\n\n", "。", ". ", "！", "？"] {
            if let range = text.range(of: separator) {
                return String(text[..<range.upperBound]).trimmingCharacters(in: .whitespacesAndNewlines)
            }
        }
        return text
    }

    private static func stableSeed(for date: Date) -> Int {
        let year = Calendar.current.component(.year, from: date)
        return (year * 1000) + date.dayOfYear
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        guard indices.contains(index) else { return nil }
        return self[index]
    }
}
