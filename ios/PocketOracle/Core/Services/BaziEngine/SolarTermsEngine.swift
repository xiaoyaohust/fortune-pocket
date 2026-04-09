import Foundation

/// Computes solar term (节气) dates by numerically solving for the Sun's apparent
/// ecliptic longitude near an approximate Gregorian date.
enum SolarTermsEngine {

    struct MonthTerm {
        let nameZh: String
        let nameEn: String
        let longitude: Double
        let monthBranch: Int
    }

    static let monthTerms: [MonthTerm] = [
        MonthTerm(nameZh: "立春", nameEn: "Lichun", longitude: 315.0, monthBranch: 2),
        MonthTerm(nameZh: "惊蛰", nameEn: "Jingzhe", longitude: 345.0, monthBranch: 3),
        MonthTerm(nameZh: "清明", nameEn: "Qingming", longitude: 15.0, monthBranch: 4),
        MonthTerm(nameZh: "立夏", nameEn: "Lixia", longitude: 45.0, monthBranch: 5),
        MonthTerm(nameZh: "芒种", nameEn: "Mangzhong", longitude: 75.0, monthBranch: 6),
        MonthTerm(nameZh: "小暑", nameEn: "Xiaoshu", longitude: 105.0, monthBranch: 7),
        MonthTerm(nameZh: "立秋", nameEn: "Liqiu", longitude: 135.0, monthBranch: 8),
        MonthTerm(nameZh: "白露", nameEn: "Bailu", longitude: 165.0, monthBranch: 9),
        MonthTerm(nameZh: "寒露", nameEn: "Hanlu", longitude: 195.0, monthBranch: 10),
        MonthTerm(nameZh: "立冬", nameEn: "Lidong", longitude: 225.0, monthBranch: 11),
        MonthTerm(nameZh: "大雪", nameEn: "Daxue", longitude: 255.0, monthBranch: 0),
        MonthTerm(nameZh: "小寒", nameEn: "Xiaohan", longitude: 285.0, monthBranch: 1),
    ]

    static func date(of term: MonthTerm, inYear year: Int) -> Date {
        let targetLongitude = normalizeAngle(term.longitude)
        let approxDate = approximateTermDate(targetLongitude: targetLongitude, year: year)
        let (start, end) = findBracket(targetLongitude: targetLongitude, approxDate: approxDate)
        return refineCrossing(targetLongitude: targetLongitude, start: start, end: end)
    }

    static func lichun(year: Int) -> Date {
        date(of: monthTerms[0], inYear: year)
    }

    static func monthTermAndBaziYear(for utcDate: Date) -> (termIndex: Int, baziYear: Int) {
        let gYear = utcYear(of: utcDate)

        var candidates: [(date: Date, termIndex: Int, baziYear: Int)] = []
        for year in (gYear - 1)...(gYear + 1) {
            for (index, term) in monthTerms.enumerated() {
                let date = date(of: term, inYear: year)
                let baziYear = index == 0 ? year : (date < lichun(year: year) ? year - 1 : year)
                candidates.append((date, index, baziYear))
            }
        }
        candidates.sort { $0.date < $1.date }

        var current = candidates[0]
        for candidate in candidates where candidate.date <= utcDate {
            current = candidate
        }
        return (current.termIndex, current.baziYear)
    }

    static func nextTerm(after utcDate: Date) -> (term: MonthTerm, date: Date) {
        let year = utcYear(of: utcDate)
        for candidateYear in year...(year + 1) {
            for term in monthTerms {
                let date = self.date(of: term, inYear: candidateYear)
                if date > utcDate { return (term, date) }
            }
        }
        let term = monthTerms[0]
        return (term, date(of: term, inYear: year + 2))
    }

    static func previousTerm(before utcDate: Date) -> (term: MonthTerm, date: Date) {
        let year = utcYear(of: utcDate)
        for candidateYear in stride(from: year + 1, through: year - 1, by: -1) {
            for term in monthTerms.reversed() {
                let date = self.date(of: term, inYear: candidateYear)
                if date < utcDate { return (term, date) }
            }
        }
        let term = monthTerms.last!
        return (term, date(of: term, inYear: year - 2))
    }

    private static func approximateTermDate(targetLongitude: Double, year: Int) -> Date {
        let yearFraction = solarLongitudeToYearFraction(targetLongitude, nearYear: year)
        let wholeYear = Int(floor(yearFraction))
        let fraction = yearFraction - Double(wholeYear)
        let yearStart = utcCalendar.date(from: DateComponents(year: wholeYear, month: 1, day: 1)) ?? .init()
        return yearStart.addingTimeInterval(fraction * 365.2422 * daySeconds)
    }

