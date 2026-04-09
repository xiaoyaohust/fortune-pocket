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

    var spreadID: String {
        switch self {
        case .general: return "three_card"
        case .love: return "love_three_card"
        case .career: return "career_three_card"
        case .wealth: return "wealth_three_card"
        }
    }

    private var isZh: Bool { AppLanguageOption.isChinese }

    var localizedName: String {
        switch self {
        case .general: return isZh ? "综合主题" : "General Focus"
        case .love: return isZh ? "爱情主题" : "Love Focus"
        case .career: return isZh ? "事业主题" : "Career Focus"
        case .wealth: return isZh ? "财富主题" : "Wealth Focus"
        }
    }

    var localizedSpreadName: String {
        switch self {
        case .general: return isZh ? "路径三牌阵" : "Path Spread"
        case .love: return isZh ? "爱情镜像阵" : "Love Mirror Spread"
        case .career: return isZh ? "事业路径阵" : "Career Path Spread"
        case .wealth: return isZh ? "财富流动阵" : "Wealth Flow Spread"
        }
    }

    var localizedSpreadSubtitle: String {
        switch self {
        case .general: return isZh ? "过去 · 现在 · 下一步" : "Past · Present · Next Step"
        case .love: return isZh ? "我在关系里 · 关系流动 · 温柔建议" : "My Place in Love · Connection Energy · Gentle Advice"
        case .career: return isZh ? "当前路径 · 隐藏阻力 · 机会入口" : "Current Path · Hidden Block · Opportunity"
        case .wealth: return isZh ? "当前流动 · 资源模式 · 下一步动作" : "Current Flow · Resource Pattern · Next Move"
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
        case .love: return isZh ? "爱情聚焦" : "Love Focus"
        case .career: return isZh ? "事业聚焦" : "Career Focus"
        case .wealth: return isZh ? "财富聚焦" : "Wealth Focus"
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
