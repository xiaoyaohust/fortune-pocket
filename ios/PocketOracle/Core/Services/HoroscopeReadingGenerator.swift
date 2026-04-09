import Foundation

enum HoroscopeReadingGenerator {

    static func generate(input: AstrologyInput) throws -> HoroscopeReading {
        let signs = try ContentLoader.shared.loadZodiacSigns().signs
        let chart = try AstrologyChartCalculator.calculate(input: input, signs: signs)
        let isZh = AppLanguageOption.isChinese

        let venus = placement(.venus, in: chart.planetPlacements)
        let mars = placement(.mars, in: chart.planetPlacements)
        let mercury = placement(.mercury, in: chart.planetPlacements)
        let jupiter = placement(.jupiter, in: chart.planetPlacements)
        let saturn = placement(.saturn, in: chart.planetPlacements)

        let dominantElement = chart.elementBalance.localizedDominantElement
        let leadingHouse = chart.houseFocus.first
        let emotionalAspect = aspect(involving: [.moon, .venus], from: chart.majorAspects)
        let careerAspect = aspect(involving: [.sun, .saturn, .mars, .jupiter], from: chart.majorAspects)
        let socialAspect = aspect(involving: [.mercury, .moon], from: chart.majorAspects)

        let chartSignature = isZh
            ? "太阳\(chart.sunSign.nameZh) · 月亮\(chart.moonSign.nameZh) · 上升\(chart.risingSign.nameZh)"
            : "Sun \(chart.sunSign.nameEn) · Moon \(chart.moonSign.nameEn) · Rising \(chart.risingSign.nameEn)"
        let chartSummary = buildChartSummary(chart: chart)

        let overall = overallText(
            chart: chart,
            chartSummary: chartSummary,
            dominantElement: dominantElement,
            leadingHouse: leadingHouse
        )
        let love = loveText(
            chart: chart,
            venus: venus,
            mars: mars,
            emotionalAspect: emotionalAspect
        )
        let career = careerText(
            chart: chart,
            jupiter: jupiter,
            saturn: saturn,
            careerAspect: careerAspect
        )
        let wealth = wealthText(
            chart: chart,
            venus: venus,
            jupiter: jupiter,
            saturn: saturn
        )
        let social = socialText(
            chart: chart,
            mercury: mercury,
            socialAspect: socialAspect
        )
        let advice = adviceText(chart: chart, leadingHouse: leadingHouse)

        let luckyColor = luckyColor(chart: chart)
        let luckyNumber = ((chart.sunSign.index + chart.moonSign.index + chart.risingSign.index) % 9) + 1

        return HoroscopeReading(
            sign: chart.sunSign,
            moonSign: chart.moonSign,
            risingSign: chart.risingSign,
            birthDateText: chart.birthDateText,
            birthTimeText: chart.birthTimeText,
            birthCityName: chart.birthCityName,
            timeZoneId: chart.timeZoneId,
            chartSummary: chartSummary,
            chartSignature: chartSignature,
            overall: overall,
            love: love,
            career: career,
            wealth: wealth,
            social: social,
            advice: advice,
            luckyColor: luckyColor,
            luckyNumber: luckyNumber,
            planetPlacements: chart.planetPlacements,
            majorAspects: chart.majorAspects,
            elementBalance: chart.elementBalance,
            houseFocus: chart.houseFocus
        )
    }

    private static func buildChartSummary(chart: AstrologyChartSnapshot) -> String {
        if AppLanguageOption.isChinese {
            return "这张盘以\(chart.sunSign.nameZh)的核心意志、\(chart.moonSign.nameZh)的情绪底色，以及\(chart.risingSign.nameZh)的外在气场为主轴。"
        }
        return "This chart is anchored by the Sun in \(chart.sunSign.nameEn), the Moon in \(chart.moonSign.nameEn), and the Rising sign in \(chart.risingSign.nameEn)."
    }

