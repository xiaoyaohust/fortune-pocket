import Foundation

struct AstrologyInput {
    let birthYear: Int
    let birthMonth: Int
    let birthDay: Int
    let birthHour: Int
    let birthMinute: Int
    let city: BirthCity
}

struct AstrologyChartSnapshot {
    let sunSign: ZodiacSign
    let moonSign: ZodiacSign
    let risingSign: ZodiacSign
    let planetPlacements: [AstrologyPlanetPlacement]
    let majorAspects: [AstrologyAspect]
    let elementBalance: AstrologyElementBalance
    let houseFocus: [AstrologyHouseFocus]
    let birthTimeText: String
    let birthDateText: String
    let birthCityName: String
    let timeZoneId: String
}

enum AstrologyEngineError: LocalizedError {
    case invalidBirthTime
    case missingTimeZone(String)
    case missingSigns

    var errorDescription: String? {
        switch self {
        case .invalidBirthTime:
            return "Invalid birth time."
        case .missingTimeZone(let identifier):
            return "Missing time zone: \(identifier)"
        case .missingSigns:
            return "Missing zodiac sign definitions."
        }
    }
}

enum AstrologyChartCalculator {

    static func calculate(
        input: AstrologyInput,
        signs: [ZodiacSign]
    ) throws -> AstrologyChartSnapshot {
        guard !signs.isEmpty else { throw AstrologyEngineError.missingSigns }
        guard let zone = TimeZone(identifier: input.city.timeZoneId) else {
            throw AstrologyEngineError.missingTimeZone(input.city.timeZoneId)
        }

        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = zone

        let birthComponents = DateComponents(
            year: input.birthYear,
            month: input.birthMonth,
            day: input.birthDay,
            hour: input.birthHour,
            minute: input.birthMinute,
            second: 0
        )
        guard let birthDate = calendar.date(from: birthComponents) else {
            throw AstrologyEngineError.invalidBirthTime
        }

        let julianDay = birthDate.timeIntervalSince1970 / 86_400.0 + 2_440_587.5
        let d = julianDay - 2_451_543.5
        let ascendantLongitude = ascendantLongitude(
            julianDay: julianDay,
            longitudeEast: input.city.longitudeEast,
            latitudeNorth: input.city.latitudeNorth
        )

        let placements = AstrologyPlanetID.allCases.map { planet -> AstrologyPlanetPlacement in
            let longitude = normalizeAngle(planetLongitude(planet, d: d))
            let sign = signFor(longitude: longitude, signs: signs)
            let retrograde = isRetrograde(planet, d: d)
            let house = houseFor(longitude: longitude, ascendantLongitude: ascendantLongitude)
            return AstrologyPlanetPlacement(
                planetId: planet,
                longitude: longitude,
                signId: sign.id,
                signNameZh: sign.nameZh,
                signNameEn: sign.nameEn,
                house: house,
                isRetrograde: retrograde
            )
        }

        let sunPlacement = try placement(.sun, in: placements)
        let moonPlacement = try placement(.moon, in: placements)
        let sunSign = signFor(signId: sunPlacement.signId, signs: signs)
        let moonSign = signFor(signId: moonPlacement.signId, signs: signs)
        let risingSign = signFor(longitude: ascendantLongitude, signs: signs)

        let aspects = majorAspects(from: placements)
        let balance = elementBalance(for: placements, risingSign: risingSign, signs: signs)
        let houseFocus = houseFocus(from: placements)

        let dateFormatter = DateFormatter()
        dateFormatter.calendar = Calendar(identifier: .gregorian)
        dateFormatter.timeZone = zone
        dateFormatter.locale = AppLanguageOption.isChinese
            ? Locale(identifier: "zh-Hans")
            : Locale(identifier: "en_US_POSIX")
        dateFormatter.dateStyle = .long

        return AstrologyChartSnapshot(
            sunSign: sunSign,
            moonSign: moonSign,
            risingSign: risingSign,
            planetPlacements: placements,
            majorAspects: aspects,
            elementBalance: balance,
            houseFocus: houseFocus,
            birthTimeText: String(format: "%02d:%02d", input.birthHour, input.birthMinute),
            birthDateText: dateFormatter.string(from: birthDate),
            birthCityName: AppLanguageOption.isChinese ? input.city.nameZh : input.city.nameEn,
            timeZoneId: input.city.timeZoneId
        )
    }

