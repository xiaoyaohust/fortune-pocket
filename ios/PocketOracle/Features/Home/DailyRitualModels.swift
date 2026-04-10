import Foundation

enum DailyRitualDestination: String {
    case tarot
    case astrology
    case bazi

    var icon: String {
        switch self {
        case .tarot: return "moon.stars.fill"
        case .astrology: return "sparkles"
        case .bazi: return "leaf.fill"
        }
    }

    var localizedTitle: String {
        switch self {
        case .tarot:
            return AppLanguageOption.isChinese ? "抽一张今日塔罗" : "Draw Today's Tarot"
        case .astrology:
            return AppLanguageOption.isChinese ? "看看今日行运" : "Read Today's Transit"
        case .bazi:
            return AppLanguageOption.isChinese ? "整理今日能量" : "Review Today's Energy"
        }
    }
}

struct DailyTarotHighlight {
    let card: TarotCard
    let isUpright: Bool
    let title: String
    let insight: String
    let keywords: [String]
}

struct DailyTransitHighlight {
    let title: String
    let summary: String
    let signName: String?
    let needsProfileCompletion: Bool
}

struct DailyRitual {
    let date: Date
    let quote: DailyQuote
    let ritualTitle: String
    let ritualSummary: String
    let destination: DailyRitualDestination
    let destinationReason: String
    let energyTitle: String
    let energyBody: String
    let tarot: DailyTarotHighlight
    let transit: DailyTransitHighlight
    let promptTitle: String
    let promptBody: String
}