    private static func overallText(
        chart: AstrologyChartSnapshot,
        chartSummary: String,
        dominantElement: String,
        leadingHouse: AstrologyHouseFocus?
    ) -> String {
        if AppLanguageOption.isChinese {
            return "\(chartSummary) 主导能量偏向\(dominantElement)，你通常会以\(chart.risingSign.localizedTraits.first ?? chart.risingSign.nameZh)的方式走进世界，也会用\(chart.moonSign.localizedTraits.first ?? chart.moonSign.nameZh)去消化感受。\(leadingHouse?.localizedSummary ?? "")"
        }
        return "\(chartSummary) The chart leans \(dominantElement.lowercased()) overall, so you often enter the world through a \(chart.risingSign.localizedTraits.first ?? chart.risingSign.nameEn) tone and process feelings in a \(chart.moonSign.localizedTraits.first ?? chart.moonSign.nameEn) way. \(leadingHouse?.localizedSummary ?? "")"
    }

    private static func loveText(
        chart: AstrologyChartSnapshot,
        venus: AstrologyPlanetPlacement,
        mars: AstrologyPlanetPlacement,
        emotionalAspect: AstrologyAspect?
    ) -> String {
        let houseText = houseLine(for: [5, 7, 8], in: chart.houseFocus)
        if AppLanguageOption.isChinese {
            let aspectText = emotionalAspect.map { readableAspect($0) } ?? "你的情感表达更受月亮与金星的位置牵引。"
            return "感情层面，金星落在\(venus.localizedSignName)，说明你在关系里重视\(signValueFocus(venus.signId, isZh: true))；火星落在\(mars.localizedSignName)，也让你在靠近他人时带着\(signActionFocus(mars.signId, isZh: true))。\(aspectText) \(houseText)"
        }
        let aspectText = emotionalAspect.map { readableAspect($0) } ?? "Your emotional style is strongly shaped by the Moon and Venus placements."
        return "In love, Venus in \(venus.localizedSignName) shows that you value \(signValueFocus(venus.signId, isZh: false)), while Mars in \(mars.localizedSignName) adds \(signActionFocus(mars.signId, isZh: false)) to the way you pursue closeness. \(aspectText) \(houseText)"
    }

    private static func careerText(
        chart: AstrologyChartSnapshot,
        jupiter: AstrologyPlanetPlacement,
        saturn: AstrologyPlanetPlacement,
        careerAspect: AstrologyAspect?
    ) -> String {
        let houseText = houseLine(for: [6, 10, 11], in: chart.houseFocus)
        if AppLanguageOption.isChinese {
            let aspectText = careerAspect.map { readableAspect($0) } ?? "事业节奏更多由太阳、火星、木星与土星的组合来决定。"
            return "事业与学业上，太阳落在\(chart.sunSign.localizedName)，让你在长期方向上追求\(signDriveFocus(chart.sunSign.id, isZh: true))；木星在\(jupiter.localizedSignName)、土星在\(saturn.localizedSignName)，提示你扩张与稳住的方式并不相同。\(aspectText) \(houseText)"
        }
        let aspectText = careerAspect.map { readableAspect($0) } ?? "Career rhythms are largely shaped by how the Sun, Mars, Jupiter, and Saturn combine."
        return "For career and study, the Sun in \(chart.sunSign.localizedName) points toward \(signDriveFocus(chart.sunSign.id, isZh: false)), while Jupiter in \(jupiter.localizedSignName) and Saturn in \(saturn.localizedSignName) show that growth and discipline do not move in exactly the same way. \(aspectText) \(houseText)"
    }

