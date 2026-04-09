import Foundation

/// Computes a full 四柱八字 (Four Pillars) chart from a `BaziInput`.
///
/// References:
///   - Day pillar anchor: JDN 2451551 = 2000-01-07 = 甲子日
///   - Solar terms: `SolarTermsEngine` (Meeus algorithm, ±5 min accuracy)
///   - Ten gods: derived from 5-element relationship + yin/yang vs day master
///   - Hidden stems: loaded from `hidden_stems.json` via `BaziDataLoader`
///   - Major cycles: direction = year stem polarity xor gender; start age = days to nearest term ÷ 3
enum BaziCalculator {

    static func calculate(
        input: BaziInput,
        hiddenStemsMap overrideHiddenStems: [Int: [HiddenStemEntry]]? = nil
    ) throws -> BaziChart {
        let hiddenStemsMap = try overrideHiddenStems ?? BaziDataLoader.loadHiddenStems()

        let resolvedTiming = resolveBirthTiming(input: input)
        let lateZiResult = applyLateZi(
            effectiveLocalComponents: resolvedTiming.effectiveLocalComponents,
            distinguishLateZi: input.distinguishLateZiHour,
            hasTime: input.birthHour != nil
        )

        let (termIndex, baziYear) = SolarTermsEngine.monthTermAndBaziYear(for: resolvedTiming.birthInstant)
        let yearPillar = yearPillar(baziYear: baziYear)
        let monthPillar = monthPillar(termIndex: termIndex, yearStemIndex: yearPillar.stemIndex)

        let dayPillar = dayPillar(localDate: lateZiResult.effectiveLocalComponents)
        let hourPillarValue: Pillar?
        if input.birthHour != nil {
            hourPillarValue = hourPillar(
                localHour: lateZiResult.effectiveLocalComponents.hour ?? 0,
                dayStemIndex: dayPillar.stemIndex
            )
        } else {
            hourPillarValue = nil
        }

        var hiddenStems: [Int: [HiddenStemEntry]] = [:]
        for branchIndex in [yearPillar.branchIndex, monthPillar.branchIndex, dayPillar.branchIndex] + (hourPillarValue.map { [$0.branchIndex] } ?? []) {
            if hiddenStems[branchIndex] == nil {
                hiddenStems[branchIndex] = hiddenStemsMap[branchIndex] ?? []
            }
        }

        let fiveElements = computeFiveElements(
            pillars: [yearPillar, monthPillar, dayPillar] + (hourPillarValue.map { [$0] } ?? []),
            hiddenStems: hiddenStems
        )

        let tenGods = computeTenGods(
            dayMaster: dayPillar.stemIndex,
            yearPillar: yearPillar,
            monthPillar: monthPillar,
            dayPillar: dayPillar,
            hourPillar: hourPillarValue,
            hiddenStems: hiddenStems
        )

        let (cycles, startingAge, direction) = computeMajorCycles(
            input: input,
            birthInstant: resolvedTiming.birthInstant,
            yearStemIndex: yearPillar.stemIndex,
            monthPillar: monthPillar
        )

        return BaziChart(
            input: input,
            yearPillar: yearPillar,
            monthPillar: monthPillar,
            dayPillar: dayPillar,
            hourPillar: hourPillarValue,
            hiddenStems: hiddenStems,
            tenGods: tenGods,
            fiveElements: fiveElements,
            majorCycles: cycles,
            startingAge: startingAge,
            cycleDirection: direction
        )
    }

    struct ResolvedBirthTiming {
        let birthInstant: Date
        let effectiveLocalComponents: DateComponents
    }

    private struct LateZiResult {
        let effectiveLocalComponents: DateComponents
    }

    static func resolveBirthTiming(input: BaziInput) -> ResolvedBirthTiming {
        let zone = resolvedTimeZone(city: input.city)
        var civilComponents = DateComponents()
        civilComponents.calendar = gregorianCalendar
        civilComponents.year = input.birthYear
        civilComponents.month = input.birthMonth
        civilComponents.day = input.birthDay
        civilComponents.hour = input.birthHour ?? 12
        civilComponents.minute = input.birthMinute ?? 0
        civilComponents.second = 0

        let birthInstant = resolvedBirthInstant(
            city: input.city,
            zone: zone,
            civilComponents: civilComponents
        )

        let effectiveLocalComponents: DateComponents
        if input.useTrueSolarTime, input.birthHour != nil, let city = input.city {
            let solarDate = trueSolarLocalDate(
                birthInstant: birthInstant,
                civilComponents: civilComponents,
                city: city
            )
            effectiveLocalComponents = utcComponents(from: solarDate)
        } else {
            effectiveLocalComponents = civilComponents
        }

        return ResolvedBirthTiming(
            birthInstant: birthInstant,
            effectiveLocalComponents: effectiveLocalComponents
        )
    }

