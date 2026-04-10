import Foundation

// MARK: - JSON-decodable models (mirrors shared-content/data/tarot/cards.json)

struct TarotCard: Codable, Identifiable {
    let id: String
    let number: Int
    let arcana: String
    let suit: String?
    let rank: String?
    let nameZh: String
    let nameEn: String
    let symbol: String
    let colorPrimary: String
    let colorAccent: String
    let keywordsUprightZh: [String]
    let keywordsUprightEn: [String]
    let keywordsReversedZh: [String]
    let keywordsReversedEn: [String]
    let meaningUprightZh: String
    let meaningUprightEn: String
    let meaningReversedZh: String
    let meaningReversedEn: String
    let energyUpright: String
    let energyReversed: String

    enum CodingKeys: String, CodingKey {
        case id, number, arcana, suit, rank, symbol
        case nameZh = "name_zh"
        case nameEn = "name_en"
        case colorPrimary = "color_primary"
        case colorAccent = "color_accent"
        case keywordsUprightZh = "keywords_upright_zh"
        case keywordsUprightEn = "keywords_upright_en"
        case keywordsReversedZh = "keywords_reversed_zh"
        case keywordsReversedEn = "keywords_reversed_en"
        case meaningUprightZh = "meaning_upright_zh"
        case meaningUprightEn = "meaning_upright_en"
        case meaningReversedZh = "meaning_reversed_zh"
        case meaningReversedEn = "meaning_reversed_en"
        case energyUpright = "energy_upright"
        case energyReversed = "energy_reversed"
    }

    // Locale-aware helpers
    var isZh: Bool { AppLanguageOption.isChinese }

    var localizedName: String { isZh ? nameZh : nameEn }

    func localizedMeaning(isUpright: Bool) -> String {
        switch (isUpright, isZh) {
        case (true, true):   return meaningUprightZh
        case (true, false):  return meaningUprightEn
        case (false, true):  return meaningReversedZh
        case (false, false): return meaningReversedEn
        }
    }

    func localizedKeywords(isUpright: Bool) -> [String] {
        switch (isUpright, isZh) {
        case (true, true):   return keywordsUprightZh
        case (true, false):  return keywordsUprightEn
        case (false, true):  return keywordsReversedZh
        case (false, false): return keywordsReversedEn
        }
    }

    var semanticSuit: String {
        suit ?? arcana
    }
}

struct TarotCardsData: Codable {
    let version: String
    let total: Int
    let cards: [TarotCard]
}

struct TarotSpread: Codable, Identifiable {
    let id: String
    let nameZh: String
    let nameEn: String
    let descriptionZh: String
    let descriptionEn: String
    let cardCount: Int
    let positions: [SpreadPosition]

    enum CodingKeys: String, CodingKey {
        case id
        case nameZh = "name_zh"
        case nameEn = "name_en"
        case descriptionZh = "description_zh"
        case descriptionEn = "description_en"
        case cardCount = "card_count"
        case positions
    }
}

struct SpreadPosition: Codable {
    let index: Int
    let labelZh: String
    let labelEn: String
    let aspect: String

    enum CodingKeys: String, CodingKey {
        case index, aspect
        case labelZh = "label_zh"
        case labelEn = "label_en"
    }
}

struct SpreadsData: Codable {
    let version: String
    let spreads: [TarotSpread]
}

// A drawn card with orientation
struct DrawnCard: Identifiable {
    let id = UUID()
    let card: TarotCard
    let isUpright: Bool
    let positionLabel: String
    let positionAspect: String
}

enum TarotQuestionTheme: String, Codable, CaseIterable, Identifiable {
    case general
    case love
    case career
    case wealth

    var id: String { rawValue }

    private var isZh: Bool { AppLanguageOption.isChinese }

    var defaultSpreadStyle: TarotSpreadStyle {
        switch self {
        case .general: return .pathSpread
        case .love: return .relationshipSpread
        case .career: return .careerSpread
        case .wealth: return .wealthSpread
        }
    }

    var availableSpreadStyles: [TarotSpreadStyle] {
        TarotSpreadStyle.available(for: self)
    }

    var localizedName: String {
        switch self {
        case .general: return isZh ? "综合主题" : "General Focus"
        case .love: return isZh ? "关系主题" : "Relationship Focus"
        case .career: return isZh ? "事业主题" : "Career Focus"
        case .wealth: return isZh ? "财富主题" : "Wealth Focus"
        }
    }

    var localizedEntryTitle: String {
        switch self {
        case .general: return isZh ? "今晚适合先看整体脉络" : "Tonight is best for reading the whole path"
        case .love: return isZh ? "今晚适合看关系里的真实流动" : "Tonight is best for reading relationship currents"
        case .career: return isZh ? "今晚适合看事业节奏与机会" : "Tonight is best for reading career momentum"
        case .wealth: return isZh ? "今晚适合看资源与金钱节奏" : "Tonight is best for reading money and resources"
        }
    }

    var localizedEntrySubtitle: String {
        switch self {
        case .general: return isZh ? "先抽一张得到方向，或用三张牌看清整段过程。" : "Start with a single card for direction, or use three cards to see the whole arc."
        case .love: return isZh ? "适合关系、暧昧、靠近与疏离，也适合照见自己在关系中的位置。" : "For romance, attachment, distance, and the role you are playing in a connection."
        case .career: return isZh ? "适合工作推进、学业压力、卡点和机会窗口。" : "For work momentum, study pressure, friction, and the opening ahead."
        case .wealth: return isZh ? "适合看资源流动、消费冲动和下一步更稳的选择。" : "For resource flow, spending impulses, and the steadier next move."
        }
    }

