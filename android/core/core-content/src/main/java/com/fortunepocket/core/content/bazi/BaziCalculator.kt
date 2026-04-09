package com.fortunepocket.core.content.bazi

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * Computes a full 四柱八字 (Four Pillars) chart from a [BaziInput].
 *
 * References:
 *   - Day pillar anchor: JDN 2451551 = 2000-01-07 = 甲子日
 *   - Solar terms: [SolarTermsEngine] (Meeus algorithm, ±5 min accuracy)
 *   - Ten gods: derived from 5-element relationship + yin/yang vs day master
 *   - Hidden stems: loaded from `hidden_stems.json` via [BaziDataLoader]
 *   - Major cycles: direction = year stem polarity xor gender; start age = days to nearest term ÷ 3
 */
object BaziCalculator {

    fun calculate(context: Context, input: BaziInput): BaziChart {
        return calculate(input, BaziDataLoader.loadHiddenStems(context))
    }

    internal fun calculate(
        input: BaziInput,
        hiddenStemsMap: Map<Int, List<HiddenStemEntry>>
    ): BaziChart {
        val resolvedTiming = resolveBirthTiming(input)
        val lateZiResult = applyLateZi(
            effectiveDateTime = resolvedTiming.effectiveDateTime,
            distinguishLateZi = input.distinguishLateZiHour,
            hasTime = input.birthHour != null
        )

        // Year/month boundaries depend on the actual birth instant vs solar terms.
        val (termIndex, baziYear) = SolarTermsEngine.monthTermAndBaziYear(
            resolvedTiming.birthInstant.toEpochMilli()
        )
        val yearPillar = yearPillar(baziYear)
        val monthPillar = monthPillar(termIndex, yearPillar.normalizedStem)

        // Day/hour depend on local civil time or true solar time.
        val dayPillar = dayPillar(lateZiResult.effectiveDateTime.toLocalDate())
        val hourPillar = if (input.birthHour != null) {
            hourPillar(
                localHour = lateZiResult.effectiveDateTime.hour,
                dayStemIndex = dayPillar.normalizedStem
            )
        } else {
            null
        }

        val hiddenStems = mutableMapOf<Int, List<HiddenStemEntry>>()
        for (p in listOfNotNull(yearPillar, monthPillar, dayPillar, hourPillar)) {
            val branch = p.normalizedBranch
            if (!hiddenStems.containsKey(branch)) {
                hiddenStems[branch] = hiddenStemsMap[branch] ?: emptyList()
            }
        }

        val fiveElements = computeFiveElements(
            pillars = listOfNotNull(yearPillar, monthPillar, dayPillar, hourPillar),
            hiddenStems = hiddenStems
        )

        val tenGods = computeTenGods(
            dayMaster = dayPillar.normalizedStem,
            yearPillar = yearPillar,
            monthPillar = monthPillar,
            dayPillar = dayPillar,
            hourPillar = hourPillar,
            hiddenStems = hiddenStems
        )

        val (cycles, startingAge, direction) = computeMajorCycles(
            input = input,
            birthInstantMillis = resolvedTiming.birthInstant.toEpochMilli(),
            yearStemIndex = yearPillar.normalizedStem,
            monthPillar = monthPillar
        )

        return BaziChart(
            input = input,
            yearPillar = yearPillar,
            monthPillar = monthPillar,
            dayPillar = dayPillar,
            hourPillar = hourPillar,
            hiddenStems = hiddenStems,
            tenGods = tenGods,
            fiveElements = fiveElements,
            majorCycles = cycles,
            startingAge = startingAge,
            cycleDirection = direction
        )
    }

    internal data class ResolvedBirthTiming(
        val birthInstant: Instant,
        val effectiveDateTime: LocalDateTime
    )

    private data class LateZiResult(val effectiveDateTime: LocalDateTime)