    private static func resolvedTimeZone(city: BirthCity?) -> TimeZone {
        let identifier = city?.timeZoneId ?? DefaultBaziTimeZoneId
        return TimeZone(identifier: identifier) ?? TimeZone(identifier: DefaultBaziTimeZoneId) ?? .gmt
    }

    /**
     * Use the dataset's standard offset as the base legal time, then apply IANA DST
     * adjustments on top. This keeps historical Chinese dates on UTC+8 instead of
     * drifting to pre-standard local mean time offsets, while still honoring DST
     * for cities like New York or London.
     */
    private static func resolvedBirthInstant(
        city: BirthCity?,
        zone: TimeZone,
        civilComponents: DateComponents
    ) -> Date {
        let standardOffsetSeconds = Int(((city?.utcOffsetHours ?? 8.0) * 3600.0).rounded())

        var standardComponents = civilComponents
        standardComponents.timeZone = TimeZone(secondsFromGMT: standardOffsetSeconds)
        let approximateInstant = gregorianCalendar.date(from: standardComponents) ?? Date()

        let dstSeconds = Int(zone.daylightSavingTimeOffset(for: approximateInstant))
        var resolvedComponents = civilComponents
        resolvedComponents.timeZone = TimeZone(secondsFromGMT: standardOffsetSeconds + dstSeconds)
        return gregorianCalendar.date(from: resolvedComponents) ?? approximateInstant
    }

    /**
     * Converts the civil birth instant into local apparent solar time.
     *
     * Formula:
     *   true solar time = UTC + longitude * 4min + equation_of_time
     */
    private static func trueSolarLocalDate(
        birthInstant: Date,
        civilComponents: DateComponents,
        city: BirthCity
    ) -> Date {
        let solarOffsetMinutes = (city.longitudeEast * 4.0) + equationOfTimeMinutes(civilComponents: civilComponents)
        return birthInstant.addingTimeInterval(solarOffsetMinutes * 60.0)
    }

