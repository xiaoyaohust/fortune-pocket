package com.fortunepocket.core.content.bazi

import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * Computes solar term (节气) dates by numerically solving for the Sun's apparent
 * ecliptic longitude near an approximate Gregorian date.
 */
object SolarTermsEngine {

    data class MonthTerm(
        val nameZh: String,
        val nameEn: String,
        val longitude: Double,
        val monthBranch: Int
    )

    val monthTerms: List<MonthTerm> = listOf(
        MonthTerm("立春", "Lichun", 315.0, 2),
        MonthTerm("惊蛰", "Jingzhe", 345.0, 3),
        MonthTerm("清明", "Qingming", 15.0, 4),
        MonthTerm("立夏", "Lixia", 45.0, 5),
        MonthTerm("芒种", "Mangzhong", 75.0, 6),
        MonthTerm("小暑", "Xiaoshu", 105.0, 7),
        MonthTerm("立秋", "Liqiu", 135.0, 8),
        MonthTerm("白露", "Bailu", 165.0, 9),
        MonthTerm("寒露", "Hanlu", 195.0, 10),
        MonthTerm("立冬", "Lidong", 225.0, 11),
        MonthTerm("大雪", "Daxue", 255.0, 0),
        MonthTerm("小寒", "Xiaohan", 285.0, 1),
    )

    fun dateMillis(term: MonthTerm, year: Int): Long {
        val targetLongitude = normalizeAngle(term.longitude)
        val approxMillis = approximateTermMillis(targetLongitude, year)
        val (start, end) = findBracket(targetLongitude, approxMillis)
        return refineCrossing(targetLongitude, start, end)
    }

    fun lichunMillis(year: Int): Long = dateMillis(monthTerms[0], year)

    fun monthTermAndBaziYear(utcMillis: Long): Pair<Int, Int> {
        val gYear = utcYear(utcMillis)

        data class Candidate(val millis: Long, val termIdx: Int, val baziYear: Int)

        val candidates = mutableListOf<Candidate>()
        for (year in (gYear - 1)..(gYear + 1)) {
            for ((index, term) in monthTerms.withIndex()) {
                val millis = dateMillis(term, year)
                val baziYear = if (index == 0) year else if (millis < lichunMillis(year)) year - 1 else year
                candidates.add(Candidate(millis, index, baziYear))
            }
        }
        candidates.sortBy { it.millis }

        var current = candidates.first()
        for (candidate in candidates) {
            if (candidate.millis <= utcMillis) {
                current = candidate
            } else {
                break
            }
        }
        return current.termIdx to current.baziYear
    }

    fun nextTerm(afterMillis: Long): Pair<MonthTerm, Long> {
        val year = utcYear(afterMillis)
        for (candidateYear in year..(year + 1)) {
            for (term in monthTerms) {
                val millis = dateMillis(term, candidateYear)
                if (millis > afterMillis) return term to millis
            }
        }
        val term = monthTerms.first()
        return term to dateMillis(term, year + 2)
    }

    fun previousTerm(beforeMillis: Long): Pair<MonthTerm, Long> {
        val year = utcYear(beforeMillis)
        for (candidateYear in (year + 1) downTo (year - 1)) {
            for (term in monthTerms.asReversed()) {
                val millis = dateMillis(term, candidateYear)
                if (millis < beforeMillis) return term to millis
            }
        }
        val term = monthTerms.last()
        return term to dateMillis(term, year - 2)
    }

    private fun approximateTermMillis(targetLongitude: Double, year: Int): Long {
        val yearFraction = solarLongitudeToYearFraction(targetLongitude, year)
        val wholeYear = floor(yearFraction).toInt()
        val fraction = yearFraction - wholeYear.toDouble()
        val yearStart = LocalDateTime.of(wholeYear, 1, 1, 0, 0)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
        return yearStart + (fraction * 365.2422 * DAY_MILLIS).roundToLong()
    }