    private static func placement(
        _ id: AstrologyPlanetID,
        in placements: [AstrologyPlanetPlacement]
    ) throws -> AstrologyPlanetPlacement {
        guard let result = placements.first(where: { $0.planetId == id }) else {
            throw AstrologyEngineError.missingSigns
        }
        return result
    }

    private static func signFor(longitude: Double, signs: [ZodiacSign]) -> ZodiacSign {
        let index = Int(floor(normalizeAngle(longitude) / 30.0)) % 12
        return signs.sorted { $0.index < $1.index }[index]
    }

    private static func signFor(signId: String, signs: [ZodiacSign]) -> ZodiacSign {
        signs.first(where: { $0.id == signId }) ?? signs[0]
    }

    private static func majorAspects(from placements: [AstrologyPlanetPlacement]) -> [AstrologyAspect] {
        var result: [AstrologyAspect] = []
        for index in placements.indices {
            for comparison in placements.indices where comparison > index {
                let first = placements[index]
                let second = placements[comparison]
                let separation = absoluteSeparation(first.longitude, second.longitude)
                for type in AstrologyAspectType.allCases {
                    let orb = abs(separation - type.angle)
                    if orb <= type.orb {
                        result.append(
                            AstrologyAspect(
                                firstPlanetId: first.planetId,
                                secondPlanetId: second.planetId,
                                type: type,
                                orbDegrees: orb
                            )
                        )
                        break
                    }
                }
            }
        }
        return result
            .sorted {
                let lhsWeight = aspectPriority($0)
                let rhsWeight = aspectPriority($1)
                if lhsWeight == rhsWeight {
                    return $0.orbDegrees < $1.orbDegrees
                }
                return lhsWeight > rhsWeight
            }
            .prefix(6)
            .map { $0 }
    }

    private static func aspectPriority(_ aspect: AstrologyAspect) -> Int {
        let personalPlanets: Set<AstrologyPlanetID> = [.sun, .moon, .mercury, .venus, .mars]
        let first = personalPlanets.contains(aspect.firstPlanetId) ? 2 : 0
        let second = personalPlanets.contains(aspect.secondPlanetId) ? 2 : 0
        let exactness = max(0, Int((8.0 - aspect.orbDegrees) * 10))
        return first + second + exactness
    }

    private static func elementBalance(
        for placements: [AstrologyPlanetPlacement],
        risingSign: ZodiacSign,
        signs: [ZodiacSign]
    ) -> AstrologyElementBalance {
        var fire = 0
        var earth = 0
        var air = 0
        var water = 0

        let signById = Dictionary(uniqueKeysWithValues: signs.map { ($0.id, $0) })
        for placement in placements {
            let sign = signById[placement.signId] ?? risingSign
            switch sign.element {
            case "fire": fire += 1
            case "earth": earth += 1
            case "air": air += 1
            case "water": water += 1
            default: break
            }
        }

        switch risingSign.element {
        case "fire": fire += 1
        case "earth": earth += 1
        case "air": air += 1
        case "water": water += 1
        default: break
        }

        return AstrologyElementBalance(fire: fire, earth: earth, air: air, water: water)
    }

    private static func houseFocus(from placements: [AstrologyPlanetPlacement]) -> [AstrologyHouseFocus] {
        let counts = placements.reduce(into: [Int: Int]()) { partial, placement in
            guard let house = placement.house else { return }
            partial[house, default: 0] += 1
        }

        return counts
            .sorted { lhs, rhs in
                if lhs.value == rhs.value { return lhs.key < rhs.key }
                return lhs.value > rhs.value
            }
            .prefix(3)
            .map { entry in
                let house = entry.key
                let emphasisZh = entry.value >= 3 ? "这里聚集了较多行星，说明这是你人生反复投入、学习与成长的领域。" : "这里有明显的行星聚焦，往往是你会持续回应的生活主题。"
                let emphasisEn = entry.value >= 3
                    ? "Several planets cluster here, making this one of the chart's loudest life themes."
                    : "There is a noticeable planetary focus here, so this topic tends to keep calling for your attention."
                let info = houseDescription(for: house)
                return AstrologyHouseFocus(
                    house: house,
                    titleZh: info.titleZh,
                    titleEn: info.titleEn,
                    summaryZh: "\(info.summaryZh) \(emphasisZh)",
                    summaryEn: "\(info.summaryEn) \(emphasisEn)"
                )
            }
    }

