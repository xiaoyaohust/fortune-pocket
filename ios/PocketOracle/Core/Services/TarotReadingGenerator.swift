import Foundation

struct TarotReading {
    let theme: TarotQuestionTheme
    let spreadId: String
    let drawnCards: [DrawnCard]
    let positionReadings: [TarotPositionReading]
    let overallEnergy: String
    let focusInsight: String
    let advice: String
    let luckyItemName: String
    let luckyColorName: String
    let luckyColorHex: String
    let luckyNumber: Int
    let spreadName: String
    let spreadDescription: String
    let date: Date
}

enum TarotReadingGenerator {

    static func generate(
        theme: TarotQuestionTheme = .general,
        spreadStyle: TarotSpreadStyle? = nil
    ) throws -> TarotReading {
        let loader = ContentLoader.shared
        let cardsData = try loader.loadTarotCards()
        let spreadsData = try loader.loadSpreads()
        let templatesData = try loadTemplatesData()
        let luckyItemsData = try loadLuckyItemsData()
        let luckyColorsData = try loadLuckyColorsData()
        return try generate(
            cardsData: cardsData,
            spreadsData: spreadsData,
            templatesData: templatesData,
            luckyItemsData: luckyItemsData,
            luckyColorsData: luckyColorsData,
            theme: theme,
            spreadStyle: spreadStyle
        )
    }

    /// User-picked overload: accepts 3 pre-chosen cards + upright states from the interactive deck.
    static func generate(
        pickedCards: [TarotCard],
        uprightStates: [Bool],
        theme: TarotQuestionTheme = .general,
        spreadStyle: TarotSpreadStyle? = nil
    ) throws -> TarotReading {
        let loader = ContentLoader.shared
        let spreadsData = try loader.loadSpreads()
        let templates = try JSONDecoder().decode(ReadingTemplates.self, from: try loadTemplatesData())
        let itemsText = try parseLuckyItems(data: try loadLuckyItemsData())
        let colorsText = try parseLuckyColors(data: try loadLuckyColorsData())
        let isZh = Locale.isZh

        let spreadIdentifier = spreadStyle?.spreadID ?? theme.defaultSpreadStyle.spreadID
        let spread = spreadsData.spreads.first(where: { $0.id == spreadIdentifier })
            ?? spreadsData.spreads.first(where: { $0.id == "three_card" })
            ?? spreadsData.spreads[0]

        let positions = spread.positions.sorted { $0.index < $1.index }
        let drawnCards: [DrawnCard] = positions.enumerated().map { offset, position in
            let card = pickedCards[offset % pickedCards.count]
            let isUpright = offset < uprightStates.count ? uprightStates[offset] : Bool.random()
            return DrawnCard(
                card: card,
                isUpright: isUpright,
                positionLabel: isZh ? position.labelZh : position.labelEn,
                positionAspect: position.aspect
            )
        }
        return buildReading(
            drawnCards: drawnCards, spread: spread, theme: theme,
            templates: templates, itemsText: itemsText, colorsText: colorsText, isZh: isZh
        )
    }

    /// Internal overload that accepts pre-loaded data — used by unit tests.
    static func generate(
        cardsData: TarotCardsData,
        spreadsData: SpreadsData,
        templatesData: Data,
        luckyItemsData: Data,
        luckyColorsData: Data,
        theme: TarotQuestionTheme = .general,
        spreadStyle: TarotSpreadStyle? = nil
    ) throws -> TarotReading {
        let templates = try JSONDecoder().decode(ReadingTemplates.self, from: templatesData)
        let itemsText = try parseLuckyItems(data: luckyItemsData)
        let colorsText = try parseLuckyColors(data: luckyColorsData)
        let isZh = Locale.isZh

        let spreadIdentifier = spreadStyle?.spreadID ?? theme.defaultSpreadStyle.spreadID
        let spread = spreadsData.spreads.first(where: { $0.id == spreadIdentifier })
            ?? spreadsData.spreads.first(where: { $0.id == "three_card" })
            ?? spreadsData.spreads[0]

        let positions = spread.positions.sorted { $0.index < $1.index }
        let shuffledCards = Array(cardsData.cards.shuffled().prefix(spread.cardCount))
        let drawnCards: [DrawnCard] = positions.enumerated().map { offset, position in
            let card = shuffledCards[offset]
            return DrawnCard(
                card: card,
                isUpright: Bool.random(),
                positionLabel: isZh ? position.labelZh : position.labelEn,
                positionAspect: position.aspect
            )
        }
        return buildReading(
            drawnCards: drawnCards, spread: spread, theme: theme,
            templates: templates, itemsText: itemsText, colorsText: colorsText, isZh: isZh
        )
    }

