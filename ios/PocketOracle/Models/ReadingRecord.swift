import Foundation
import SwiftData

// MARK: - SwiftData persistent model

@Model
final class ReadingRecord {
    var id: UUID
    var typeRaw: String           // ReadingType.rawValue
    var createdAt: Date
    var title: String
    var summary: String
    var detailJSON: String        // Full result as JSON string
    var schemaVersion: Int
    var isPremium: Bool           // Reserved for future subscription features

    init(
        type: ReadingType,
        title: String,
        summary: String,
        detailJSON: String
    ) {
        self.id = UUID()
        self.typeRaw = type.rawValue
        self.createdAt = Date()
        self.title = title
        self.summary = summary
        self.detailJSON = detailJSON
        self.schemaVersion = 1
        self.isPremium = false
    }

    var type: ReadingType {
        ReadingType(rawValue: typeRaw) ?? .tarot
    }
}

// MARK: - Reading type

enum ReadingType: String, Codable, CaseIterable {
    case tarot = "tarot"
    case astrology = "astrology"
    case bazi = "bazi"

    var localizedName: String {
        switch self {
        case .tarot:     return String.appLocalized("reading_type_tarot")
        case .astrology: return String.appLocalized("reading_type_astrology")
        case .bazi:      return String.appLocalized("reading_type_bazi")
        }
    }

    var icon: String {
        switch self {
        case .tarot:     return "🃏"
        case .astrology: return "✨"
        case .bazi:      return "☯️"
        }
    }
}

// MARK: - Tarot reading detail (serialized into detailJSON)

struct TarotReadingDetail: Codable {
    let spreadId: String
    let themeId: String?
    let themeNameZh: String?
    let themeNameEn: String?
    let spreadDescription: String?
    let drawnCards: [DrawnCardDetail]
    let positionReadings: [TarotPositionReadingDetail]?
    let overallEnergy: String
    let focusInsight: String?
    let loveReading: String?
    let careerReading: String?
    let wealthReading: String?
    let adviceTip: String
    let luckyItem: String
    let luckyColor: String?
    let luckyNumber: Int?
}

struct DrawnCardDetail: Codable {
    let cardId: String
    let cardNameZh: String
    let cardNameEn: String
    let isUpright: Bool
    let positionLabel: String
    let positionAspect: String?
    let meaning: String
}

struct TarotPositionReadingDetail: Codable {
    let title: String
    let aspect: String
    let body: String
}

// MARK: - Horoscope reading detail

struct HoroscopeReadingDetail: Codable {
    let signId: String
    let signNameZh: String
    let signNameEn: String
    let date: String
    let birthTime: String?
    let birthCityName: String?
    let timeZoneId: String?
    let moonSignId: String?
    let moonSignNameZh: String?
    let moonSignNameEn: String?
    let risingSignId: String?
    let risingSignNameZh: String?
    let risingSignNameEn: String?
    let chartSignature: String?
    let chartSummary: String?
    let personalityCore: String?
    let relationshipPattern: String?
    let strengths: String?
    let growthEdge: String?
    let currentTheme: String?
    let overall: String
    let love: String
    let career: String
    let wealth: String
    let social: String
    let advice: String
    let luckyColor: String
    let luckyNumber: Int
    let planetPlacements: [AstrologyPlanetPlacement]?
    let majorAspects: [AstrologyAspect]?
    let elementBalance: AstrologyElementBalance?
    let houseFocus: [AstrologyHouseFocus]?
}

// MARK: - Bazi reading detail (legacy entertainment-text format)

struct BaziReadingDetail: Codable {
    let birthDate: String
    let birthHour: String?
    let dayStemId: String
    let disclaimer: String
    let overallPersonality: String
    let loveTendency: String
    let careerTendency: String
    let wealthTendency: String
    let phaseAdvice: String
    let elementDescription: String
}

// MARK: - Bazi chart snapshot (new engine format)

struct BaziChartSnapshot: Codable {
    let year: String
    let month: String
    let day: String
    let hour: String?
    let dayMasterElement: String
    let cycleDirection: String
    let startingAge: Int
}