    /**
     * NOAA approximation of equation of time, in minutes.
     */
    private static func equationOfTimeMinutes(civilComponents: DateComponents) -> Double {
        let zone = civilComponents.timeZone ?? .gmt
        let civilDate = gregorianCalendar.date(from: civilComponents) ?? Date()
        let dayOfYear = gregorianCalendar.ordinality(of: .day, in: .year, for: civilDate, timeZone: zone) ?? 1
        let hour = civilComponents.hour ?? 12
        let minute = civilComponents.minute ?? 0
        let fractionalHour = Double(hour) + (Double(minute) / 60.0)
        let gamma = 2.0 * Double.pi / 365.0 * (Double(dayOfYear - 1) + ((fractionalHour - 12.0) / 24.0))
        return 229.18 * (
            0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2.0 * gamma) -
                0.040849 * sin(2.0 * gamma)
        )
    }

    private static func applyLateZi(
        effectiveLocalComponents: DateComponents,
        distinguishLateZi: Bool,
        hasTime: Bool
    ) -> LateZiResult {
        guard hasTime, distinguishLateZi, effectiveLocalComponents.hour == 23 else {
            return LateZiResult(effectiveLocalComponents: effectiveLocalComponents)
        }
        let shifted = shiftUTCComponents(effectiveLocalComponents, byDays: 1)
        return LateZiResult(effectiveLocalComponents: shifted)
    }

    private static func yearPillar(baziYear: Int) -> Pillar {
        let offset = baziYear - 4
        let stem = ((offset % 10) + 10) % 10
        let branch = ((offset % 12) + 12) % 12
        return Pillar(stem, branch)
    }

    private static func monthPillar(termIndex: Int, yearStemIndex: Int) -> Pillar {
        let monthBranch = SolarTermsEngine.monthTerms[termIndex].monthBranch
        let yinMonthStem = ((yearStemIndex % 5) * 2 + 2) % 10
        let monthStem = (yinMonthStem + termIndex) % 10
        return Pillar(monthStem, monthBranch)
    }

    private static func dayPillar(localDate: DateComponents) -> Pillar {
        let year = localDate.year ?? 2000
        let month = localDate.month ?? 1
        let day = localDate.day ?? 1
        let jdn = julianDayNumber(year: year, month: month, day: day)
        let offset = jdn - 2451551
        let idx = ((offset % 60) + 60) % 60
        return Pillar(idx % 10, idx % 12)
    }

    private static func julianDayNumber(year: Int, month: Int, day: Int) -> Int {
        let a = (14 - month) / 12
        let yy = year + 4800 - a
        let mm = month + 12 * a - 3
        return day + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }

    private static func hourPillar(localHour: Int, dayStemIndex: Int) -> Pillar {
        let hourBranch = localHour == 23 ? 0 : (localHour + 1) / 2
        let hourStem = ((dayStemIndex % 5) * 2 + hourBranch) % 10
        return Pillar(hourStem, hourBranch)
    }

    private static func tenGodOf(dayMaster: Int, targetStem: Int) -> TenGodEntry {
        let dayMasterElement = StemElements[dayMaster]
        let targetElement = StemElements[targetStem]
        let samePolarity = StemPolarity[dayMaster] == StemPolarity[targetStem]

        let relationship = elementRelationship(from: dayMasterElement, to: targetElement)
        let godIndex: Int
        switch relationship {
        case 0: godIndex = samePolarity ? 0 : 1
        case 1: godIndex = samePolarity ? 2 : 3
        case 2: godIndex = samePolarity ? 8 : 9
        case 3: godIndex = samePolarity ? 4 : 5
        case 4: godIndex = samePolarity ? 6 : 7
        default: godIndex = 0
        }
        return TenGodEntry(name: TenGodNamesZh[godIndex], nameEn: TenGodNamesEn[godIndex])
    }

    private static func elementRelationship(from: Int, to: Int) -> Int {
        if from == to { return 0 }
        if (from + 1) % 5 == to { return 1 }
        if (to + 1) % 5 == from { return 2 }
        let controls: [Int: Int] = [0: 2, 2: 4, 4: 1, 1: 3, 3: 0]
        if controls[from] == to { return 3 }
        if controls[to] == from { return 4 }
        return 0
    }

    private static func computeTenGods(
        dayMaster: Int,
        yearPillar: Pillar,
        monthPillar: Pillar,
        dayPillar: Pillar,
        hourPillar: Pillar?,
        hiddenStems: [Int: [HiddenStemEntry]]
    ) -> TenGods {
        func mainHiddenStem(of branchIndex: Int) -> Int {
            hiddenStems[branchIndex]?.first(where: { $0.weight == .main })?.stemIndex ?? 0
        }

        return TenGods(
            yearStemGod: tenGodOf(dayMaster: dayMaster, targetStem: yearPillar.stemIndex),
            monthStemGod: tenGodOf(dayMaster: dayMaster, targetStem: monthPillar.stemIndex),
            hourStemGod: hourPillar.map { tenGodOf(dayMaster: dayMaster, targetStem: $0.stemIndex) },
            yearBranchGod: tenGodOf(dayMaster: dayMaster, targetStem: mainHiddenStem(of: yearPillar.branchIndex)),
            monthBranchGod: tenGodOf(dayMaster: dayMaster, targetStem: mainHiddenStem(of: monthPillar.branchIndex)),
            dayBranchGod: tenGodOf(dayMaster: dayMaster, targetStem: mainHiddenStem(of: dayPillar.branchIndex)),
            hourBranchGod: hourPillar.map { tenGodOf(dayMaster: dayMaster, targetStem: mainHiddenStem(of: $0.branchIndex)) }
        )
    }

    private static func computeFiveElements(
        pillars: [Pillar],
        hiddenStems: [Int: [HiddenStemEntry]]
    ) -> FiveElementStrength {
        var strength = FiveElementStrength()
        for pillar in pillars {
            strength.add(StemElements[pillar.stemIndex], 2)
            for hiddenStem in hiddenStems[pillar.branchIndex] ?? [] {
                let weight: Int
                switch hiddenStem.weight {
                case .main: weight = 3
                case .mid: weight = 2
                case .minor: weight = 1
                }
                strength.add(StemElements[hiddenStem.stemIndex], weight)
            }
        }
        return strength
    }

    private static func computeMajorCycles(
        input: BaziInput,
        birthInstant: Date,
        yearStemIndex: Int,
        monthPillar: Pillar
    ) -> (cycles: [MajorCycle], startingAge: Int, direction: String) {
        let isYangYear = StemPolarity[yearStemIndex] == 0
        let isMale = input.gender == .male
        let forward = isYangYear == isMale
        let direction = forward ? "顺排" : "逆排"

        let refDate: Date = forward
            ? SolarTermsEngine.nextTerm(after: birthInstant).date
            : SolarTermsEngine.previousTerm(before: birthInstant).date

        let daysDiff = abs(refDate.timeIntervalSince(birthInstant)) / 86_400.0
        let startingAge = max(1, Int((daysDiff / 3.0).rounded()))

        let baseIndex = jiaziIndex(stem: monthPillar.stemIndex, branch: monthPillar.branchIndex)
        var cycles: [MajorCycle] = []
        for step in 1...10 {
            let delta = forward ? step : -step
            let cycleIndex = ((baseIndex + delta) % 60 + 60) % 60
            cycles.append(
                MajorCycle(
                    startAge: startingAge + (step - 1) * 10,
                    pillar: Pillar(cycleIndex % 10, cycleIndex % 12)
                )
            )
        }
        return (cycles, startingAge, direction)
    }

    private static func jiaziIndex(stem: Int, branch: Int) -> Int {
        for value in 0..<60 where value % 10 == stem && value % 12 == branch {
            return value
        }
        return 0
    }

    private static let gregorianCalendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .gmt
        return calendar
    }()

    private static func utcComponents(from date: Date) -> DateComponents {
        gregorianCalendar.dateComponents([.year, .month, .day, .hour, .minute], from: date)
    }

    private static func shiftUTCComponents(_ components: DateComponents, byDays days: Int) -> DateComponents {
        var normalized = components
        normalized.calendar = gregorianCalendar
        normalized.timeZone = .gmt
        let baseDate = gregorianCalendar.date(from: normalized) ?? Date()
        let shifted = baseDate.addingTimeInterval(Double(days) * 86_400.0)
        return utcComponents(from: shifted)
    }
}