    private static func buildReading(
        drawnCards: [DrawnCard],
        spread: TarotSpread,
        theme: TarotQuestionTheme,
        templates: ReadingTemplates,
        itemsText: [(name: String, energy: String)],
        colorsText: [(name: String, hex: String)],
        isZh: Bool
    ) -> TarotReading {
        let energyKey = assessEnergy(cards: drawnCards)
        let overallEnergy = buildOverall(
            cards: drawnCards, spread: spread, theme: theme,
            energyKey: energyKey, templates: templates, isZh: isZh
        )
        let positionReadings = drawnCards.map { buildPositionReading(card: $0, theme: theme, isZh: isZh) }
        let focusInsight = buildFocusInsight(cards: drawnCards, theme: theme, energyKey: energyKey, isZh: isZh)
        let advice = buildAdvice(card: drawnCards.last ?? drawnCards[0], templates: templates, isZh: isZh)

        let luckyItem = itemsText.randomElement() ?? (isZh ? "水晶" : "Crystal", "")
        let luckyColor = colorsText.randomElement() ?? (isZh ? "金色" : "Gold", "#C9A84C")
        let luckyNumber = (drawnCards.map { $0.card.number }.reduce(0, +) % 9) + 1

        return TarotReading(
            theme: theme,
            spreadId: spread.id,
            drawnCards: drawnCards,
            positionReadings: positionReadings,
            overallEnergy: overallEnergy,
            focusInsight: focusInsight,
            advice: advice,
            luckyItemName: luckyItem.name,
            luckyColorName: luckyColor.name,
            luckyColorHex: luckyColor.hex,
            luckyNumber: luckyNumber,
            spreadName: isZh ? spread.nameZh : spread.nameEn,
            spreadDescription: isZh ? spread.descriptionZh : spread.descriptionEn,
            date: Date()
        )
    }

    static func toDetail(reading: TarotReading) -> TarotReadingDetail {
        TarotReadingDetail(
            spreadId: reading.spreadId,
            themeId: reading.theme.rawValue,
            themeNameZh: [
                .general: "综合主题",
                .love: "关系主题",
                .career: "事业主题",
                .wealth: "财富主题"
            ][reading.theme],
            themeNameEn: [
                .general: "General Focus",
                .love: "Relationship Focus",
                .career: "Career Focus",
                .wealth: "Wealth Focus"
            ][reading.theme],
            spreadDescription: reading.spreadDescription,
            drawnCards: reading.drawnCards.map { drawn in
                DrawnCardDetail(
                    cardId: drawn.card.id,
                    cardNameZh: drawn.card.nameZh,
                    cardNameEn: drawn.card.nameEn,
                    isUpright: drawn.isUpright,
                    positionLabel: drawn.positionLabel,
                    positionAspect: drawn.positionAspect,
                    meaning: drawn.card.localizedMeaning(isUpright: drawn.isUpright)
                )
            },
            positionReadings: reading.positionReadings.map { item in
                TarotPositionReadingDetail(
                    title: item.positionLabel,
                    aspect: item.aspect,
                    body: item.interpretation
                )
            },
            overallEnergy: reading.overallEnergy,
            focusInsight: reading.focusInsight,
            loveReading: nil,
            careerReading: nil,
            wealthReading: nil,
            adviceTip: reading.advice,
            luckyItem: reading.luckyItemName,
            luckyColor: reading.luckyColorName,
            luckyNumber: reading.luckyNumber
        )
    }

    static func assessEnergy(cards: [DrawnCard]) -> String {
        let lights = cards.filter {
            ($0.isUpright && $0.card.energyUpright == "light")
                || (!$0.isUpright && $0.card.energyReversed == "light")
        }.count
        let shadows = cards.filter {
            ($0.isUpright && $0.card.energyUpright == "shadow")
                || (!$0.isUpright && $0.card.energyReversed == "shadow")
        }.count
        if lights >= 2 { return "light" }
        if shadows >= 2 { return "shadow" }
        return "mixed"
    }

