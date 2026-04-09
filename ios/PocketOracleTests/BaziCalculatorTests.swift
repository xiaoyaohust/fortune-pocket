import XCTest
@testable import PocketOracle

final class BaziCalculatorTests: XCTestCase {

    private var hiddenStems: [Int: [HiddenStemEntry]] = [:]
    private var citiesById: [String: BirthCity] = [:]
    private var vectors = TestVectorsFile(cases: [])

    private var testBundle: Bundle {
        Bundle(for: Self.self)
    }

    private var candidateBundles: [Bundle] {
        [testBundle, .main]
    }

    override func setUpWithError() throws {
        try super.setUpWithError()

        let hiddenStemsData = try loadData(named: "data/bazi/hidden_stems")
        hiddenStems = try BaziDataLoader.parseHiddenStems(data: hiddenStemsData)

        let citiesData = try loadData(named: "data/bazi/cities")
        let cities = try BaziDataLoader.parseCities(data: citiesData)
        citiesById = Dictionary(uniqueKeysWithValues: cities.map { ($0.id, $0) })

        let vectorsData = try loadData(named: "data/bazi/test_vectors")
        vectors = try JSONDecoder().decode(TestVectorsFile.self, from: vectorsData)
    }

    func testSharedTestVectorsMatchExpectedPillars() throws {
        for vector in vectors.cases {
            let chart = try BaziCalculator.calculate(
                input: makeInput(from: vector),
                hiddenStemsMap: hiddenStems
            )

            if let expected = vector.expected.yearPillar {
                XCTAssertEqual(chart.yearPillar.nameZh, expected, "\(vector.id) year pillar")
            }
            if let expected = vector.expected.monthPillar {
                XCTAssertEqual(chart.monthPillar.nameZh, expected, "\(vector.id) month pillar")
            }
            if let expected = vector.expected.dayPillar {
                XCTAssertEqual(chart.dayPillar.nameZh, expected, "\(vector.id) day pillar")
            }
            if let expected = vector.expected.hourPillar {
                XCTAssertEqual(chart.hourPillar?.nameZh, expected, "\(vector.id) hour pillar")
            }
        }
    }

    func testResolvesIanaTimeZoneWithDst() {
        let newYork = try! XCTUnwrap(citiesById["new_york"])
        let timing = BaziCalculator.resolveBirthTiming(
            input: BaziInput(
                birthYear: 2024,
                birthMonth: 7,
                birthDay: 1,
                birthHour: 12,
                birthMinute: 0,
                city: newYork,
                gender: .female,
                useTrueSolarTime: false,
                distinguishLateZiHour: false
            )
        )

        let utc = utcComponents(from: timing.birthInstant)
        XCTAssertEqual(utc.hour, 16)
        XCTAssertEqual(utc.minute, 0)
    }

    func testTrueSolarTimeAddsEquationOfTimeBeyondLongitudeOnlyCorrection() {
        let beijing = try! XCTUnwrap(citiesById["beijing"])
        let civilInput = BaziInput(
            birthYear: 2024,
            birthMonth: 2,
            birthDay: 4,
            birthHour: 12,
            birthMinute: 0,
            city: beijing,
            gender: .male,
            useTrueSolarTime: false,
            distinguishLateZiHour: false
        )
        let solarInput = BaziInput(
            birthYear: 2024,
            birthMonth: 2,
            birthDay: 4,
            birthHour: 12,
            birthMinute: 0,
            city: beijing,
            gender: .male,
            useTrueSolarTime: true,
            distinguishLateZiHour: false
        )

        let civilTiming = BaziCalculator.resolveBirthTiming(input: civilInput)
        let solarTiming = BaziCalculator.resolveBirthTiming(input: solarInput)
        let longitudeOnlyDate = civilTiming.birthInstant.addingTimeInterval(beijing.longitudeEast * 4.0 * 60.0)
        let longitudeOnly = utcComponents(from: longitudeOnlyDate)

        XCTAssertNotEqual(longitudeOnly.minute, solarTiming.effectiveLocalComponents.minute)
        XCTAssertNotEqual(civilTiming.effectiveLocalComponents.hour, solarTiming.effectiveLocalComponents.hour)
    }

    func testLateZiAdvancesDayButKeepsZiHour() throws {
        let beijing = try XCTUnwrap(citiesById["beijing"])
        let chart = try BaziCalculator.calculate(
            input: BaziInput(
                birthYear: 2000,
                birthMonth: 1,
                birthDay: 7,
                birthHour: 23,
                birthMinute: 30,
                city: beijing,
                gender: .male,
                useTrueSolarTime: false,
                distinguishLateZiHour: true
            ),
            hiddenStemsMap: hiddenStems
        )

        XCTAssertEqual(chart.dayPillar.nameZh, "乙丑")
        XCTAssertEqual(chart.hourPillar?.nameZh, "丙子")
    }

    private func makeInput(from vector: TestVectorCase) -> BaziInput {
        let dateParts = vector.birthDate.split(separator: "-").compactMap { Int($0) }
        let timeParts = vector.birthTime?.split(separator: ":").compactMap { Int($0) } ?? []

        return BaziInput(
            birthYear: dateParts[0],
            birthMonth: dateParts[1],
            birthDay: dateParts[2],
            birthHour: timeParts.first,
            birthMinute: timeParts.dropFirst().first,
            city: vector.birthCityId.flatMap { citiesById[$0] },
            gender: vector.gender == "female" ? .female : .male,
            useTrueSolarTime: vector.useTrueSolarTime,
            distinguishLateZiHour: false
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

        throw BaziError.missingDataFile("\(name).json")
    }

    private var repoRoot: URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
    }

    private func utcComponents(from date: Date) -> DateComponents {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .gmt
        return calendar.dateComponents([.year, .month, .day, .hour, .minute], from: date)
    }
}

private struct TestVectorsFile: Decodable {
    let cases: [TestVectorCase]
}

private struct TestVectorCase: Decodable {
    let id: String
    let birthDate: String
    let birthTime: String?
    let birthCityId: String?
    let gender: String
    let useTrueSolarTime: Bool
    let expected: ExpectedPillars

    enum CodingKeys: String, CodingKey {
        case id
        case birthDate = "birth_date"
        case birthTime = "birth_time"
        case birthCityId = "birth_city_id"
        case gender
        case useTrueSolarTime = "use_true_solar_time"
        case expected
    }
}

private struct ExpectedPillars: Decodable {
    let yearPillar: String?
    let monthPillar: String?
    let dayPillar: String?
    let hourPillar: String?

    enum CodingKeys: String, CodingKey {
        case yearPillar = "year_pillar"
        case monthPillar = "month_pillar"
        case dayPillar = "day_pillar"
        case hourPillar = "hour_pillar"
    }
}