    private static func houseDescription(for house: Int) -> (titleZh: String, titleEn: String, summaryZh: String, summaryEn: String) {
        switch house {
        case 1:
            return ("第一宫 · 自我呈现", "House 1 · Identity", "你给人的第一印象、行动方式与身体感受会更受关注。", "Your identity, first impression, and way of initiating things become more visible.")
        case 2:
            return ("第二宫 · 资源与价值", "House 2 · Resources", "金钱观、稳定感与你如何建立价值感，会是重要命题。", "Money patterns, security, and personal values become important topics.")
        case 3:
            return ("第三宫 · 沟通与学习", "House 3 · Communication", "表达、学习节奏与日常信息交换，会持续塑造你的生活。", "Communication, learning, and day-to-day exchanges strongly shape your life.")
        case 4:
            return ("第四宫 · 家与根基", "House 4 · Roots", "家庭、安全感与内在根基，是你需要反复整理的主题。", "Home, emotional roots, and inner security become recurring themes.")
        case 5:
            return ("第五宫 · 爱与创作", "House 5 · Romance", "恋爱表达、创造力与自我发光的方式，会格外重要。", "Romance, creativity, and self-expression become especially important.")
        case 6:
            return ("第六宫 · 工作与日常", "House 6 · Routine", "工作流程、健康节奏与长期习惯，会深刻影响你的状态。", "Work patterns, health routines, and daily habits significantly affect your wellbeing.")
        case 7:
            return ("第七宫 · 关系与合作", "House 7 · Partnership", "亲密关系、合作与镜像式成长，是重要的人生课题。", "Partnerships, intimacy, and growth through others become major life lessons.")
        case 8:
            return ("第八宫 · 深层链接", "House 8 · Depth", "共享资源、信任与深层情绪课题，会推动你蜕变。", "Shared resources, trust, and deeper emotional processes drive transformation.")
        case 9:
            return ("第九宫 · 信念与远方", "House 9 · Meaning", "求知、远行与人生信念，会带来更大的扩展感。", "Learning, travel, and belief systems become a strong source of expansion.")
        case 10:
            return ("第十宫 · 事业与方向", "House 10 · Calling", "事业方向、社会角色与长期目标，会更需要清晰定义。", "Career direction, public role, and long-term ambition need clear definition.")
        case 11:
            return ("第十一宫 · 社群与愿景", "House 11 · Community", "朋友、社群与未来愿景，会成为你不断连接的场域。", "Friends, communities, and future vision become meaningful points of connection.")
        default:
            return ("第十二宫 · 内在与疗愈", "House 12 · Inner World", "休息、潜意识与内在修复，是你需要认真照顾的面向。", "Rest, the inner world, and healing become areas that deserve conscious care.")
        }
    }

    private static func houseFor(longitude: Double, ascendantLongitude: Double) -> Int {
        let normalized = normalizeAngle(longitude - ascendantLongitude)
        return Int(floor(normalized / 30.0)) + 1
    }

    private static func ascendantLongitude(
        julianDay: Double,
        longitudeEast: Double,
        latitudeNorth: Double
    ) -> Double {
        let theta = deg2rad(localSiderealTimeDegrees(julianDay: julianDay, longitudeEast: longitudeEast))
        let epsilon = deg2rad(obliquityDegrees(daysSinceEpoch: julianDay - 2_451_543.5))
        let latitude = deg2rad(latitudeNorth)
        let asc = rad2deg(
            atan2(
                cos(theta),
                -(sin(theta) * cos(epsilon) + tan(latitude) * sin(epsilon))
            )
        )
        return normalizeAngle(asc)
    }

    private static func localSiderealTimeDegrees(julianDay: Double, longitudeEast: Double) -> Double {
        let t = (julianDay - 2_451_545.0) / 36_525.0
        let gmst = 280.460_618_37
            + 360.985_647_366_29 * (julianDay - 2_451_545.0)
            + 0.000_387_933 * t * t
            - (t * t * t) / 38_710_000.0
        return normalizeAngle(gmst + longitudeEast)
    }