    private static func buildOverall(
        cards: [DrawnCard],
        spread: TarotSpread,
        theme: TarotQuestionTheme,
        energyKey: String,
        templates: ReadingTemplates,
        isZh: Bool
    ) -> String {
        let pool = isZh
            ? (energyKey == "light" ? templates.overallLightZh : energyKey == "shadow" ? templates.overallShadowZh : templates.overallMixedZh)
            : (energyKey == "light" ? templates.overallLightEn : energyKey == "shadow" ? templates.overallShadowEn : templates.overallMixedEn)
        let energySentence = pool.randomElement() ?? ""
        let spreadDescription = isZh ? spread.descriptionZh : spread.descriptionEn
        let dominantLine = dominantEnergyLine(cards: cards, theme: theme, isZh: isZh)

        return [energySentence, spreadDescription, dominantLine]
            .filter { !$0.isEmpty }
            .joined(separator: "\n\n")
    }

    private static func buildPositionReading(
        card: DrawnCard,
        theme: TarotQuestionTheme,
        isZh: Bool
    ) -> TarotPositionReading {
        let intro = introText(for: card.positionAspect, theme: theme, isZh: isZh)
        let meaning = card.card.localizedMeaning(isUpright: card.isUpright)
        let keywords = card.card.localizedKeywords(isUpright: card.isUpright)
            .prefix(3)
            .joined(separator: isZh ? "、" : ", ")
        let content = isZh
            ? "\(intro)\n\n\(meaning)\n\n关键词：\(keywords)"
            : "\(intro)\n\n\(meaning)\n\nKeywords: \(keywords)"

        return TarotPositionReading(
            positionLabel: card.positionLabel,
            aspect: card.positionAspect,
            cardNameZh: card.card.nameZh,
            cardNameEn: card.card.nameEn,
            isUpright: card.isUpright,
            interpretation: content
        )
    }

    private static func buildFocusInsight(
        cards: [DrawnCard],
        theme: TarotQuestionTheme,
        energyKey: String,
        isZh: Bool
    ) -> String {
        let anchor = anchorCard(for: theme, cards: cards)
        let anchorName = isZh ? anchor.card.nameZh : anchor.card.nameEn
        let anchorMeaning = firstSentence(of: anchor.card.localizedMeaning(isUpright: anchor.isUpright))
        let suit = dominantSuit(in: cards)
        let suitLine = suitInsight(theme: theme, dominantSuit: suit, energyKey: energyKey, isZh: isZh)
        let anchorLine = isZh
            ? "\(anchorName)是这次最贴近主题的一张牌，它提醒你：\(anchorMeaning)"
            : "\(anchorName) is the card closest to your focus right now, and its message is: \(anchorMeaning)"

        return [suitLine, anchorLine]
            .filter { !$0.isEmpty }
            .joined(separator: "\n\n")
    }

    private static func dominantEnergyLine(cards: [DrawnCard], theme: TarotQuestionTheme, isZh: Bool) -> String {
        let majorCount = cards.filter { $0.card.arcana == "major" }.count
        if majorCount >= 2 {
            return isZh
                ? "两张以上的大牌同时出现，说明这不是一个小小情绪波动，而更像一次需要认真看见的阶段转折。"
                : "With two or more Major Arcana appearing together, this feels less like a passing mood and more like a meaningful turning point."
        }

        switch dominantSuit(in: cards) {
        case "cups":
            return isZh
                ? "这组三牌更偏向情感与关系层面，提醒你先照顾内在感受，再决定如何回应外部变化。"
                : "These cards lean toward emotion and relationship dynamics, asking you to honor inner feeling before reacting outwardly."
        case "wands":
            return isZh
                ? "这组三牌的火元素更强，说明行动力、欲望与推进感会是这次占卜里的主旋律。"
                : "The fire of the spread is strong here, making action, desire, and forward movement the main current of the reading."
        case "swords":
            return isZh
                ? "这组三牌强调认知与沟通，你越愿意说清楚、看清楚，局面就越容易松动。"
                : "The spread highlights thought and communication. The clearer you become, the more the situation can loosen and change."
        case "pentacles":
            return isZh
                ? "这组三牌更关心现实基础，节奏、资源和安全感会比表面情绪更值得优先处理。"
                : "These cards care about practical foundations. Pace, resources, and steadiness deserve more attention than surface emotion."
        default:
            return theme == .general
                ? (isZh ? "这次牌阵没有单一元素压过其他声音，更适合把它当成一面整体观察自己的镜子。" : "No single suit dominates this spread, which makes it better read as a full mirror of your current state.")
                : ""
        }
    }

