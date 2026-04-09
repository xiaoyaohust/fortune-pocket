import Foundation

/// Loads shared-content JSON files from the app bundle.
///
/// In Xcode: add the `shared-content/data` folder as a **Folder Reference**
/// (blue folder icon, not yellow group). This copies the directory structure
/// into the bundle, preserving subdirectory paths.
///
/// Access path example: "data/tarot/cards.json" →
/// Bundle.main.url(forResource: "data/tarot/cards", withExtension: "json")
/// Or use the directory-based approach below.
final class ContentLoader {

    static let shared = ContentLoader()
    private init() {}

    // MARK: - In-memory cache
    private var tarotCardsCache: TarotCardsData?
    private var spreadsCache: SpreadsData?
    private var zodiacSignsCache: ZodiacSignsData?
    private var stemsCache: StemsData?
    private var branchesCache: BranchesData?
    private var baziTemplatesCache: BaziPersonalityTemplates?
    private var dailyQuotesCache: DailyQuotesData?

    // MARK: - Public loaders

    func loadTarotCards() throws -> TarotCardsData {
        if let cached = tarotCardsCache { return cached }
        let data = try loadJSON("data/tarot/cards")
        let result = try JSONDecoder().decode(TarotCardsData.self, from: data)
        tarotCardsCache = result
        return result
    }

    func loadSpreads() throws -> SpreadsData {
        if let cached = spreadsCache { return cached }
        let data = try loadJSON("data/tarot/spreads")
        let result = try JSONDecoder().decode(SpreadsData.self, from: data)
        spreadsCache = result
        return result
    }

    func loadZodiacSigns() throws -> ZodiacSignsData {
        if let cached = zodiacSignsCache { return cached }
        let data = try loadJSON("data/astrology/signs")
        let result = try JSONDecoder().decode(ZodiacSignsData.self, from: data)
        zodiacSignsCache = result
        return result
    }

    func loadHeavenlyStems() throws -> StemsData {
        if let cached = stemsCache { return cached }
        let data = try loadJSON("data/bazi/stems")
        let result = try JSONDecoder().decode(StemsData.self, from: data)
        stemsCache = result
        return result
    }

    func loadEarthlyBranches() throws -> BranchesData {
        if let cached = branchesCache { return cached }
        let data = try loadJSON("data/bazi/branches")
        let result = try JSONDecoder().decode(BranchesData.self, from: data)
        branchesCache = result
        return result
    }

    func loadBaziTemplates() throws -> BaziPersonalityTemplates {
        if let cached = baziTemplatesCache { return cached }
        let data = try loadJSON("data/bazi/personality-templates")
        let result = try JSONDecoder().decode(BaziPersonalityTemplates.self, from: data)
        baziTemplatesCache = result
        return result
    }

    func loadDailyQuotes() throws -> DailyQuotesData {
        if let cached = dailyQuotesCache { return cached }
        let data = try loadJSON("data/quotes/daily-quotes")
        let result = try JSONDecoder().decode(DailyQuotesData.self, from: data)
        dailyQuotesCache = result
        return result
    }

    func loadRawJSON(named name: String) throws -> Data {
        try loadJSON(name)
    }

    // MARK: - Private

    private func loadJSON(_ name: String) throws -> Data {
        // Try flat bundle lookup first (works with Folder Reference in Xcode)
        if let url = Bundle.main.url(forResource: name, withExtension: "json") {
            return try Data(contentsOf: url)
        }
        // Fallback: path-based lookup
        let components = name.split(separator: "/").map(String.init)
        guard let last = components.last else {
            throw ContentLoaderError.fileNotFound(name)
        }
        let subdir = components.dropLast().joined(separator: "/")
        if let url = Bundle.main.url(forResource: last, withExtension: "json", subdirectory: subdir) {
            return try Data(contentsOf: url)
        }
        // Fallback for Xcode builds that flatten the resource tree into the app root.
        if let url = Bundle.main.url(forResource: last, withExtension: "json") {
            return try Data(contentsOf: url)
        }
        throw ContentLoaderError.fileNotFound(name)
    }
}

enum ContentLoaderError: LocalizedError {
    case fileNotFound(String)

    var errorDescription: String? {
        switch self {
        case .fileNotFound(let name):
            return "Content file not found in bundle: \(name).json"
        }
    }
}
