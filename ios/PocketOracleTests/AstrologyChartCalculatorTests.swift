import XCTest
@testable import PocketOracle

final class AstrologyChartCalculatorTests: XCTestCase {

    private var signs: [ZodiacSign] = []
    private var citiesById: [String: BirthCity] = [:]
    private var vectors = AstrologyVectorFile(cases: [])

    private var testBundle: Bundle { Bundle(for: Self.self) }
    private var candidateBundles: [Bundle] { [testBundle, .main] }

    override func setUpWithError() throws {
        try super.setUpWithError()

        UserDefaults.standard.set(AppLanguageOption.en.rawValue, forKey: AppPreferenceKeys.appLanguage)

        let signsData = try loadData(named: "data/astrology/signs")
        signs = try JSONDecoder().decode(ZodiacSignsData.self, from: signsData).signs

        let citiesData = try loadData(named: "data/bazi/cities")
        let cities = try BaziDataLoader.parseCities(data: citiesData)
        citiesById = Dictionary(uniqueKeysWithValues: cities.map { ($0.id, $0) })

        let vectorsData = try loadData(named: "data/astrology/natal_chart_test_vectors")
        vectors = try JSONDecoder().decode(AstrologyVectorFile.self, from: vectorsData)
    }

    func testSharedVectorsMatchExpectedPlacementsAndAspects() throws {
        for vector in vectors.cases {
            let city = try XCTUnwrap(citiesById[vector.birthCityId], "Missing city for \(vector.id)")
            let snapshot = try AstrologyChartCalculator.calculate(
                input: makeInput(from: vector, city: city),
                signs: signs
            )

            XCTAssertEqual(snapshot.sunSign.id, vector.expected.sunSignId, "\(vector.id) sun sign")
            XCTAssertEqual(snapshot.moonSign.id, vector.expected.moonSignId, "\(vector.id) moon sign")
            XCTAssertEqual(snapshot.risingSign.id, vector.expected.risingSignId, "\(vector.id) rising sign")
            XCTAssertEqual(snapshot.houseFocus.map(\.house), vector.expected.houseFocus, "\(vector.id) house focus")

            for expectedPlacement in vector.expected.placements {
                let placement = try XCTUnwrap(
                    snapshot.planetPlacements.first(where: { $0.planetId.rawValue == expectedPlacement.planetId }),
                    "Missing placement \(expectedPlacement.planetId) for \(vector.id)"
                )
                XCTAssertEqual(placement.signId, expectedPlacement.signId, "\(vector.id) \(expectedPlacement.planetId) sign")
                XCTAssertEqual(placement.house, expectedPlacement.house, "\(vector.id) \(expectedPlacement.planetId) house")
                XCTAssertEqual(placement.isRetrograde, expectedPlacement.retrograde, "\(vector.id) \(expectedPlacement.planetId) retrograde")
            }

            let actualAspects = snapshot.majorAspects.map {
                AspectExpectation(
                    firstPlanetId: $0.firstPlanetId.rawValue,
                    secondPlanetId: $0.secondPlanetId.rawValue,
                    type: $0.type.rawValue
                )
            }
            XCTAssertEqual(actualAspects, vector.expected.majorAspects, "\(vector.id) major aspects")
        }
    }

    private func makeInput(from vector: AstrologyVectorCase, city: BirthCity) -> AstrologyInput {
        let dateParts = vector.birthDate.split(separator: "-").compactMap { Int($0) }
        let timeParts = vector.birthTime.split(separator: ":").compactMap { Int($0) }
        return AstrologyInput(
            birthYear: dateParts[0],
            birthMonth: dateParts[1],
            birthDay: dateParts[2],
            birthHour: timeParts[0],
            birthMinute: timeParts[1],
            city: city
        )
    }

    private func loadData(named name: String) throws -> Data {
        let components = name.split(separator: "/").map(String.init)
        let resourceName = components.last ?? name
        let subdirectory = components.dropLast().joined(separator: "/")

        let repositoryURL = repoRoot
            .appendingPathComponent("shared-content")
            .appendingPathComponent("\(name).json")
        if FileManager.default.fileExists(atPath: repositoryURL.path) {
            return try Data(contentsOf: repositoryURL)
        }

        for bundle in candidateBundles {
            if let url = bundle.url(forResource: name, withExtension: "json") {
                return try Data(contentsOf: url)
            }
            if let url = bundle.url(
                forResource: resourceName,
                withExtension: "json",
                subdirectory: subdirectory.isEmpty ? nil : subdirectory
            ) {
                return try Data(contentsOf: url)
            }
            if let url = bundle.url(forResource: resourceName, withExtension: "json") {
                return try Data(contentsOf: url)
            }
        }

        throw NSError(
            domain: "AstrologyChartCalculatorTests",
            code: 1,
            userInfo: [NSLocalizedDescriptionKey: "Missing resource: \(name).json"]
        )
    }

    private var repoRoot: URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
    }
}

private struct AstrologyVectorFile: Decodable {
    let cases: [AstrologyVectorCase]
}

private struct AstrologyVectorCase: Decodable {
    let id: String
    let birthDate: String
    let birthTime: String
    let birthCityId: String
    let expected: AstrologyExpectedChart

    enum CodingKeys: String, CodingKey {
        case id
        case birthDate = "birth_date"
        case birthTime = "birth_time"
        case birthCityId = "birth_city_id"
        case expected
    }
}

private struct AstrologyExpectedChart: Decodable {
    let sunSignId: String
    let moonSignId: String
    let risingSignId: String
    let houseFocus: [Int]
    let placements: [PlacementExpectation]
    let majorAspects: [AspectExpectation]

    enum CodingKeys: String, CodingKey {
        case sunSignId = "sun_sign_id"
        case moonSignId = "moon_sign_id"
        case risingSignId = "rising_sign_id"
        case houseFocus = "house_focus"
        case placements
        case majorAspects = "major_aspects"
    }
}

private struct PlacementExpectation: Decodable, Equatable {
    let planetId: String
    let signId: String
    let house: Int
    let retrograde: Bool

    enum CodingKeys: String, CodingKey {
        case planetId = "planet_id"
        case signId = "sign_id"
        case house
        case retrograde
    }
}

private struct AspectExpectation: Decodable, Equatable {
    let firstPlanetId: String
    let secondPlanetId: String
    let type: String

    enum CodingKeys: String, CodingKey {
        case firstPlanetId = "first_planet_id"
        case secondPlanetId = "second_planet_id"
        case type
    }
}
