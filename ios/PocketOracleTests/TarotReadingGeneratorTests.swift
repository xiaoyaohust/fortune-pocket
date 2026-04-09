import XCTest
@testable import PocketOracle

final class TarotReadingGeneratorTests: XCTestCase {

    private var cardsData: TarotCardsData!
    private var spreadsData: SpreadsData!
    private var templatesData: Data!
    private var luckyItemsData: Data!
    private var luckyColorsData: Data!

    private var testBundle: Bundle { Bundle(for: Self.self) }
    private var candidateBundles: [Bundle] { [testBundle, .main] }

    override func setUpWithError() throws {
        try super.setUpWithError()

        cardsData = try loadJSON(TarotCardsData.self, named: "cards", subdirectory: "data/tarot")
        spreadsData = try loadJSON(SpreadsData.self, named: "spreads", subdirectory: "data/tarot")
        templatesData = try loadData(named: "reading-templates", subdirectory: "data/tarot")
        luckyItemsData = try loadData(named: "items", subdirectory: "data/lucky")
        luckyColorsData = try loadData(named: "colors", subdirectory: "data/lucky")
    }

    // ---------------------------------------------------------------------------
    // Structural invariants
    // ---------------------------------------------------------------------------

    func testGeneratedReadingHasCorrectCardCount() throws {
        let reading = try generate()
        let spread = try XCTUnwrap(spreadsData.spreads.first(where: { $0.id == "three_card" }))
        XCTAssertEqual(spread.cardCount, reading.drawnCards.count, "drawn card count matches spread.cardCount")
    }

    func testNoCardDrawnTwice() throws {
        let reading = try generate()
        let ids = reading.drawnCards.map { $0.card.id }
        XCTAssertEqual(ids.count, Set(ids).count, "all drawn card IDs should be unique")
    }

    func testPositionReadingsCountMatchesCardCount() throws {
        let reading = try generate()
        XCTAssertEqual(reading.drawnCards.count, reading.positionReadings.count)
    }

    func testAllStringFieldsNonEmpty() throws {
        let reading = try generate()
        XCTAssertFalse(reading.overallEnergy.isEmpty, "overallEnergy should not be empty")
        XCTAssertFalse(reading.focusInsight.isEmpty, "focusInsight should not be empty")
        XCTAssertFalse(reading.advice.isEmpty, "advice should not be empty")
        XCTAssertFalse(reading.spreadName.isEmpty, "spreadName should not be empty")
        XCTAssertFalse(reading.spreadDescription.isEmpty, "spreadDescription should not be empty")
        XCTAssertFalse(reading.luckyItemName.isEmpty, "luckyItemName should not be empty")
        XCTAssertFalse(reading.luckyColorName.isEmpty, "luckyColorName should not be empty")
        for (i, pr) in reading.positionReadings.enumerated() {
            XCTAssertFalse(pr.interpretation.isEmpty, "positionReadings[\(i)].interpretation should not be empty")
        }
    }

    func testLuckyNumberInRange() throws {
        for _ in 0..<10 {
            let reading = try generate()
            XCTAssertTrue((1...9).contains(reading.luckyNumber),
                          "luckyNumber \(reading.luckyNumber) should be in 1...9")
        }
    }

    func testAllThemesGenerateWithoutError() throws {
        for theme in TarotQuestionTheme.allCases {
            let reading = try generate(theme: theme)
            XCTAssertFalse(reading.spreadId.isEmpty, "spreadId should not be empty for theme \(theme)")
        }
    }

    func testSpreadIdMatchesTheme() throws {
        let reading = try generate(theme: .love)
        XCTAssertEqual("love_three_card", reading.spreadId)
    }

    // ---------------------------------------------------------------------------
    // Energy assessment
    // ---------------------------------------------------------------------------

    func testAssessEnergyReturnsLightWhenTwoPlusLightCards() {
        let lightCard = makeCard(energyUpright: "light", energyReversed: "shadow")
        let shadowCard = makeCard(energyUpright: "shadow", energyReversed: "shadow")
        let cards = [
            DrawnCard(card: lightCard, isUpright: true, positionLabel: "1", positionAspect: "past"),
            DrawnCard(card: lightCard, isUpright: true, positionLabel: "2", positionAspect: "present"),
            DrawnCard(card: shadowCard, isUpright: true, positionLabel: "3", positionAspect: "next_step")
        ]
        XCTAssertEqual("light", TarotReadingGenerator.assessEnergy(cards: cards))
    }

    func testAssessEnergyReturnsShadowWhenTwoPlusShadowCards() {
        let shadowCard = makeCard(energyUpright: "shadow", energyReversed: "light")
        let lightCard = makeCard(energyUpright: "light", energyReversed: "light")
        let cards = [
            DrawnCard(card: shadowCard, isUpright: true, positionLabel: "1", positionAspect: "past"),
            DrawnCard(card: shadowCard, isUpright: true, positionLabel: "2", positionAspect: "present"),
            DrawnCard(card: lightCard, isUpright: true, positionLabel: "3", positionAspect: "next_step")
        ]
        XCTAssertEqual("shadow", TarotReadingGenerator.assessEnergy(cards: cards))
    }

