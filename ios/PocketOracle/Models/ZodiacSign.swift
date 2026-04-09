import Foundation

struct ZodiacSign: Codable, Identifiable {
    let id: String
    let index: Int
    let nameZh: String
    let nameEn: String
    let symbol: String
    let element: String
    let quality: String
    let rulingPlanetZh: String
    let rulingPlanetEn: String
    let dateRange: DateRange
    let colorPrimary: String
    let colorAccent: String
    let traitsZh: [String]
    let traitsEn: [String]
    let strengthsZh: [String]
    let strengthsEn: [String]
    let weaknessesZh: [String]
    let weaknessesEn: [String]
    let descriptionZh: String
    let descriptionEn: String
    let luckyColorZh: String
    let luckyColorEn: String
    let luckyNumber: Int

    enum CodingKeys: String, CodingKey {
        case id, index, symbol, element, quality
        case nameZh = "name_zh"
        case nameEn = "name_en"
        case rulingPlanetZh = "ruling_planet_zh"
        case rulingPlanetEn = "ruling_planet_en"
        case dateRange = "date_range"
        case colorPrimary = "color_primary"
        case colorAccent = "color_accent"
        case traitsZh = "traits_zh"
        case traitsEn = "traits_en"
        case strengthsZh = "strengths_zh"
        case strengthsEn = "strengths_en"
        case weaknessesZh = "weaknesses_zh"
        case weaknessesEn = "weaknesses_en"
        case descriptionZh = "description_zh"
        case descriptionEn = "description_en"
        case luckyColorZh = "lucky_color_zh"
        case luckyColorEn = "lucky_color_en"
        case luckyNumber = "lucky_number"
    }

    var isZh: Bool { AppLanguageOption.isChinese }
    var localizedName: String { isZh ? nameZh : nameEn }
    var localizedDescription: String { isZh ? descriptionZh : descriptionEn }
    var localizedTraits: [String] { isZh ? traitsZh : traitsEn }
    var localizedLuckyColor: String { isZh ? luckyColorZh : luckyColorEn }

    /// Returns the sign for a given birth month and day. Returns nil if no match.
    static func sign(for month: Int, day: Int, in signs: [ZodiacSign]) -> ZodiacSign? {
        signs.first { sign in
            let r = sign.dateRange
            if r.startMonth == r.endMonth {
                return month == r.startMonth && day >= r.startDay && day <= r.endDay
            } else if month == r.startMonth {
                return day >= r.startDay
            } else if month == r.endMonth {
                return day <= r.endDay
            }
            return false
        }
    }
}

struct DateRange: Codable {
    let startMonth: Int
    let startDay: Int
    let endMonth: Int
    let endDay: Int

    enum CodingKeys: String, CodingKey {
        case startMonth = "start_month"
        case startDay = "start_day"
        case endMonth = "end_month"
        case endDay = "end_day"
    }
}

struct ZodiacSignsData: Codable {
    let version: String
    let signs: [ZodiacSign]
}