    private static func wealthText(
        chart: AstrologyChartSnapshot,
        venus: AstrologyPlanetPlacement,
        jupiter: AstrologyPlanetPlacement,
        saturn: AstrologyPlanetPlacement
    ) -> String {
        let houseText = houseLine(for: [2, 8, 10], in: chart.houseFocus)
        if AppLanguageOption.isChinese {
            return "财务观更像是一种长期习惯，而不是单次运气。金星在\(venus.localizedSignName)、木星在\(jupiter.localizedSignName)，说明你会倾向通过\(signValueFocus(venus.signId, isZh: true))来建立安全感；土星在\(saturn.localizedSignName)则提醒你，真正的稳定来自持续结构而不是一时热度。\(houseText)"
        }
        return "Money patterns in this chart are more about long-term habits than sudden luck. Venus in \(venus.localizedSignName) and Jupiter in \(jupiter.localizedSignName) suggest you build security through \(signValueFocus(venus.signId, isZh: false)), while Saturn in \(saturn.localizedSignName) reminds you that stability comes from structure rather than short bursts of intensity. \(houseText)"
    }

    private static func socialText(
        chart: AstrologyChartSnapshot,
        mercury: AstrologyPlanetPlacement,
        socialAspect: AstrologyAspect?
    ) -> String {
        let houseText = houseLine(for: [3, 7, 11], in: chart.houseFocus)
        if AppLanguageOption.isChinese {
            let aspectText = socialAspect.map { readableAspect($0) } ?? "你的沟通气质主要从水星与月亮的组合里流露出来。"
            return "社交互动里，水星落在\(mercury.localizedSignName)，说明你表达时偏向\(signCommunicationFocus(mercury.signId, isZh: true))；月亮在\(chart.moonSign.localizedName)也让你特别在意关系里的情绪气候。\(aspectText) \(houseText)"
        }
        let aspectText = socialAspect.map { readableAspect($0) } ?? "Your communication style is mainly revealed through the blend of Mercury and the Moon."
        return "Socially, Mercury in \(mercury.localizedSignName) suggests you communicate through \(signCommunicationFocus(mercury.signId, isZh: false)), while the Moon in \(chart.moonSign.localizedName) makes you especially sensitive to the emotional climate between people. \(aspectText) \(houseText)"
    }

    private static func adviceText(
        chart: AstrologyChartSnapshot,
        leadingHouse: AstrologyHouseFocus?
    ) -> String {
        if AppLanguageOption.isChinese {
            if chart.majorAspects.contains(where: { $0.type == .square || $0.type == .opposition }) {
                return "你的星盘里存在几组推动成长的紧张相位，这并不意味着人生更难，而是说明你更适合在拉扯中提炼自己的节奏。建议你优先照顾\(leadingHouse?.localizedTitle ?? "当下最常被你忽略的主题")，先把节奏稳住，再做决定。"
            }
            return "这张盘的主要相位整体偏顺畅，说明你的很多力量可以自然流动。建议你把精力投入到\(leadingHouse?.localizedTitle ?? "真正重要的主题")，让已经有优势的地方持续发光。"
        }
        if chart.majorAspects.contains(where: { $0.type == .square || $0.type == .opposition }) {
            return "This chart contains a few tension-building aspects. That does not mean life is harder by default; it means growth often comes through learning how to regulate contrast. Start with \(leadingHouse?.localizedTitle ?? "the area that keeps asking for care"), steady your pace there, and let decisions come afterward."
        }
        return "The major aspects here flow more naturally overall, so many of your strengths can move without excessive friction. Put your energy into \(leadingHouse?.localizedTitle ?? "the themes that truly matter"), and let your strongest patterns work for you."
    }

    private static func readableAspect(_ aspect: AstrologyAspect) -> String {
        let first = aspect.firstPlanetId.localizedName
        let second = aspect.secondPlanetId.localizedName
        let aspectName = aspect.type.localizedName
        if AppLanguageOption.isChinese {
            return "\(first)与\(second)形成\(aspectName)，这会让相关主题更容易被你明确感受到。"
        }
        return "\(first) forms a \(aspectName.lowercased()) with \(second), making that dynamic easier to feel consciously."
    }

