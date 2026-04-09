package com.fortunepocket.core.content.astrology

import com.fortunepocket.core.content.bazi.BirthCity
import com.fortunepocket.core.model.AstrologyAspect
import com.fortunepocket.core.model.AstrologyAspectType
import com.fortunepocket.core.model.AstrologyElementBalance
import com.fortunepocket.core.model.AstrologyHouseFocus
import com.fortunepocket.core.model.AstrologyPlanetID
import com.fortunepocket.core.model.AstrologyPlanetPlacement
import com.fortunepocket.core.model.ZodiacSign
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

data class AstrologyInput(
    val birthYear: Int,
    val birthMonth: Int,
    val birthDay: Int,
    val birthHour: Int,
    val birthMinute: Int,
    val city: BirthCity
)

data class AstrologyChartSnapshot(
    val sunSign: ZodiacSign,
    val moonSign: ZodiacSign,
    val risingSign: ZodiacSign,
    val planetPlacements: List<AstrologyPlanetPlacement>,
    val majorAspects: List<AstrologyAspect>,
    val elementBalance: AstrologyElementBalance,
    val houseFocus: List<AstrologyHouseFocus>,
    val birthDateText: String,
    val birthTimeText: String,
    val birthCityName: String,
    val timeZoneId: String
)

object AstrologyChartCalculator {