    private static func findBracket(targetLongitude: Double, approxDate: Date) -> (Date, Date) {
        var previous = approxDate.addingTimeInterval(-15.0 * daySeconds)
        var previousDiff = signedLongitudeDifference(
            current: solarLongitudeDegrees(at: previous),
            target: targetLongitude
        )

        var current = previous.addingTimeInterval(bracketStepSeconds)
        for _ in 0..<120 {
            let currentDiff = signedLongitudeDifference(
                current: solarLongitudeDegrees(at: current),
                target: targetLongitude
            )
            if hasSignChange(previousDiff, currentDiff) {
                return (previous, current)
            }
            previous = current
            previousDiff = currentDiff
            current = current.addingTimeInterval(bracketStepSeconds)
        }

        return (
            approxDate.addingTimeInterval(-daySeconds),
            approxDate.addingTimeInterval(daySeconds)
        )
    }

    private static func refineCrossing(targetLongitude: Double, start: Date, end: Date) -> Date {
        var low = start
        var high = end
        for _ in 0..<48 {
            let mid = low.addingTimeInterval(high.timeIntervalSince(low) / 2.0)
            let lowDiff = signedLongitudeDifference(
                current: solarLongitudeDegrees(at: low),
                target: targetLongitude
            )
            let midDiff = signedLongitudeDifference(
                current: solarLongitudeDegrees(at: mid),
                target: targetLongitude
            )
            if hasSignChange(lowDiff, midDiff) {
                high = mid
            } else {
                low = mid
            }
        }
        return low.addingTimeInterval(high.timeIntervalSince(low) / 2.0)
    }

    /**
     * Solar apparent longitude approximation in degrees.
     *
     * Sufficient for term-boundary solving after binary refinement.
     */
    private static func solarLongitudeDegrees(at utcDate: Date) -> Double {
        let julianDay = (utcDate.timeIntervalSince1970 / daySeconds) + unixEpochJulianDay
        let t = (julianDay - 2451545.0) / 36525.0

        let meanLongitude = normalizeAngle(
            280.46646 + (36000.76983 * t) + (0.0003032 * t * t)
        )
        let meanAnomaly = normalizeAngle(
            357.52911 + (35999.05029 * t) - (0.0001537 * t * t)
        )
        let meanAnomalyRad = degreesToRadians(meanAnomaly)

        let equationOfCenter =
            (1.914602 - (0.004817 * t) - (0.000014 * t * t)) * sin(meanAnomalyRad) +
            (0.019993 - (0.000101 * t)) * sin(2.0 * meanAnomalyRad) +
            0.000289 * sin(3.0 * meanAnomalyRad)

        let trueLongitude = meanLongitude + equationOfCenter
        let omega = 125.04 - (1934.136 * t)
        let apparentLongitude = trueLongitude - 0.00569 - (0.00478 * sin(degreesToRadians(omega)))
        return normalizeAngle(apparentLongitude)
    }

    private static func solarLongitudeToYearFraction(_ longitude: Double, nearYear: Int) -> Double {
        let daysSinceEquinox = (longitude < 0 ? longitude + 360.0 : longitude) / 360.0 * 365.2422
        let equinoxDayOfYear = 79.0
        var dayOfYear = equinoxDayOfYear + daysSinceEquinox
        if dayOfYear > 365.2422 { dayOfYear -= 365.2422 }
        return Double(nearYear) + dayOfYear / 365.2422
    }

    private static func utcYear(of date: Date) -> Int {
        utcCalendar.component(.year, from: date)
    }

    private static func normalizeAngle(_ value: Double) -> Double {
        var angle = value.truncatingRemainder(dividingBy: 360.0)
        if angle < 0 { angle += 360.0 }
        return angle
    }

    private static func signedLongitudeDifference(current: Double, target: Double) -> Double {
        var delta = normalizeAngle(current) - normalizeAngle(target)
        if delta > 180.0 { delta -= 360.0 }
        if delta <= -180.0 { delta += 360.0 }
        return delta
    }

    private static func hasSignChange(_ first: Double, _ second: Double) -> Bool {
        first == 0.0 || second == 0.0 || (first < 0 && second > 0) || (first > 0 && second < 0)
    }

    private static func degreesToRadians(_ value: Double) -> Double {
        value * .pi / 180.0
    }

    private static let utcCalendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .gmt
        return calendar
    }()

    private static let daySeconds = 86_400.0
    private static let bracketStepSeconds = 6.0 * 60.0 * 60.0
    private static let unixEpochJulianDay = 2440587.5
}