enum BaziDataLoader {

    struct HiddenStemRecord: Decodable {
        let branch: Int
        let stems: [StemRecord]

        struct StemRecord: Decodable {
            let stem: Int
            let weight: String
        }
    }

    struct HiddenStemsFile: Decodable {
        let hidden_stems: [HiddenStemRecord]
    }

    static func loadHiddenStems(bundle: Bundle = .main) throws -> [Int: [HiddenStemEntry]] {
        let data = try loadJSON(named: "data/bazi/hidden_stems", bundle: bundle)
        return try parseHiddenStems(data: data)
    }

    static func parseHiddenStems(data: Data) throws -> [Int: [HiddenStemEntry]] {
        let file = try JSONDecoder().decode(HiddenStemsFile.self, from: data)
        var map: [Int: [HiddenStemEntry]] = [:]
        for record in file.hidden_stems {
            map[record.branch] = record.stems.compactMap { stem in
                guard let weight = HiddenStemWeight(rawValue: stem.weight) else { return nil }
                return HiddenStemEntry(stemIndex: stem.stem, weight: weight)
            }
        }
        return map
    }

    static func loadCities(bundle: Bundle = .main) throws -> [BirthCity] {
        let data = try loadJSON(named: "data/bazi/cities", bundle: bundle)
        return try parseCities(data: data)
    }

    static func parseCities(data: Data) throws -> [BirthCity] {
        let file = try JSONDecoder().decode(CitiesData.self, from: data)
        return file.cities
    }

    private static func loadJSON(named name: String, bundle: Bundle) throws -> Data {
        if let url = bundle.url(forResource: name, withExtension: "json") {
            return try Data(contentsOf: url)
        }

        let components = name.split(separator: "/").map(String.init)
        let resourceName = components.last ?? name
        let subdirectory = components.dropLast().joined(separator: "/")

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

        throw BaziError.missingDataFile("\(name).json")
    }
}

enum BaziError: Error, LocalizedError {
    case missingDataFile(String)
    case invalidInput(String)

    var errorDescription: String? {
        switch self {
        case .missingDataFile(let name):
            return "Missing data file: \(name)"
        case .invalidInput(let message):
            return "Invalid input: \(message)"
        }
    }
}

private extension Calendar {
    func ordinality(
        of smaller: Calendar.Component,
        in larger: Calendar.Component,
        for date: Date,
        timeZone: TimeZone
    ) -> Int? {
        var calendar = self
        calendar.timeZone = timeZone
        return calendar.ordinality(of: smaller, in: larger, for: date)
    }
}