    func testAssessEnergyReturnsMixedForBalancedCards() {
        let cards = [
            DrawnCard(card: makeCard(energyUpright: "light", energyReversed: "shadow"),
                      isUpright: true, positionLabel: "1", positionAspect: "past"),
            DrawnCard(card: makeCard(energyUpright: "shadow", energyReversed: "light"),
                      isUpright: true, positionLabel: "2", positionAspect: "present"),
            DrawnCard(card: makeCard(energyUpright: "mixed", energyReversed: "mixed"),
                      isUpright: true, positionLabel: "3", positionAspect: "next_step")
        ]
        XCTAssertEqual("mixed", TarotReadingGenerator.assessEnergy(cards: cards))
    }

    func testAssessEnergyAccountsForOrientation() {
        // energyUpright=shadow but drawn reversed → energyReversed=light → counts as light
        let card = makeCard(energyUpright: "shadow", energyReversed: "light")
        let cards = [
            DrawnCard(card: card, isUpright: false, positionLabel: "1", positionAspect: "past"),
            DrawnCard(card: card, isUpright: false, positionLabel: "2", positionAspect: "present"),
            DrawnCard(card: card, isUpright: true,  positionLabel: "3", positionAspect: "next_step")
        ]
        XCTAssertEqual("light", TarotReadingGenerator.assessEnergy(cards: cards))
    }

    // ---------------------------------------------------------------------------
    // toDetail round-trip
    // ---------------------------------------------------------------------------

    func testToDetailPreservesCardCount() throws {
        let reading = try generate()
        let detail = TarotReadingGenerator.toDetail(reading: reading)
        XCTAssertEqual(reading.drawnCards.count, detail.drawnCards.count)
    }

    func testToDetailPreservesCardIds() throws {
        let reading = try generate()
        let detail = TarotReadingGenerator.toDetail(reading: reading)
        let originalIds = reading.drawnCards.map { $0.card.id }
        let detailIds = detail.drawnCards.map { $0.cardId }
        XCTAssertEqual(originalIds, detailIds)
    }

    func testToDetailPreservesPositionReadingsCount() throws {
        let reading = try generate()
        let detail = TarotReadingGenerator.toDetail(reading: reading)
        XCTAssertEqual(reading.positionReadings.count, detail.positionReadings?.count ?? 0)
    }

    func testToDetailThemeIdMatchesEnum() throws {
        let reading = try generate(theme: .career)
        let detail = TarotReadingGenerator.toDetail(reading: reading)
        XCTAssertEqual("career", detail.themeId)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private func generate(theme: TarotQuestionTheme = .general) throws -> TarotReading {
        return try TarotReadingGenerator.generate(
            cardsData: cardsData,
            spreadsData: spreadsData,
            templatesData: templatesData,
            luckyItemsData: luckyItemsData,
            luckyColorsData: luckyColorsData,
            theme: theme
        )
    }

    private func loadData(named name: String, subdirectory: String) throws -> Data {
        for bundle in candidateBundles {
            if let url = bundle.url(forResource: name, withExtension: "json", subdirectory: subdirectory) {
                return try Data(contentsOf: url)
            }
            if let url = bundle.url(forResource: name, withExtension: "json") {
                return try Data(contentsOf: url)
            }
        }
        throw NSError(domain: "TarotReadingGeneratorTests", code: 1,
                      userInfo: [NSLocalizedDescriptionKey: "Missing resource: \(subdirectory)/\(name).json"])
    }

    private func loadJSON<T: Decodable>(_ type: T.Type, named name: String, subdirectory: String) throws -> T {
        let data = try loadData(named: name, subdirectory: subdirectory)
        return try JSONDecoder().decode(type, from: data)
    }

    private func makeCard(energyUpright: String, energyReversed: String) -> TarotCard {
        TarotCard(
            id: "test_\(energyUpright)_\(energyReversed)",
            number: 1,
            arcana: "minor",
            suit: "cups",
            rank: "ace",
            nameZh: "测试牌",
            nameEn: "Test Card",
            symbol: "☆",
            colorPrimary: "#000000",
            colorAccent: "#FFFFFF",
            keywordsUprightZh: ["测试"],
            keywordsUprightEn: ["test"],
            keywordsReversedZh: ["逆测试"],
            keywordsReversedEn: ["reverse test"],
            meaningUprightZh: "这是一张测试牌的正位含义。",
            meaningUprightEn: "This is the upright meaning of a test card.",
            meaningReversedZh: "这是一张测试牌的逆位含义。",
            meaningReversedEn: "This is the reversed meaning of a test card.",
            energyUpright: energyUpright,
            energyReversed: energyReversed
        )
    }
}