    internal fun resolveBirthTiming(input: BaziInput): ResolvedBirthTiming {
        val zoneId = resolvedZoneId(input.city)
        val birthHour = input.birthHour ?: 12
        val birthMinute = input.birthMinute ?: 0
        val civilDateTime = LocalDateTime.of(
            input.birthYear,
            input.birthMonth,
            input.birthDay,
            birthHour,
            birthMinute
        )
        val civilOffset = resolvedCivilOffset(input.city, zoneId, civilDateTime)
        val birthInstant = civilDateTime.toInstant(civilOffset)

        val effectiveDateTime = if (input.useTrueSolarTime && input.birthHour != null && input.city != null) {
            trueSolarLocalDateTime(
                birthInstant = birthInstant,
                civilDateTime = civilDateTime,
                city = input.city
            )
        } else {
            civilDateTime
        }

        return ResolvedBirthTiming(
            birthInstant = birthInstant,
            effectiveDateTime = effectiveDateTime
        )
    }

    private fun resolvedZoneId(city: BirthCity?): ZoneId {
        val zoneId = city?.timeZoneId ?: DefaultBaziTimeZoneId
        return runCatching { ZoneId.of(zoneId) }
            .getOrElse { ZoneId.of(DefaultBaziTimeZoneId) }
    }

    /**
     * Use the dataset's standard offset as the base legal time, then apply IANA DST
     * adjustments on top. This keeps historical Chinese dates on UTC+8 instead of
     * drifting to pre-standard local mean time offsets, while still honoring DST
     * for cities like New York or London.
     */
    private fun resolvedCivilOffset(
        city: BirthCity?,
        zoneId: ZoneId,
        civilDateTime: LocalDateTime
    ): ZoneOffset {
        val standardOffsetSeconds = ((city?.utcOffsetHours ?: 8.0) * 3600.0).roundToInt()
        val standardOffset = ZoneOffset.ofTotalSeconds(standardOffsetSeconds)
        val approximateInstant = civilDateTime.toInstant(standardOffset)
        val dstSeconds = zoneId.rules.getDaylightSavings(approximateInstant).seconds.toInt()
        return ZoneOffset.ofTotalSeconds(standardOffsetSeconds + dstSeconds)
    }

    /**
     * Converts the civil birth instant into local apparent solar time.
     *
     * Formula:
     *   true solar time = UTC + longitude * 4min + equation_of_time
     */
    private fun trueSolarLocalDateTime(
        birthInstant: Instant,
        civilDateTime: LocalDateTime,
        city: BirthCity
    ): LocalDateTime {
        val solarOffsetMinutes = (city.longitudeEast * 4.0) + equationOfTimeMinutes(civilDateTime)
        val solarInstant = birthInstant.plusMillis((solarOffsetMinutes * 60_000.0).roundToLong())
        return LocalDateTime.ofInstant(solarInstant, ZoneOffset.UTC)
    }