    private fun findBracket(targetLongitude: Double, approxMillis: Long): Pair<Long, Long> {
        var previousMillis = approxMillis - (15 * DAY_MILLIS)
        var previousDiff = signedLongitudeDifference(
            solarLongitudeDegrees(previousMillis),
            targetLongitude
        )

        var currentMillis = previousMillis + BRACKET_STEP_MILLIS
        repeat(120) {
            val currentDiff = signedLongitudeDifference(
                solarLongitudeDegrees(currentMillis),
                targetLongitude
            )
            if (hasSignChange(previousDiff, currentDiff)) {
                return previousMillis to currentMillis
            }
            previousMillis = currentMillis
            previousDiff = currentDiff
            currentMillis += BRACKET_STEP_MILLIS
        }

        return approxMillis - DAY_MILLIS to approxMillis + DAY_MILLIS
    }

    private fun refineCrossing(targetLongitude: Double, startMillis: Long, endMillis: Long): Long {
        var low = startMillis
        var high = endMillis
        repeat(48) {
            val mid = low + (high - low) / 2
            val lowDiff = signedLongitudeDifference(solarLongitudeDegrees(low), targetLongitude)
            val midDiff = signedLongitudeDifference(solarLongitudeDegrees(mid), targetLongitude)
            if (hasSignChange(lowDiff, midDiff)) {
                high = mid
            } else {
                low = mid
            }
        }
        return low + (high - low) / 2
    }

    /**
     * Solar apparent longitude approximation in degrees.
     *
     * Sufficient for term-boundary solving after binary refinement.
     */
    private fun solarLongitudeDegrees(utcMillis: Long): Double {
        val julianDay = (utcMillis / DAY_MILLIS.toDouble()) + UNIX_EPOCH_JULIAN_DAY
        val t = (julianDay - 2451545.0) / 36525.0

        val meanLongitude = normalizeAngle(
            280.46646 + (36000.76983 * t) + (0.0003032 * t * t)
        )
        val meanAnomaly = normalizeAngle(
            357.52911 + (35999.05029 * t) - (0.0001537 * t * t)
        )
        val meanAnomalyRad = degreesToRadians(meanAnomaly)

        val equationOfCenter =
            (1.914602 - (0.004817 * t) - (0.000014 * t * t)) * sin(meanAnomalyRad) +
                (0.019993 - (0.000101 * t)) * sin(2.0 * meanAnomalyRad) +
                0.000289 * sin(3.0 * meanAnomalyRad)

        val trueLongitude = meanLongitude + equationOfCenter
        val omega = 125.04 - (1934.136 * t)
        val apparentLongitude = trueLongitude - 0.00569 - (0.00478 * sin(degreesToRadians(omega)))
        return normalizeAngle(apparentLongitude)
    }

    private fun solarLongitudeToYearFraction(longitude: Double, nearYear: Int): Double {
        val daysSinceEquinox = (if (longitude < 0) longitude + 360 else longitude) / 360.0 * 365.2422
        val equinoxDayOfYear = 79.0
        var dayOfYear = equinoxDayOfYear + daysSinceEquinox
        if (dayOfYear > 365.2422) dayOfYear -= 365.2422
        return nearYear.toDouble() + dayOfYear / 365.2422
    }

    private fun utcYear(millis: Long): Int {
        return LocalDateTime.ofEpochSecond(millis / 1000, 0, ZoneOffset.UTC).year
    }

    private fun normalizeAngle(value: Double): Double {
        var angle = value % 360.0
        if (angle < 0) angle += 360.0
        return angle
    }

    private fun signedLongitudeDifference(current: Double, target: Double): Double {
        var delta = normalizeAngle(current) - normalizeAngle(target)
        if (delta > 180.0) delta -= 360.0
        if (delta <= -180.0) delta += 360.0
        return delta
    }

    private fun hasSignChange(first: Double, second: Double): Boolean {
        return first == 0.0 || second == 0.0 || (first < 0 && second > 0) || (first > 0 && second < 0)
    }

    private fun degreesToRadians(value: Double): Double = value * Math.PI / 180.0

    private const val DAY_MILLIS = 86_400_000L
    private const val BRACKET_STEP_MILLIS = 6L * 60L * 60L * 1000L
    private const val UNIX_EPOCH_JULIAN_DAY = 2440587.5
}