    private static func houseLine(for houses: [Int], in focus: [AstrologyHouseFocus]) -> String {
        guard let matched = focus.first(where: { houses.contains($0.house) }) else { return "" }
        return matched.localizedSummary
    }

    private static func aspect(
        involving planets: Set<AstrologyPlanetID>,
        from aspects: [AstrologyAspect]
    ) -> AstrologyAspect? {
        aspects.first {
            planets.contains($0.firstPlanetId) || planets.contains($0.secondPlanetId)
        }
    }

    private static func placement(
        _ id: AstrologyPlanetID,
        in placements: [AstrologyPlanetPlacement]
    ) -> AstrologyPlanetPlacement {
        placements.first(where: { $0.planetId == id }) ?? placements[0]
    }

    private static func signValueFocus(_ signId: String, isZh: Bool) -> String {
        switch signId {
        case "taurus", "virgo", "capricorn":
            return isZh ? "稳定、具体、可落地的回应" : "stability, tangible gestures, and grounded consistency"
        case "gemini", "libra", "aquarius":
            return isZh ? "交流、共识与精神层面的同步" : "conversation, shared perspective, and mental resonance"
        case "cancer", "scorpio", "pisces":
            return isZh ? "情绪上的理解、深度与安全感" : "emotional understanding, depth, and a sense of safety"
        default:
            return isZh ? "热度、主动性与清晰的回应" : "warmth, initiative, and clear signals"
        }
    }

    private static func signActionFocus(_ signId: String, isZh: Bool) -> String {
        switch signId {
        case "aries", "leo", "sagittarius":
            return isZh ? "直接而热烈的推进感" : "direct, fiery momentum"
        case "taurus", "virgo", "capricorn":
            return isZh ? "慢热但持续的行动方式" : "a slower but sustained style of action"
        case "gemini", "libra", "aquarius":
            return isZh ? "先沟通、再靠近的节奏" : "a communicate-first rhythm"
        default:
            return isZh ? "情绪带动下的靠近方式" : "an emotionally led approach to closeness"
        }
    }

    private static func signDriveFocus(_ signId: String, isZh: Bool) -> String {
        switch signId {
        case "aries", "capricorn", "leo":
            return isZh ? "自己开路、承担主导角色" : "initiating, leading, and setting direction"
        case "virgo", "taurus":
            return isZh ? "把复杂的事一步步做实" : "turning complexity into practical progress"
        case "gemini", "aquarius", "libra":
            return isZh ? "连接信息、人脉与更大的视角" : "connecting ideas, people, and broader perspective"
        default:
            return isZh ? "把情绪、愿景与长期意义融进工作里" : "weaving emotion, vision, and meaning into your work"
        }
    }

    private static func signCommunicationFocus(_ signId: String, isZh: Bool) -> String {
        switch signId {
        case "gemini", "libra", "aquarius":
            return isZh ? "轻盈、机敏、带有思辨感" : "lightness, agility, and an intellectual tone"
        case "taurus", "virgo", "capricorn":
            return isZh ? "具体、谨慎、注重细节" : "clarity, caution, and practical detail"
        case "cancer", "scorpio", "pisces":
            return isZh ? "感受先行、语气更具共情力" : "emotion-first language with strong empathy"
        default:
            return isZh ? "直接表达、热情推进" : "direct expression and energetic momentum"
        }
    }

    private static func luckyColor(chart: AstrologyChartSnapshot) -> String {
        switch chart.elementBalance.dominantElementKey {
        case "fire":
            return AppLanguageOption.isChinese ? "金红色" : "Golden Crimson"
        case "earth":
            return AppLanguageOption.isChinese ? "苔绿色" : "Moss Green"
        case "air":
            return AppLanguageOption.isChinese ? "雾蓝色" : "Mist Blue"
        case "water":
            return AppLanguageOption.isChinese ? "月银色" : "Moon Silver"
        default:
            return chart.sunSign.localizedLuckyColor
        }
    }
}