    /**
     * NOAA approximation of equation of time, in minutes.
     */
    private fun equationOfTimeMinutes(civilDateTime: LocalDateTime): Double {
        val dayOfYear = civilDateTime.dayOfYear
        val fractionalHour = civilDateTime.hour + (civilDateTime.minute / 60.0)
        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1 + (fractionalHour - 12.0) / 24.0)
        return 229.18 * (
            0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2.0 * gamma) -
                0.040849 * sin(2.0 * gamma)
            )
    }

    private fun applyLateZi(
        effectiveDateTime: LocalDateTime,
        distinguishLateZi: Boolean,
        hasTime: Boolean
    ): LateZiResult {
        if (!hasTime || !distinguishLateZi || effectiveDateTime.hour != 23) {
            return LateZiResult(effectiveDateTime)
        }
        return LateZiResult(effectiveDateTime.plusDays(1))
    }

    private fun yearPillar(baziYear: Int): Pillar {
        val offset = baziYear - 4
        val stem = ((offset % 10) + 10) % 10
        val branch = ((offset % 12) + 12) % 12
        return Pillar(stem, branch)
    }

    private fun monthPillar(termIndex: Int, yearStemIndex: Int): Pillar {
        val monthBranch = SolarTermsEngine.monthTerms[termIndex].monthBranch
        val yinMonthStem = ((yearStemIndex % 5) * 2 + 2) % 10
        val monthStem = (yinMonthStem + termIndex) % 10
        return Pillar(monthStem, monthBranch)
    }

    /** JDN reference: 2000-01-07 = 甲子日, JDN = 2451551 */
    private fun dayPillar(localDate: LocalDate): Pillar {
        val jdn = julianDayNumber(localDate)
        val offset = jdn - 2451551
        val idx = ((offset % 60) + 60) % 60
        return Pillar(idx % 10, idx % 12)
    }

    private fun julianDayNumber(localDate: LocalDate): Int {
        val y = localDate.year
        val m = localDate.monthValue
        val d = localDate.dayOfMonth

        val a = (14 - m) / 12
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
    }

    private fun hourPillar(localHour: Int, dayStemIndex: Int): Pillar {
        val hourBranch = if (localHour == 23) 0 else (localHour + 1) / 2
        val hourStem = ((dayStemIndex % 5) * 2 + hourBranch) % 10
        return Pillar(hourStem, hourBranch)
    }

    private fun tenGodOf(dayMaster: Int, targetStem: Int): TenGodEntry {
        val dmElement = StemElements[dayMaster]
        val tgtElement = StemElements[targetStem]
        val samePolarity = StemPolarity[dayMaster] == StemPolarity[targetStem]

        val rel = elementRelationship(from = dmElement, to = tgtElement)
        val godIndex = when (rel) {
            0 -> if (samePolarity) 0 else 1
            1 -> if (samePolarity) 2 else 3
            2 -> if (samePolarity) 8 else 9
            3 -> if (samePolarity) 4 else 5
            4 -> if (samePolarity) 6 else 7
            else -> 0
        }
        return TenGodEntry(TenGodNamesZh[godIndex], TenGodNamesEn[godIndex])
    }

    private fun elementRelationship(from: Int, to: Int): Int {
        if (from == to) return 0
        if ((from + 1) % 5 == to) return 1
        if ((to + 1) % 5 == from) return 2
        val controls = mapOf(0 to 2, 2 to 4, 4 to 1, 1 to 3, 3 to 0)
        if (controls[from] == to) return 3
        if (controls[to] == from) return 4
        return 0
    }

    private fun computeTenGods(
        dayMaster: Int,
        yearPillar: Pillar,
        monthPillar: Pillar,
        dayPillar: Pillar,
        hourPillar: Pillar?,
        hiddenStems: Map<Int, List<HiddenStemEntry>>
    ): TenGods {
        fun mainHidden(branchIdx: Int): Int {
            return hiddenStems[branchIdx]
                ?.firstOrNull { it.weight == HiddenStemWeight.MAIN }
                ?.stemIndex
                ?: 0
        }

        return TenGods(
            yearStemGod = tenGodOf(dayMaster, yearPillar.normalizedStem),
            monthStemGod = tenGodOf(dayMaster, monthPillar.normalizedStem),
            hourStemGod = hourPillar?.let { tenGodOf(dayMaster, it.normalizedStem) },
            yearBranchGod = tenGodOf(dayMaster, mainHidden(yearPillar.normalizedBranch)),
            monthBranchGod = tenGodOf(dayMaster, mainHidden(monthPillar.normalizedBranch)),
            dayBranchGod = tenGodOf(dayMaster, mainHidden(dayPillar.normalizedBranch)),
            hourBranchGod = hourPillar?.let { tenGodOf(dayMaster, mainHidden(it.normalizedBranch)) }
        )
    }

    private fun computeFiveElements(
        pillars: List<Pillar>,
        hiddenStems: Map<Int, List<HiddenStemEntry>>
    ): FiveElementStrength {
        val strength = FiveElementStrength()
        for (p in pillars) {
            strength.add(StemElements[p.normalizedStem], 2)
            hiddenStems[p.normalizedBranch]?.forEach { h ->
                val weight = when (h.weight) {
                    HiddenStemWeight.MAIN -> 3
                    HiddenStemWeight.MID -> 2
                    HiddenStemWeight.MINOR -> 1
                }
                strength.add(StemElements[h.stemIndex], weight)
            }
        }
        return strength
    }

    private data class CycleResult(
        val cycles: List<MajorCycle>,
        val startingAge: Int,
        val direction: String
    )

    private fun computeMajorCycles(
        input: BaziInput,
        birthInstantMillis: Long,
        yearStemIndex: Int,
        monthPillar: Pillar
    ): CycleResult {
        val isYangYear = StemPolarity[yearStemIndex] == 0
        val isMale = input.gender == Gender.MALE
        val forward = isYangYear == isMale
        val direction = if (forward) "顺排" else "逆排"

        val (_, refMillis) = if (forward) {
            SolarTermsEngine.nextTerm(afterMillis = birthInstantMillis)
        } else {
            SolarTermsEngine.previousTerm(beforeMillis = birthInstantMillis)
        }

        val daysDiff = abs((refMillis - birthInstantMillis).toDouble()) / 86_400_000.0
        val startingAge = maxOf(1, (daysDiff / 3.0).roundToInt())

        val baseIdx = jiaziIndex(monthPillar.normalizedStem, monthPillar.normalizedBranch)
        val cycles = (1..10).map { i ->
            val delta = if (forward) i else -i
            val cycleIdx = ((baseIdx + delta) % 60 + 60) % 60
            MajorCycle(
                startAge = startingAge + (i - 1) * 10,
                pillar = Pillar(cycleIdx % 10, cycleIdx % 12)
            )
        }

        return CycleResult(cycles, startingAge, direction)
    }

    private fun jiaziIndex(stem: Int, branch: Int): Int {
        for (x in 0 until 60) {
            if (x % 10 == stem && x % 12 == branch) return x
        }
        return 0
    }
}