    private static func introText(for aspect: String, theme: TarotQuestionTheme, isZh: Bool) -> String {
        switch aspect {
        case "past":
            return isZh ? "过去位显示，哪些经历仍在悄悄影响你现在的感受与判断。" : "The past position shows which earlier experiences are still shaping your current responses."
        case "present":
            return isZh ? "现在位揭示，你此刻最需要面对的核心能量正在这里。" : "The present position names the core energy that most needs your attention right now."
        case "next_step":
            return isZh ? "下一步位不强调结果，而提醒你眼下最值得采取的温柔动作。" : "The next-step position focuses less on destiny and more on the gentlest move worth taking now."
        case "self_in_love":
            return isZh ? "这一位描摹你在关系中的姿态，也映照你目前最真实的情感需求。" : "This position reflects how you are showing up in love and what your heart truly needs."
        case "connection":
            return isZh ? "这一位观察关系本身的流动，帮助你看见彼此之间真正发生了什么。" : "This position looks at the relationship current itself and what is really moving between you."
        case "gentle_advice":
            return isZh ? "这一位给的是关系里的温柔建议，不是命令，而是更适合靠近的方式。" : "This position offers gentle relational advice, not a command but a softer way to approach things."
        case "current_path":
            return isZh ? "这一位呈现你当下的工作或学业路径，说明你正走在怎样的节奏里。" : "This position describes your current path in work or study and the rhythm you are already in."
        case "hidden_block":
            return isZh ? "这一位提醒你，真正拖慢推进的，往往不是表面的忙，而是更深一层的阻力。" : "This position points to the hidden friction underneath the obvious busyness."
        case "opportunity":
            return isZh ? "这一位不是空泛好运，而是告诉你最值得回应的机会入口在哪里。" : "This position highlights not vague luck, but the opening that is genuinely worth responding to."
        case "current_flow":
            return isZh ? "这一位显示金钱与资源目前的流向，帮你看清收支和安全感的底色。" : "This position shows the current flow of money and resources, revealing the tone beneath your sense of security."
        case "resource_pattern":
            return isZh ? "这一位映照你一贯使用资源的方式，也暴露了最常见的循环与惯性。" : "This position reflects your deeper pattern around handling resources and the loops you repeat most often."
        case "next_move":
            return isZh ? "这一位提醒你，财富议题最有效的改变，通常来自下一步的小而准的动作。" : "This position suggests that the most effective shift in wealth often comes from one precise next move."
        default:
            return isZh
                ? "\(theme.localizedName)里，这张牌正在给你一面更具体的镜子。"
                : "Within this focus, the card offers a more specific mirror."
        }
    }

