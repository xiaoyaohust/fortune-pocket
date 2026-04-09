import Foundation

struct HeavenlyStem: Codable, Identifiable {
    let id: String
    let index: Int
    let nameZh: String
    let nameEn: String
    let element: String
    let polarity: String
    let imageZh: String
    let imageEn: String
    let colorPrimary: String
    let colorAccent: String
    let personalityZh: String
    let personalityEn: String
    let loveZh: String
    let loveEn: String
    let careerZh: String
    let careerEn: String
    let wealthZh: String
    let wealthEn: String

    enum CodingKeys: String, CodingKey {
        case id, index, element, polarity
        case nameZh = "name_zh"
        case nameEn = "name_en"
        case imageZh = "image_zh"
        case imageEn = "image_en"
        case colorPrimary = "color_primary"
        case colorAccent = "color_accent"
        case personalityZh = "personality_zh"
        case personalityEn = "personality_en"
        case loveZh = "love_zh"
        case loveEn = "love_en"
        case careerZh = "career_zh"
        case careerEn = "career_en"
        case wealthZh = "wealth_zh"
        case wealthEn = "wealth_en"
    }

    var isZh: Bool { AppLanguageOption.isChinese }
    var localizedPersonality: String { isZh ? personalityZh : personalityEn }
    var localizedLove: String { isZh ? loveZh : loveEn }
    var localizedCareer: String { isZh ? careerZh : careerEn }
    var localizedWealth: String { isZh ? wealthZh : wealthEn }
}

struct StemsData: Codable {
    let version: String
    let stems: [HeavenlyStem]
}

struct EarthlyBranch: Codable, Identifiable {
    let id: String
    let index: Int
    let nameZh: String
    let nameEn: String
    let animalZh: String
    let animalEn: String
    let element: String
    let colorPrimary: String
    let colorAccent: String
    let month: Int
    let nuanceZh: String
    let nuanceEn: String

    enum CodingKeys: String, CodingKey {
        case id, index, element, month
        case nameZh = "name_zh"
        case nameEn = "name_en"
        case animalZh = "animal_zh"
        case animalEn = "animal_en"
        case colorPrimary = "color_primary"
        case colorAccent = "color_accent"
        case nuanceZh = "nuance_zh"
        case nuanceEn = "nuance_en"
    }
}

struct BranchesData: Codable {
    let version: String
    let branches: [EarthlyBranch]
}

struct BaziPersonalityTemplates: Codable {
    let version: String
    let disclaimerZh: String
    let disclaimerEn: String
    let phaseAdviceZh: [String]
    let phaseAdviceEn: [String]
    let overallIntroZh: [String]
    let overallIntroEn: [String]
    let elementPersonality: ElementPersonality

    enum CodingKeys: String, CodingKey {
        case version
        case disclaimerZh = "disclaimer_zh"
        case disclaimerEn = "disclaimer_en"
        case phaseAdviceZh = "phase_advice_zh"
        case phaseAdviceEn = "phase_advice_en"
        case overallIntroZh = "overall_intro_zh"
        case overallIntroEn = "overall_intro_en"
        case elementPersonality = "element_personality"
    }
}

struct ElementPersonality: Codable {
    let woodZh: String; let woodEn: String
    let fireZh: String; let fireEn: String
    let earthZh: String; let earthEn: String
    let metalZh: String; let metalEn: String
    let waterZh: String; let waterEn: String

    enum CodingKeys: String, CodingKey {
        case woodZh = "wood_zh"; case woodEn = "wood_en"
        case fireZh = "fire_zh"; case fireEn = "fire_en"
        case earthZh = "earth_zh"; case earthEn = "earth_en"
        case metalZh = "metal_zh"; case metalEn = "metal_en"
        case waterZh = "water_zh"; case waterEn = "water_en"
    }

    func description(element: String, isZh: Bool) -> String {
        switch element {
        case "wood":  return isZh ? woodZh : woodEn
        case "fire":  return isZh ? fireZh : fireEn
        case "earth": return isZh ? earthZh : earthEn
        case "metal": return isZh ? metalZh : metalEn
        case "water": return isZh ? waterZh : waterEn
        default:      return isZh ? woodZh : woodEn
        }
    }
}

struct DailyQuote: Codable, Identifiable {
    let id: String
    let textZh: String
    let textEn: String
    let category: String

    enum CodingKeys: String, CodingKey {
        case id, category
        case textZh = "text_zh"
        case textEn = "text_en"
    }

    var localizedText: String {
        AppLanguageOption.isChinese ? textZh : textEn
    }
}

struct DailyQuotesData: Codable {
    let version: String
    let quotes: [DailyQuote]
}

// In-memory result of a Bazi reading (not persisted directly)
struct BaziReading {
    let birthDate:          Date
    let birthHour:          Int?       // 0–23, nil if not specified
    let stem:               HeavenlyStem
    let disclaimer:         String
    let overallPersonality: String
    let love:               String
    let career:             String
    let wealth:             String
    let phaseAdvice:        String
    let elementDescription: String
}