    private static func obliquityDegrees(daysSinceEpoch d: Double) -> Double {
        23.4393 - 3.563e-7 * d
    }

    private static func isRetrograde(_ planet: AstrologyPlanetID, d: Double) -> Bool {
        guard planet != .sun, planet != .moon else { return false }
        let previous = planetLongitude(planet, d: d - 0.5)
        let next = planetLongitude(planet, d: d + 0.5)
        let delta = signedAngleDifference(from: previous, to: next)
        return delta < 0
    }

    private static func planetLongitude(_ planet: AstrologyPlanetID, d: Double) -> Double {
        switch planet {
        case .sun:
            return sunLongitude(d: d)
        case .moon:
            return moonLongitude(d: d)
        case .pluto:
            let sample = plutoSample(d: d)
            return sample.longitude
        default:
            let earth = earthHeliocentric(d: d)
            let heliocentric = heliocentricPlanet(planet, d: d)
            let xg = heliocentric.x - earth.x
            let yg = heliocentric.y - earth.y
            return normalizeAngle(rad2deg(atan2(yg, xg)))
        }
    }

    private static func sunLongitude(d: Double) -> Double {
        let earth = earthHeliocentric(d: d)
        return normalizeAngle(rad2deg(atan2(earth.y, earth.x)) + 180.0)
    }

    private static func moonLongitude(d: Double) -> Double {
        let n = normalizeAngle(125.1228 - 0.0529538083 * d)
        let i = 5.1454
        let w = normalizeAngle(318.0634 + 0.1643573223 * d)
        let a = 60.2666
        let e = 0.054900
        let m = normalizeAngle(115.3654 + 13.0649929509 * d)
        let sample = orbitalSample(
            ascendingNode: n,
            inclination: i,
            argumentOfPerihelion: w,
            semiMajorAxis: a,
            eccentricity: e,
            meanAnomaly: m
        )

        let sunMeanAnomaly = normalizeAngle(356.0470 + 0.9856002585 * d)
        let sunMeanLongitude = normalizeAngle(282.9404 + 4.70935e-5 * d + sunMeanAnomaly)
        let moonMeanLongitude = normalizeAngle(n + w + m)
        let meanElongation = normalizeAngle(moonMeanLongitude - sunMeanLongitude)
        let argumentOfLatitude = normalizeAngle(moonMeanLongitude - n)

        var longitude = rad2deg(atan2(sample.y, sample.x))
        var latitude = rad2deg(atan2(sample.z, sqrt(sample.x * sample.x + sample.y * sample.y)))

        longitude += -1.274 * sinDeg(m - 2.0 * meanElongation)
        longitude += 0.658 * sinDeg(2.0 * meanElongation)
        longitude += -0.186 * sinDeg(sunMeanAnomaly)
        longitude += -0.059 * sinDeg(2.0 * m - 2.0 * meanElongation)
        longitude += -0.057 * sinDeg(m - 2.0 * meanElongation + sunMeanAnomaly)
        longitude += 0.053 * sinDeg(m + 2.0 * meanElongation)
        longitude += 0.046 * sinDeg(2.0 * meanElongation - sunMeanAnomaly)
        longitude += 0.041 * sinDeg(m - sunMeanAnomaly)
        longitude += -0.035 * sinDeg(meanElongation)
        longitude += -0.031 * sinDeg(m + sunMeanAnomaly)
        longitude += -0.015 * sinDeg(2.0 * argumentOfLatitude - 2.0 * meanElongation)
        longitude += 0.011 * sinDeg(m - 4.0 * meanElongation)

        latitude += -0.173 * sinDeg(argumentOfLatitude - 2.0 * meanElongation)
        latitude += -0.055 * sinDeg(m - argumentOfLatitude - 2.0 * meanElongation)
        latitude += -0.046 * sinDeg(m + argumentOfLatitude - 2.0 * meanElongation)
        latitude += 0.033 * sinDeg(argumentOfLatitude + 2.0 * meanElongation)
        latitude += 0.017 * sinDeg(2.0 * m + argumentOfLatitude)

        _ = latitude
        return normalizeAngle(longitude)
    }