    private static func suitInsight(
        theme: TarotQuestionTheme,
        dominantSuit: String,
        energyKey: String,
        isZh: Bool
    ) -> String {
        switch (theme, dominantSuit) {
        case (.love, "cups"):
            return isZh ? "爱情主题里，圣杯占优说明真正关键的不是技巧，而是情绪有没有被认真接住。" : "With Cups leading in a love reading, the real question is whether feelings are being genuinely held and met."
        case (.love, "swords"):
            return isZh ? "爱情主题里，宝剑偏多往往表示需要更诚实的沟通，而不是继续靠猜测维持平静。" : "In a love reading, a strong Swords presence usually asks for clearer honesty instead of keeping the peace through guesswork."
        case (.love, "pentacles"):
            return isZh ? "爱情主题里，星币偏多说明安全感、现实安排与长期承诺比表面浪漫更重要。" : "In a love reading, Pentacles emphasize safety, practical arrangements, and reliable commitment over surface romance."
        case (.love, "wands"):
            return isZh ? "爱情主题里，权杖偏多说明吸引力与主动性都很强，但也要留意热度是否跑在理解前面。" : "In a love reading, Wands bring strong attraction and momentum, but ask whether heat is moving faster than understanding."
        case (.career, "wands"):
            return isZh ? "事业主题里，权杖占优代表行动窗口已经打开，关键在于立刻把想法推进成节奏。" : "In a career reading, Wands suggest the action window is open and ideas now need movement and rhythm."
        case (.career, "swords"):
            return isZh ? "事业主题里，宝剑偏多说明判断、沟通和策略会比蛮干更重要。" : "In a career reading, Swords suggest that discernment, communication, and strategy matter more than brute effort."
        case (.career, "pentacles"):
            return isZh ? "事业主题里，星币偏多说明结果来自稳步积累，系统感与执行力会比灵感更值钱。" : "In a career reading, Pentacles point to steady accumulation, where structure and follow-through outperform raw inspiration."
        case (.career, "cups"):
            return isZh ? "事业主题里，圣杯偏多提醒你，团队关系和内在认同感会直接影响输出质量。" : "In a career reading, Cups show that team dynamics and emotional alignment directly affect performance."
        case (.wealth, "pentacles"):
            return isZh ? "财富主题里，星币占优说明这件事更适合靠结构、预算与长期安排来优化。" : "In a wealth reading, Pentacles suggest the answer lies in structure, budgeting, and longer-range planning."
        case (.wealth, "swords"):
            return isZh ? "财富主题里，宝剑偏多说明先把模糊账目、焦虑决策或错误判断看清楚，钱才会听话。" : "In a wealth reading, Swords ask you to clear the fog around numbers, anxious decisions, or mistaken assumptions."
        case (.wealth, "cups"):
            return isZh ? "财富主题里，圣杯偏多时，金钱往往和情绪、照顾他人或安全感需求绑得很紧。" : "In a wealth reading, Cups often show money tied closely to emotion, care-taking, or the need for reassurance."
        case (.wealth, "wands"):
            return isZh ? "财富主题里，权杖偏多说明你不是没有机会，而是需要更稳地处理冲劲与风险。" : "In a wealth reading, Wands suggest opportunity is present, but momentum and risk need steadier handling."
        default:
            switch energyKey {
            case "light":
                return isZh ? "整体来看，这次的主题能量并不封闭，更多是在邀请你顺势整理并继续向前。" : "Overall, the energy is not closed off; it is inviting you to organize yourself and keep moving."
            case "shadow":
                return isZh ? "整体来看，这次更像一次提醒你慢下来、看清楚再行动的抽牌，而不是催促结果立刻发生。" : "Overall, this feels more like a prompt to slow down and see clearly than a push for instant results."
            default:
                return isZh ? "整体来看，这组牌没有给出单一答案，而是在提醒你接住复杂性，再从中挑出最重要的一条线。" : "Overall, the spread avoids one single answer and instead asks you to hold the complexity before choosing the thread that matters most."
            }
        }
    }

    private static func anchorCard(for theme: TarotQuestionTheme, cards: [DrawnCard]) -> DrawnCard {
        cards.max { lhs, rhs in
            relevance(of: lhs.card, for: theme) < relevance(of: rhs.card, for: theme)
        } ?? cards[0]
    }

    private static func relevance(of card: TarotCard, for theme: TarotQuestionTheme) -> Int {
        let suit = card.semanticSuit
        switch theme {
        case .general:
            return card.arcana == "major" ? 3 : 1
        case .love:
            switch suit {
            case "cups": return 4
            case "pentacles": return 3
            case "wands": return 2
            case "swords": return 1
            default: return card.arcana == "major" ? 3 : 1
            }
        case .career:
            switch suit {
            case "wands": return 4
            case "pentacles": return 3
            case "swords": return 3
            case "cups": return 1
            default: return card.arcana == "major" ? 3 : 1
            }
        case .wealth:
            switch suit {
            case "pentacles": return 4
            case "swords": return 2
            case "wands": return 2
            case "cups": return 1
            default: return card.arcana == "major" ? 3 : 1
            }
        }
    }

    private static func dominantSuit(in cards: [DrawnCard]) -> String {
        let counts = Dictionary(grouping: cards, by: { $0.card.semanticSuit })
            .mapValues(\.count)
        return counts.max { lhs, rhs in lhs.value < rhs.value }?.key ?? "major"
    }

