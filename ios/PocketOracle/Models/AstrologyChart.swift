import Foundation

enum AstrologyPlanetID: String, Codable, CaseIterable, Identifiable {
    case sun
    case moon
    case mercury
    case venus
    case mars
    case jupiter
    case saturn
    case uranus
    case neptune
    case pluto

    var id: String { rawValue }

    var localizedName: String {
        switch self {
        case .sun: return AppLanguageOption.isChinese ? "太阳" : "Sun"
        case .moon: return AppLanguageOption.isChinese ? "月亮" : "Moon"
        case .mercury: return AppLanguageOption.isChinese ? "水星" : "Mercury"
        case .venus: return AppLanguageOption.isChinese ? "金星" : "Venus"
        case .mars: return AppLanguageOption.isChinese ? "火星" : "Mars"
        case .jupiter: return AppLanguageOption.isChinese ? "木星" : "Jupiter"
        case .saturn: return AppLanguageOption.isChinese ? "土星" : "Saturn"
        case .uranus: return AppLanguageOption.isChinese ? "天王星" : "Uranus"
        case .neptune: return AppLanguageOption.isChinese ? "海王星" : "Neptune"
        case .pluto: return AppLanguageOption.isChinese ? "冥王星" : "Pluto"
        }
    }
}

enum AstrologyAspectType: String, Codable, CaseIterable, Identifiable {
    case conjunction
    case sextile
    case square
    case trine
    case opposition

    var id: String { rawValue }

    var angle: Double {
        switch self {
        case .conjunction: return 0
        case .sextile: return 60
        case .square: return 90
        case .trine: return 120
        case .opposition: return 180
        }
    }

    var orb: Double {
        switch self {
        case .conjunction: return 8
        case .sextile: return 4.5
        case .square: return 6
        case .trine: return 6
        case .opposition: return 8
        }
    }

    var localizedName: String {
        switch self {
        case .conjunction: return AppLanguageOption.isChinese ? "合相" : "Conjunction"
        case .sextile: return AppLanguageOption.isChinese ? "六合" : "Sextile"
        case .square: return AppLanguageOption.isChinese ? "刑克" : "Square"
        case .trine: return AppLanguageOption.isChinese ? "拱相" : "Trine"
        case .opposition: return AppLanguageOption.isChinese ? "冲相" : "Opposition"
        }
    }
}

struct AstrologyPlanetPlacement: Codable, Identifiable {
    let planetId: AstrologyPlanetID
    let longitude: Double
    let signId: String
    let signNameZh: String
    let signNameEn: String
    let house: Int?
    let isRetrograde: Bool

    var id: String { planetId.rawValue }

    var localizedPlanetName: String { planetId.localizedName }
    var localizedSignName: String { AppLanguageOption.isChinese ? signNameZh : signNameEn }
    var degreeInSign: Double { longitude.truncatingRemainder(dividingBy: 30) }
}

struct AstrologyAspect: Codable, Identifiable {
    let firstPlanetId: AstrologyPlanetID
    let secondPlanetId: AstrologyPlanetID
    let type: AstrologyAspectType
    let orbDegrees: Double

    var id: String { "\(firstPlanetId.rawValue)-\(secondPlanetId.rawValue)-\(type.rawValue)" }
}

struct AstrologyElementBalance: Codable {
    let fire: Int
    let earth: Int
    let air: Int
    let water: Int

    var dominantElementKey: String {
        [
            ("fire", fire),
            ("earth", earth),
            ("air", air),
            ("water", water)
        ]
        .max { lhs, rhs in
            if lhs.1 == rhs.1 {
                return lhs.0 > rhs.0
            }
            return lhs.1 < rhs.1
        }?
        .0 ?? "fire"
    }

    var localizedDominantElement: String {
        switch dominantElementKey {
        case "fire": return AppLanguageOption.isChinese ? "火象" : "Fire"
        case "earth": return AppLanguageOption.isChinese ? "土象" : "Earth"
        case "air": return AppLanguageOption.isChinese ? "风象" : "Air"
        case "water": return AppLanguageOption.isChinese ? "水象" : "Water"
        default: return AppLanguageOption.isChinese ? "火象" : "Fire"
        }
    }
}

struct AstrologyHouseFocus: Codable, Identifiable {
    let house: Int
    let titleZh: String
    let titleEn: String
    let summaryZh: String
    let summaryEn: String

    var id: Int { house }
    var localizedTitle: String { AppLanguageOption.isChinese ? titleZh : titleEn }
    var localizedSummary: String { AppLanguageOption.isChinese ? summaryZh : summaryEn }
}

struct HoroscopeReading {
    let sign: ZodiacSign
    let moonSign: ZodiacSign
    let risingSign: ZodiacSign?
    let birthDateText: String
    let birthTimeText: String
    let birthCityName: String
    let timeZoneId: String
    let chartSummary: String
    let chartSignature: String
    let personalityCore: String
    let relationshipPattern: String
    let strengths: String
    let growthEdge: String
    let currentTheme: String
    let overall: String
    let love: String
    let career: String
    let wealth: String
    let social: String
    let advice: String
    let luckyColor: String
    let luckyNumber: Int
    let planetPlacements: [AstrologyPlanetPlacement]
    let majorAspects: [AstrologyAspect]
    let elementBalance: AstrologyElementBalance
    let houseFocus: [AstrologyHouseFocus]
}