    var localizedPromptDescription: String {
        switch self {
        case .general:
            return isZh
                ? "适合想看看整体处境、情绪走向和下一步方向的时候。"
                : "Best when you want a gentle read on your current path and next step."
        case .love:
            return isZh
                ? "围绕关系状态、情感互动和你此刻最需要的相处方式。"
                : "Focus on relationship dynamics, emotional flow, and the gentlest way to respond."
        case .career:
            return isZh
                ? "围绕学业、工作推进、卡点与机会入口来展开解读。"
                : "Focus on study or work momentum, hidden friction, and the clearest opening ahead."
        case .wealth:
            return isZh
                ? "围绕金钱流动、资源使用方式与更稳妥的下一步。"
                : "Focus on money flow, resource patterns, and the steadier next move."
        }
    }

    var localizedFocusTitle: String {
        switch self {
        case .general: return isZh ? "牌阵总览" : "Spread Synthesis"
        case .love: return isZh ? "关系聚焦" : "Relationship Focus"
        case .career: return isZh ? "事业聚焦" : "Career Focus"
        case .wealth: return isZh ? "财富聚焦" : "Wealth Focus"
        }
    }
}

enum TarotSpreadStyle: String, Codable, CaseIterable, Identifiable {
    case singleCard = "single_card"
    case pathSpread = "three_card"
    case relationshipSpread = "love_three_card"
    case careerSpread = "career_three_card"
    case wealthSpread = "wealth_three_card"
    case mindBodySpirit = "mind_body_spirit"

    var id: String { rawValue }

    var spreadID: String { rawValue }

    var cardCount: Int {
        switch self {
        case .singleCard: return 1
        case .pathSpread, .relationshipSpread, .careerSpread, .wealthSpread, .mindBodySpirit: return 3
        }
    }

    static func available(for theme: TarotQuestionTheme) -> [TarotSpreadStyle] {
        switch theme {
        case .general:
            return [.singleCard, .pathSpread, .mindBodySpirit]
        case .love:
            return [.singleCard, .relationshipSpread]
        case .career:
            return [.singleCard, .careerSpread]
        case .wealth:
            return [.singleCard, .wealthSpread]
        }
    }

    func localizedName(isZh: Bool = AppLanguageOption.isChinese) -> String {
        switch self {
        case .singleCard: return isZh ? "单张指引" : "Single Card"
        case .pathSpread: return isZh ? "路径三牌阵" : "Path Spread"
        case .relationshipSpread: return isZh ? "关系镜像阵" : "Relationship Mirror"
        case .careerSpread: return isZh ? "事业路径阵" : "Career Path"
        case .wealthSpread: return isZh ? "财富流动阵" : "Wealth Flow"
        case .mindBodySpirit: return isZh ? "身心灵三牌阵" : "Mind Body Spirit"
        }
    }

    func localizedSubtitle(isZh: Bool = AppLanguageOption.isChinese) -> String {
        switch self {
        case .singleCard:
            return isZh ? "一张牌，给今晚一个清晰方向" : "One card for tonight's clearest direction"
        case .pathSpread:
            return isZh ? "过去 · 现在 · 下一步" : "Past · Present · Next Step"
        case .relationshipSpread:
            return isZh ? "我在关系里 · 关系流动 · 温柔建议" : "My Place · Connection · Gentle Advice"
        case .careerSpread:
            return isZh ? "当前路径 · 隐藏阻力 · 机会入口" : "Current Path · Hidden Block · Opportunity"
        case .wealthSpread:
            return isZh ? "当前流动 · 资源模式 · 下一步动作" : "Current Flow · Resource Pattern · Next Move"
        case .mindBodySpirit:
            return isZh ? "身体 · 心绪 · 灵魂讯号" : "Body · Mind · Spirit"
        }
    }

    func localizedDescription(isZh: Bool = AppLanguageOption.isChinese) -> String {
        switch self {
        case .singleCard:
            return isZh
                ? "适合当下只想要一个方向感、不想被太多信息淹没的时候。"
                : "Best when you want one clear direction without being overwhelmed."
        case .pathSpread:
            return isZh
                ? "适合梳理你一路走来的情绪脉络、现状位置和下一步动作。"
                : "Best for understanding the arc from what shaped you to what comes next."
        case .relationshipSpread:
            return isZh
                ? "适合关系主题，帮助你同时看见自己、关系流动和最温柔的回应方式。"
                : "Best for relationship questions, showing you, the connection, and the gentlest response."
        case .careerSpread:
            return isZh
                ? "适合工作与学业，帮你看清正在发生的节奏、阻力和机会入口。"
                : "Best for work and study, revealing momentum, friction, and the opening ahead."
        case .wealthSpread:
            return isZh
                ? "适合资源与金钱问题，看见流向、惯性和更稳的下一步。"
                : "Best for money and resources, showing flow, habits, and a steadier next move."
        case .mindBodySpirit:
            return isZh
                ? "适合想听见更深层状态时，看看身体、心绪与灵魂当前分别在说什么。"
                : "Best when you want a deeper scan of what body, mind, and spirit are each saying."
        }
    }
}

struct TarotPositionReading: Codable, Identifiable {
    let positionLabel: String
    let aspect: String
    let cardNameZh: String
    let cardNameEn: String
    let isUpright: Bool
    let interpretation: String

    var id: String {
        "\(aspect)-\(cardNameZh)-\(isUpright)"
    }
}