    private static func buildAdvice(
        card: DrawnCard,
        templates: ReadingTemplates,
        isZh: Bool
    ) -> String {
        let pool = isZh ? templates.adviceZh : templates.adviceEn
        var template = pool.randomElement() ?? "{kw1}"
        let keywords = card.card.localizedKeywords(isUpright: card.isUpright)
        let kw1 = keywords.first ?? (isZh ? "觉察" : "awareness")
        let kw2 = keywords.dropFirst().first ?? (isZh ? "平静" : "calm")
        let name = isZh ? card.card.nameZh : card.card.nameEn
        template = template
            .replacingOccurrences(of: "{kw1}", with: kw1)
            .replacingOccurrences(of: "{kw2}", with: kw2)
            .replacingOccurrences(of: "{card_name}", with: name)
        return template
    }

    private static func firstSentence(of text: String) -> String {
        for sep in ["。", ". ", "！", "？"] {
            if let range = text.range(of: sep) {
                return String(text[..<range.upperBound])
            }
        }
        return text
    }

    private static func loadTemplatesData() throws -> Data {
        try ContentLoader.shared.loadRawJSON(named: "data/tarot/reading-templates")
    }

    private typealias LuckyItem = (name: String, energy: String)
    private typealias LuckyColor = (name: String, hex: String)

    private static func loadLuckyItemsData() throws -> Data {
        (try? ContentLoader.shared.loadRawJSON(named: "data/lucky/items")) ?? Data()
    }

    private static func loadLuckyColorsData() throws -> Data {
        (try? ContentLoader.shared.loadRawJSON(named: "data/lucky/colors")) ?? Data()
    }

    private static func parseLuckyItems(data: Data) throws -> [LuckyItem] {
        if data.isEmpty { return [(Locale.isZh ? "水晶" : "Crystal", "")] }
        struct Wrapper: Decodable {
            struct Item: Decodable {
                let name_zh: String
                let name_en: String
                let energy_zh: String
                let energy_en: String
            }
            let items: [Item]
        }
        let wrapper = try JSONDecoder().decode(Wrapper.self, from: data)
        return wrapper.items.map { item in
            let isZh = Locale.isZh
            return (name: isZh ? item.name_zh : item.name_en, energy: isZh ? item.energy_zh : item.energy_en)
        }
    }

    private static func parseLuckyColors(data: Data) throws -> [LuckyColor] {
        if data.isEmpty { return [(Locale.isZh ? "金色" : "Gold", "#C9A84C")] }
        struct Wrapper: Decodable {
            struct ColorItem: Decodable {
                let name_zh: String
                let name_en: String
                let hex: String
            }
            let colors: [ColorItem]
        }
        let wrapper = try JSONDecoder().decode(Wrapper.self, from: data)
        return wrapper.colors.map { color in
            (name: Locale.isZh ? color.name_zh : color.name_en, hex: color.hex)
        }
    }
}

private struct ReadingTemplates: Decodable {
    let overallLightZh: [String]
    let overallShadowZh: [String]
    let overallMixedZh: [String]
    let overallLightEn: [String]
    let overallShadowEn: [String]
    let overallMixedEn: [String]
    let lovePrefixZh: [String]
    let lovePrefixEn: [String]
    let careerPrefixZh: [String]
    let careerPrefixEn: [String]
    let wealthPrefixZh: [String]
    let wealthPrefixEn: [String]
    let adviceZh: [String]
    let adviceEn: [String]

    enum CodingKeys: String, CodingKey {
        case overallLightZh = "overall_light_zh"
        case overallShadowZh = "overall_shadow_zh"
        case overallMixedZh = "overall_mixed_zh"
        case overallLightEn = "overall_light_en"
        case overallShadowEn = "overall_shadow_en"
        case overallMixedEn = "overall_mixed_en"
        case lovePrefixZh = "love_prefix_zh"
        case lovePrefixEn = "love_prefix_en"
        case careerPrefixZh = "career_prefix_zh"
        case careerPrefixEn = "career_prefix_en"
        case wealthPrefixZh = "wealth_prefix_zh"
        case wealthPrefixEn = "wealth_prefix_en"
        case adviceZh = "advice_zh"
        case adviceEn = "advice_en"
    }
}

private extension Locale {
    static var isZh: Bool {
        AppLanguageOption.isChinese
    }
}