object BaziDataLoader {

    @Serializable
    private data class HiddenStemsFile(
        val hidden_stems: List<HiddenStemRecord>
    )

    @Serializable
    private data class HiddenStemRecord(
        val branch: Int,
        val stems: List<StemRecord>
    )

    @Serializable
    private data class StemRecord(
        val stem: Int,
        val weight: String
    )

    @Serializable
    private data class CitiesFile(
        val version: String? = null,
        val note: String? = null,
        val cities: List<CityRecord>
    )

    @Serializable
    private data class CityRecord(
        val id: String,
        val name_zh: String,
        val name_en: String,
        val country: String,
        val search_aliases: List<String> = emptyList(),
        val latitude_north: Double,
        val longitude_east: Double,
        val time_zone_id: String? = null,
        val utc_offset_hours: Double
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun loadHiddenStems(context: Context): Map<Int, List<HiddenStemEntry>> {
        val raw = context.assets.open("data/bazi/hidden_stems.json")
            .bufferedReader()
            .readText()
        return parseHiddenStems(raw)
    }

    internal fun parseHiddenStems(raw: String): Map<Int, List<HiddenStemEntry>> {
        val file = json.decodeFromString<HiddenStemsFile>(raw)
        return file.hidden_stems.associate { record ->
            record.branch to record.stems.mapNotNull { stem ->
                val weight = when (stem.weight) {
                    "main" -> HiddenStemWeight.MAIN
                    "mid" -> HiddenStemWeight.MID
                    "minor" -> HiddenStemWeight.MINOR
                    else -> null
                }
                weight?.let { HiddenStemEntry(stem.stem, it) }
            }
        }
    }

    fun loadCities(context: Context): List<BirthCity> {
        val raw = context.assets.open("data/bazi/cities.json")
            .bufferedReader()
            .readText()
        return parseCities(raw)
    }

    internal fun parseCities(raw: String): List<BirthCity> {
        val file = json.decodeFromString<CitiesFile>(raw)
        return file.cities.map {
            BirthCity(
                id = it.id,
                nameZh = it.name_zh,
                nameEn = it.name_en,
                country = it.country,
                searchAliases = it.search_aliases,
                latitudeNorth = it.latitude_north,
                longitudeEast = it.longitude_east,
                timeZoneId = it.time_zone_id ?: DefaultBaziTimeZoneId,
                utcOffsetHours = it.utc_offset_hours
            )
        }
    }
}