    private static func heliocentricPlanet(_ planet: AstrologyPlanetID, d: Double) -> Vector3 {
        switch planet {
        case .mercury:
            return orbitalSample(
                ascendingNode: 48.3313 + 3.24587e-5 * d,
                inclination: 7.0047 + 5.0e-8 * d,
                argumentOfPerihelion: 29.1241 + 1.01444e-5 * d,
                semiMajorAxis: 0.387098,
                eccentricity: 0.205635 + 5.59e-10 * d,
                meanAnomaly: 168.6562 + 4.0923344368 * d
            )
        case .venus:
            return orbitalSample(
                ascendingNode: 76.6799 + 2.46590e-5 * d,
                inclination: 3.3946 + 2.75e-8 * d,
                argumentOfPerihelion: 54.8910 + 1.38374e-5 * d,
                semiMajorAxis: 0.723330,
                eccentricity: 0.006773 - 1.302e-9 * d,
                meanAnomaly: 48.0052 + 1.6021302244 * d
            )
        case .mars:
            return orbitalSample(
                ascendingNode: 49.5574 + 2.11081e-5 * d,
                inclination: 1.8497 - 1.78e-8 * d,
                argumentOfPerihelion: 286.5016 + 2.92961e-5 * d,
                semiMajorAxis: 1.523688,
                eccentricity: 0.093405 + 2.516e-9 * d,
                meanAnomaly: 18.6021 + 0.5240207766 * d
            )
        case .jupiter:
            return orbitalSample(
                ascendingNode: 100.4542 + 2.76854e-5 * d,
                inclination: 1.3030 - 1.557e-7 * d,
                argumentOfPerihelion: 273.8777 + 1.64505e-5 * d,
                semiMajorAxis: 5.20256,
                eccentricity: 0.048498 + 4.469e-9 * d,
                meanAnomaly: 19.8950 + 0.0830853001 * d
            )
        case .saturn:
            return orbitalSample(
                ascendingNode: 113.6634 + 2.38980e-5 * d,
                inclination: 2.4886 - 1.081e-7 * d,
                argumentOfPerihelion: 339.3939 + 2.97661e-5 * d,
                semiMajorAxis: 9.55475,
                eccentricity: 0.055546 - 9.499e-9 * d,
                meanAnomaly: 316.9670 + 0.0334442282 * d
            )
        case .uranus:
            return orbitalSample(
                ascendingNode: 74.0005 + 1.3978e-5 * d,
                inclination: 0.7733 + 1.9e-8 * d,
                argumentOfPerihelion: 96.6612 + 3.0565e-5 * d,
                semiMajorAxis: 19.18171 - 1.55e-8 * d,
                eccentricity: 0.047318 + 7.45e-9 * d,
                meanAnomaly: 142.5905 + 0.011725806 * d
            )
        case .neptune:
            return orbitalSample(
                ascendingNode: 131.7806 + 3.0173e-5 * d,
                inclination: 1.7700 - 2.55e-7 * d,
                argumentOfPerihelion: 272.8461 - 6.027e-6 * d,
                semiMajorAxis: 30.05826 + 3.313e-8 * d,
                eccentricity: 0.008606 + 2.15e-9 * d,
                meanAnomaly: 260.2471 + 0.005995147 * d
            )
        case .pluto:
            return plutoSample(d: d).vector
        case .sun, .moon:
            return Vector3(x: 0, y: 0, z: 0)
        }
    }

    private static func earthHeliocentric(d: Double) -> Vector3 {
        orbitalSample(
            ascendingNode: 0,
            inclination: 0,
            argumentOfPerihelion: 282.9404 + 4.70935e-5 * d,
            semiMajorAxis: 1.000000,
            eccentricity: 0.016709 - 1.151e-9 * d,
            meanAnomaly: 356.0470 + 0.9856002585 * d
        )
    }

    private static func orbitalSample(
        ascendingNode: Double,
        inclination: Double,
        argumentOfPerihelion: Double,
        semiMajorAxis: Double,
        eccentricity: Double,
        meanAnomaly: Double
    ) -> Vector3 {
        let m = deg2rad(normalizeAngle(meanAnomaly))
        let eccentricAnomaly = solveEccentricAnomaly(meanAnomalyRadians: m, eccentricity: eccentricity)
        let xv = semiMajorAxis * (cos(eccentricAnomaly) - eccentricity)
        let yv = semiMajorAxis * (sqrt(1.0 - eccentricity * eccentricity) * sin(eccentricAnomaly))
        let trueAnomaly = atan2(yv, xv)
        let radius = sqrt(xv * xv + yv * yv)

        let n = deg2rad(ascendingNode)
        let i = deg2rad(inclination)
        let w = deg2rad(argumentOfPerihelion)
        let vw = trueAnomaly + w

        let xh = radius * (cos(n) * cos(vw) - sin(n) * sin(vw) * cos(i))
        let yh = radius * (sin(n) * cos(vw) + cos(n) * sin(vw) * cos(i))
        let zh = radius * (sin(vw) * sin(i))

        return Vector3(x: xh, y: yh, z: zh)
    }