    fun calculate(
        input: AstrologyInput,
        signs: List<ZodiacSign>,
        isZh: Boolean
    ): AstrologyChartSnapshot {
        require(signs.isNotEmpty()) { "Missing zodiac sign definitions" }

        val zone = TimeZone.getTimeZone(input.city.timeZoneId)
        val calendar = Calendar.getInstance(zone).apply {
            set(Calendar.YEAR, input.birthYear)
            set(Calendar.MONTH, input.birthMonth - 1)
            set(Calendar.DAY_OF_MONTH, input.birthDay)
            set(Calendar.HOUR_OF_DAY, input.birthHour)
            set(Calendar.MINUTE, input.birthMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val birthDate = calendar.time
        val julianDay = birthDate.time / 86_400_000.0 + 2_440_587.5
        val d = julianDay - 2_451_543.5
        val ascendant = ascendantLongitude(
            julianDay = julianDay,
            longitudeEast = input.city.longitudeEast,
            latitudeNorth = input.city.latitudeNorth
        )

        val orderedSigns = signs.sortedBy { it.index }
        val placements = AstrologyPlanetID.entries.map { planet ->
            val longitude = normalizeAngle(planetLongitude(planet, d))
            val sign = orderedSigns[(floor(longitude / 30.0).toInt()).mod(12)]
            AstrologyPlanetPlacement(
                planetId = planet,
                longitude = longitude,
                signId = sign.id,
                signNameZh = sign.nameZh,
                signNameEn = sign.nameEn,
                house = houseFor(longitude, ascendant),
                isRetrograde = isRetrograde(planet, d)
            )
        }

        val sunSign = orderedSigns.first { it.id == placements.first { p -> p.planetId == AstrologyPlanetID.SUN }.signId }
        val moonSign = orderedSigns.first { it.id == placements.first { p -> p.planetId == AstrologyPlanetID.MOON }.signId }
        val risingSign = orderedSigns[(floor(ascendant / 30.0).toInt()).mod(12)]

        val dateFormat = DateFormat.getDateInstance(DateFormat.LONG, if (isZh) Locale.SIMPLIFIED_CHINESE else Locale.US)
        dateFormat.timeZone = zone

        return AstrologyChartSnapshot(
            sunSign = sunSign,
            moonSign = moonSign,
            risingSign = risingSign,
            planetPlacements = placements,
            majorAspects = majorAspects(placements),
            elementBalance = elementBalance(placements, risingSign, orderedSigns),
            houseFocus = houseFocus(placements),
            birthDateText = dateFormat.format(birthDate),
            birthTimeText = String.format(Locale.US, "%02d:%02d", input.birthHour, input.birthMinute),
            birthCityName = if (isZh) input.city.nameZh else input.city.nameEn,
            timeZoneId = input.city.timeZoneId
        )
    }

    private fun majorAspects(placements: List<AstrologyPlanetPlacement>): List<AstrologyAspect> {
        val result = mutableListOf<AstrologyAspect>()
        for (i in placements.indices) {
            for (j in i + 1 until placements.size) {
                val first = placements[i]
                val second = placements[j]
                val separation = absoluteSeparation(first.longitude, second.longitude)
                for (type in AstrologyAspectType.entries) {
                    val orb = abs(separation - type.angle)
                    if (orb <= type.orb) {
                        result += AstrologyAspect(
                            firstPlanetId = first.planetId,
                            secondPlanetId = second.planetId,
                            type = type,
                            orbDegrees = orb
                        )
                        break
                    }
                }
            }
        }

        return result
            .sortedWith(
                compareByDescending<AstrologyAspect> { aspectPriority(it) }
                    .thenBy { it.orbDegrees }
            )
            .take(6)
    }

    private fun aspectPriority(aspect: AstrologyAspect): Int {
        val personal = setOf(
            AstrologyPlanetID.SUN,
            AstrologyPlanetID.MOON,
            AstrologyPlanetID.MERCURY,
            AstrologyPlanetID.VENUS,
            AstrologyPlanetID.MARS
        )
        val first = if (personal.contains(aspect.firstPlanetId)) 2 else 0
        val second = if (personal.contains(aspect.secondPlanetId)) 2 else 0
        val exactness = max(0, ((8.0 - aspect.orbDegrees) * 10.0).toInt())
        return first + second + exactness
    }

    private fun elementBalance(
        placements: List<AstrologyPlanetPlacement>,
        risingSign: ZodiacSign,
        signs: List<ZodiacSign>
    ): AstrologyElementBalance {
        val signById = signs.associateBy { it.id }
        var fire = 0
        var earth = 0
        var air = 0
        var water = 0

        fun add(element: String) {
            when (element) {
                "fire" -> fire += 1
                "earth" -> earth += 1
                "air" -> air += 1
                "water" -> water += 1
            }
        }

        placements.forEach { placement ->
            add(signById[placement.signId]?.element ?: risingSign.element)
        }
        add(risingSign.element)

        return AstrologyElementBalance(fire = fire, earth = earth, air = air, water = water)
    }

    private fun houseFocus(placements: List<AstrologyPlanetPlacement>): List<AstrologyHouseFocus> {
        return placements
            .mapNotNull { it.house }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .take(3)
            .map { entry ->
                val info = houseDescription(entry.key)
                val emphasisZh = if (entry.value >= 3) {
                    "这里聚集了较多行星，说明这是你人生反复投入、学习与成长的领域。"
                } else {
                    "这里有明显的行星聚焦，往往是你会持续回应的生活主题。"
                }
                val emphasisEn = if (entry.value >= 3) {
                    "Several planets cluster here, making this one of the chart's loudest life themes."
                } else {
                    "There is a noticeable planetary focus here, so this topic keeps calling for your attention."
                }
                AstrologyHouseFocus(
                    house = entry.key,
                    titleZh = info.first,
                    titleEn = info.second,
                    summaryZh = "${info.third} $emphasisZh",
                    summaryEn = "${info.fourth} $emphasisEn"
                )
            }
    }

    private fun houseDescription(house: Int): Quadruple<String, String, String, String> = when (house) {
        1 -> Quadruple("第一宫 · 自我呈现", "House 1 · Identity", "你给人的第一印象、行动方式与身体感受会更受关注。", "Your identity, first impression, and way of initiating things become more visible.")
        2 -> Quadruple("第二宫 · 资源与价值", "House 2 · Resources", "金钱观、稳定感与你如何建立价值感，会是重要命题。", "Money patterns, security, and personal values become important topics.")
        3 -> Quadruple("第三宫 · 沟通与学习", "House 3 · Communication", "表达、学习节奏与日常信息交换，会持续塑造你的生活。", "Communication, learning, and day-to-day exchanges strongly shape your life.")
        4 -> Quadruple("第四宫 · 家与根基", "House 4 · Roots", "家庭、安全感与内在根基，是你需要反复整理的主题。", "Home, emotional roots, and inner security become recurring themes.")
        5 -> Quadruple("第五宫 · 爱与创作", "House 5 · Romance", "恋爱表达、创造力与自我发光的方式，会格外重要。", "Romance, creativity, and self-expression become especially important.")
        6 -> Quadruple("第六宫 · 工作与日常", "House 6 · Routine", "工作流程、健康节奏与长期习惯，会深刻影响你的状态。", "Work patterns, health routines, and daily habits significantly affect your wellbeing.")
        7 -> Quadruple("第七宫 · 关系与合作", "House 7 · Partnership", "亲密关系、合作与镜像式成长，是重要的人生课题。", "Partnerships, intimacy, and growth through others become major life lessons.")
        8 -> Quadruple("第八宫 · 深层链接", "House 8 · Depth", "共享资源、信任与深层情绪课题，会推动你蜕变。", "Shared resources, trust, and deeper emotional processes drive transformation.")
        9 -> Quadruple("第九宫 · 信念与远方", "House 9 · Meaning", "求知、远行与人生信念，会带来更大的扩展感。", "Learning, travel, and belief systems become a strong source of expansion.")
        10 -> Quadruple("第十宫 · 事业与方向", "House 10 · Calling", "事业方向、社会角色与长期目标，会更需要清晰定义。", "Career direction, public role, and long-term ambition need clear definition.")
        11 -> Quadruple("第十一宫 · 社群与愿景", "House 11 · Community", "朋友、社群与未来愿景，会成为你不断连接的场域。", "Friends, communities, and future vision become meaningful points of connection.")
        else -> Quadruple("第十二宫 · 内在与疗愈", "House 12 · Inner World", "休息、潜意识与内在修复，是你需要认真照顾的面向。", "Rest, the inner world, and healing become areas that deserve conscious care.")
    }

    private fun houseFor(longitude: Double, ascendantLongitude: Double): Int {
        val normalized = normalizeAngle(longitude - ascendantLongitude)
        return floor(normalized / 30.0).toInt() + 1
    }

    private fun ascendantLongitude(julianDay: Double, longitudeEast: Double, latitudeNorth: Double): Double {
        val theta = deg2rad(localSiderealTimeDegrees(julianDay, longitudeEast))
        val epsilon = deg2rad(obliquityDegrees(julianDay - 2_451_543.5))
        val latitude = deg2rad(latitudeNorth)
        val asc = rad2deg(
            atan2(
                cos(theta),
                -(sin(theta) * cos(epsilon) + tan(latitude) * sin(epsilon))
            )
        )
        return normalizeAngle(asc)
    }

    private fun localSiderealTimeDegrees(julianDay: Double, longitudeEast: Double): Double {
        val t = (julianDay - 2_451_545.0) / 36_525.0
        val gmst = 280.46061837 +
            360.98564736629 * (julianDay - 2_451_545.0) +
            0.000387933 * t * t -
            (t * t * t) / 38_710_000.0
        return normalizeAngle(gmst + longitudeEast)
    }

    private fun obliquityDegrees(d: Double): Double = 23.4393 - 3.563e-7 * d

    private fun isRetrograde(planet: AstrologyPlanetID, d: Double): Boolean {
        if (planet == AstrologyPlanetID.SUN || planet == AstrologyPlanetID.MOON) return false
        val previous = planetLongitude(planet, d - 0.5)
        val next = planetLongitude(planet, d + 0.5)
        return signedAngleDifference(previous, next) < 0.0
    }

    private fun planetLongitude(planet: AstrologyPlanetID, d: Double): Double = when (planet) {
        AstrologyPlanetID.SUN -> sunLongitude(d)
        AstrologyPlanetID.MOON -> moonLongitude(d)
        AstrologyPlanetID.PLUTO -> plutoSample(d).first
        else -> {
            val earth = earthHeliocentric(d)
            val planetVector = heliocentricPlanet(planet, d)
            normalizeAngle(rad2deg(atan2(planetVector.y - earth.y, planetVector.x - earth.x)))
        }
    }

    private fun sunLongitude(d: Double): Double {
        val earth = earthHeliocentric(d)
        return normalizeAngle(rad2deg(atan2(earth.y, earth.x)) + 180.0)
    }

    private fun moonLongitude(d: Double): Double {
        val n = normalizeAngle(125.1228 - 0.0529538083 * d)
        val i = 5.1454
        val w = normalizeAngle(318.0634 + 0.1643573223 * d)
        val a = 60.2666
        val e = 0.054900
        val m = normalizeAngle(115.3654 + 13.0649929509 * d)
        val sample = orbitalSample(n, i, w, a, e, m)

        val sunMeanAnomaly = normalizeAngle(356.0470 + 0.9856002585 * d)
        val sunMeanLongitude = normalizeAngle(282.9404 + 4.70935e-5 * d + sunMeanAnomaly)
        val moonMeanLongitude = normalizeAngle(n + w + m)
        val meanElongation = normalizeAngle(moonMeanLongitude - sunMeanLongitude)
        val argumentOfLatitude = normalizeAngle(moonMeanLongitude - n)

        var longitude = rad2deg(atan2(sample.y, sample.x))
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
        return normalizeAngle(longitude)
    }

    private fun heliocentricPlanet(planet: AstrologyPlanetID, d: Double): Vector3 = when (planet) {
        AstrologyPlanetID.MERCURY -> orbitalSample(48.3313 + 3.24587e-5 * d, 7.0047 + 5.0e-8 * d, 29.1241 + 1.01444e-5 * d, 0.387098, 0.205635 + 5.59e-10 * d, 168.6562 + 4.0923344368 * d)
        AstrologyPlanetID.VENUS -> orbitalSample(76.6799 + 2.46590e-5 * d, 3.3946 + 2.75e-8 * d, 54.8910 + 1.38374e-5 * d, 0.723330, 0.006773 - 1.302e-9 * d, 48.0052 + 1.6021302244 * d)
        AstrologyPlanetID.MARS -> orbitalSample(49.5574 + 2.11081e-5 * d, 1.8497 - 1.78e-8 * d, 286.5016 + 2.92961e-5 * d, 1.523688, 0.093405 + 2.516e-9 * d, 18.6021 + 0.5240207766 * d)
        AstrologyPlanetID.JUPITER -> orbitalSample(100.4542 + 2.76854e-5 * d, 1.3030 - 1.557e-7 * d, 273.8777 + 1.64505e-5 * d, 5.20256, 0.048498 + 4.469e-9 * d, 19.8950 + 0.0830853001 * d)
        AstrologyPlanetID.SATURN -> orbitalSample(113.6634 + 2.38980e-5 * d, 2.4886 - 1.081e-7 * d, 339.3939 + 2.97661e-5 * d, 9.55475, 0.055546 - 9.499e-9 * d, 316.9670 + 0.0334442282 * d)
        AstrologyPlanetID.URANUS -> orbitalSample(74.0005 + 1.3978e-5 * d, 0.7733 + 1.9e-8 * d, 96.6612 + 3.0565e-5 * d, 19.18171 - 1.55e-8 * d, 0.047318 + 7.45e-9 * d, 142.5905 + 0.011725806 * d)
        AstrologyPlanetID.NEPTUNE -> orbitalSample(131.7806 + 3.0173e-5 * d, 1.7700 - 2.55e-7 * d, 272.8461 - 6.027e-6 * d, 30.05826 + 3.313e-8 * d, 0.008606 + 2.15e-9 * d, 260.2471 + 0.005995147 * d)
        AstrologyPlanetID.PLUTO -> plutoSample(d).second
        AstrologyPlanetID.SUN, AstrologyPlanetID.MOON -> Vector3(0.0, 0.0, 0.0)
    }

    private fun earthHeliocentric(d: Double): Vector3 =
        orbitalSample(0.0, 0.0, 282.9404 + 4.70935e-5 * d, 1.0, 0.016709 - 1.151e-9 * d, 356.0470 + 0.9856002585 * d)

    private fun orbitalSample(
        ascendingNode: Double,
        inclination: Double,
        argumentOfPerihelion: Double,
        semiMajorAxis: Double,
        eccentricity: Double,
        meanAnomaly: Double
    ): Vector3 {
        val m = deg2rad(normalizeAngle(meanAnomaly))
        val eccentricAnomaly = solveEccentricAnomaly(m, eccentricity)
        val xv = semiMajorAxis * (cos(eccentricAnomaly) - eccentricity)
        val yv = semiMajorAxis * (sqrt(1.0 - eccentricity * eccentricity) * sin(eccentricAnomaly))
        val trueAnomaly = atan2(yv, xv)
        val radius = sqrt(xv * xv + yv * yv)

        val n = deg2rad(ascendingNode)
        val i = deg2rad(inclination)
        val w = deg2rad(argumentOfPerihelion)
        val vw = trueAnomaly + w

        return Vector3(
            x = radius * (cos(n) * cos(vw) - sin(n) * sin(vw) * cos(i)),
            y = radius * (sin(n) * cos(vw) + cos(n) * sin(vw) * cos(i)),
            z = radius * (sin(vw) * sin(i))
        )
    }

    private fun plutoSample(d: Double): Pair<Double, Vector3> {
        val s = normalizeAngle(50.03 + 0.033459652 * d)
        val p = normalizeAngle(238.95 + 0.003968789 * d)
        val longitude = normalizeAngle(
            238.9508 + 0.00400703 * d
                - 19.799 * sinDeg(p) + 19.848 * cosDeg(p)
                + 0.897 * sinDeg(2 * p) - 4.956 * cosDeg(2 * p)
                + 0.610 * sinDeg(3 * p) + 1.211 * cosDeg(3 * p)
                - 0.341 * sinDeg(4 * p) - 0.190 * cosDeg(4 * p)
                + 0.128 * sinDeg(5 * p) - 0.034 * cosDeg(5 * p)
                - 0.038 * sinDeg(6 * p) + 0.031 * cosDeg(6 * p)
                + 0.020 * sinDeg(s - p) - 0.010 * cosDeg(s - p)
        )
        val latitude =
            -3.9082
                - 5.453 * sinDeg(p) - 14.975 * cosDeg(p)
                + 3.527 * sinDeg(2 * p) + 1.673 * cosDeg(2 * p)
                - 1.051 * sinDeg(3 * p) + 0.328 * cosDeg(3 * p)
                + 0.179 * sinDeg(4 * p) - 0.292 * cosDeg(4 * p)
                + 0.019 * sinDeg(5 * p) + 0.100 * cosDeg(5 * p)
                - 0.031 * sinDeg(6 * p) - 0.026 * cosDeg(6 * p)
                + 0.011 * cosDeg(s - p)
        val radius =
            40.72 + 6.68 * sinDeg(p) + 6.90 * cosDeg(p)
                - 1.18 * sinDeg(2 * p) - 0.03 * cosDeg(2 * p)
                + 0.15 * sinDeg(3 * p) - 0.14 * cosDeg(3 * p)

        val lon = deg2rad(longitude)
        val lat = deg2rad(latitude)
        return longitude to Vector3(
            x = radius * cos(lon) * cos(lat),
            y = radius * sin(lon) * cos(lat),
            z = radius * sin(lat)
        )
    }

    private fun solveEccentricAnomaly(meanAnomalyRadians: Double, eccentricity: Double): Double {
        var estimate = meanAnomalyRadians + eccentricity * sin(meanAnomalyRadians) * (1.0 + eccentricity * cos(meanAnomalyRadians))
        repeat(5) {
            estimate -= (estimate - eccentricity * sin(estimate) - meanAnomalyRadians) / (1.0 - eccentricity * cos(estimate))
        }
        return estimate
    }

    private fun absoluteSeparation(lhs: Double, rhs: Double): Double {
        val raw = abs(normalizeAngle(lhs - rhs))
        return if (raw > 180.0) 360.0 - raw else raw
    }

    private fun signedAngleDifference(start: Double, end: Double): Double {
        val delta = normalizeAngle(end - start)
        return if (delta > 180.0) delta - 360.0 else delta
    }

    private fun normalizeAngle(value: Double): Double {
        var result = value % 360.0
        if (result < 0) result += 360.0
        return result
    }

    private fun deg2rad(value: Double): Double = value * PI / 180.0
    private fun rad2deg(value: Double): Double = value * 180.0 / PI
    private fun sinDeg(value: Double): Double = sin(deg2rad(value))
    private fun cosDeg(value: Double): Double = cos(deg2rad(value))
}

private data class Vector3(val x: Double, val y: Double, val z: Double)
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