    private static func plutoSample(d: Double) -> (longitude: Double, vector: Vector3) {
        let s = normalizeAngle(50.03 + 0.033459652 * d)
        let p = normalizeAngle(238.95 + 0.003968789 * d)

        let longitude = normalizeAngle(
            238.9508 + 0.00400703 * d
            - 19.799 * sinDeg(p) + 19.848 * cosDeg(p)
            + 0.897 * sinDeg(2 * p) - 4.956 * cosDeg(2 * p)
            + 0.610 * sinDeg(3 * p) + 1.211 * cosDeg(3 * p)
            - 0.341 * sinDeg(4 * p) - 0.190 * cosDeg(4 * p)
            + 0.128 * sinDeg(5 * p) - 0.034 * cosDeg(5 * p)
            - 0.038 * sinDeg(6 * p) + 0.031 * cosDeg(6 * p)
            + 0.020 * sinDeg(s - p) - 0.010 * cosDeg(s - p)
        )
        let latitude =
            -3.9082
            - 5.453 * sinDeg(p) - 14.975 * cosDeg(p)
            + 3.527 * sinDeg(2 * p) + 1.673 * cosDeg(2 * p)
            - 1.051 * sinDeg(3 * p) + 0.328 * cosDeg(3 * p)
            + 0.179 * sinDeg(4 * p) - 0.292 * cosDeg(4 * p)
            + 0.019 * sinDeg(5 * p) + 0.100 * cosDeg(5 * p)
            - 0.031 * sinDeg(6 * p) - 0.026 * cosDeg(6 * p)
            + 0.011 * cosDeg(s - p)
        let radius =
            40.72
            + 6.68 * sinDeg(p) + 6.90 * cosDeg(p)
            - 1.18 * sinDeg(2 * p) - 0.03 * cosDeg(2 * p)
            + 0.15 * sinDeg(3 * p) - 0.14 * cosDeg(3 * p)

        let longitudeRadians = deg2rad(longitude)
        let latitudeRadians = deg2rad(latitude)
        let vector = Vector3(
            x: radius * cos(longitudeRadians) * cos(latitudeRadians),
            y: radius * sin(longitudeRadians) * cos(latitudeRadians),
            z: radius * sin(latitudeRadians)
        )
        return (longitude, vector)
    }

    private static func solveEccentricAnomaly(meanAnomalyRadians m: Double, eccentricity e: Double) -> Double {
        var estimate = m + e * sin(m) * (1.0 + e * cos(m))
        for _ in 0..<5 {
            estimate = estimate - (estimate - e * sin(estimate) - m) / (1.0 - e * cos(estimate))
        }
        return estimate
    }

    private static func absoluteSeparation(_ lhs: Double, _ rhs: Double) -> Double {
        let raw = abs(normalizeAngle(lhs - rhs))
        return raw > 180 ? 360 - raw : raw
    }

    private static func signedAngleDifference(from start: Double, to end: Double) -> Double {
        let delta = normalizeAngle(end - start)
        return delta > 180 ? delta - 360 : delta
    }

    private static func normalizeAngle(_ value: Double) -> Double {
        var result = value.truncatingRemainder(dividingBy: 360.0)
        if result < 0 { result += 360.0 }
        return result
    }

    private static func deg2rad(_ value: Double) -> Double { value * .pi / 180.0 }
    private static func rad2deg(_ value: Double) -> Double { value * 180.0 / .pi }
    private static func sinDeg(_ value: Double) -> Double { sin(deg2rad(value)) }
    private static func cosDeg(_ value: Double) -> Double { cos(deg2rad(value)) }
}

private struct Vector3 {
    let x: Double
    let y: Double
    let z: Double
}
