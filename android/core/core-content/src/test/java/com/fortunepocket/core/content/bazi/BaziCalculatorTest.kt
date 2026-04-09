package com.fortunepocket.core.content.bazi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

class BaziCalculatorTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val hiddenStems by lazy {
        BaziDataLoader.parseHiddenStems(loadResource("hidden_stems.json"))
    }

    private val citiesById by lazy {
        BaziDataLoader.parseCities(loadResource("cities.json")).associateBy { it.id }
    }

    private val testVectors by lazy {
        json.decodeFromString<TestVectorsFile>(loadResource("test_vectors.json"))
    }

    @Test
    fun sharedTestVectorsMatchExpectedPillars() {
        for (case in testVectors.cases) {
            val chart = BaziCalculator.calculate(input = toInput(case), hiddenStemsMap = hiddenStems)
            case.expected.yearPillar?.let { expected ->
                assertEquals("${case.id} year pillar", expected, chart.yearPillar.nameZh)
            }
            case.expected.monthPillar?.let { expected ->
                assertEquals("${case.id} month pillar", expected, chart.monthPillar.nameZh)
            }
            case.expected.dayPillar?.let { expected ->
                assertEquals("${case.id} day pillar", expected, chart.dayPillar.nameZh)
            }
            case.expected.hourPillar?.let { expected ->
                assertEquals("${case.id} hour pillar", expected, chart.hourPillar?.nameZh)
            }
        }
    }

    @Test
    fun resolvesIanaTimeZoneWithDst() {
        val newYork = requireNotNull(citiesById["new_york"])
        val timing = BaziCalculator.resolveBirthTiming(
            BaziInput(
                birthYear = 2024,
                birthMonth = 7,
                birthDay = 1,
                birthHour = 12,
                birthMinute = 0,
                city = newYork,
                gender = Gender.FEMALE,
                useTrueSolarTime = false,
                distinguishLateZiHour = false
            )
        )

        val utc = timing.birthInstant.atOffset(ZoneOffset.UTC)
        assertEquals(16, utc.hour)
        assertEquals(0, utc.minute)
    }

    @Test
    fun trueSolarTimeAddsEquationOfTimeBeyondLongitudeOnlyCorrection() {
        val beijing = requireNotNull(citiesById["beijing"])
        val civilInput = BaziInput(
            birthYear = 2024,
            birthMonth = 2,
            birthDay = 4,
            birthHour = 12,
            birthMinute = 0,
            city = beijing,
            gender = Gender.MALE,
            useTrueSolarTime = false,
            distinguishLateZiHour = false
        )
        val solarInput = civilInput.copy(useTrueSolarTime = true)

        val civilTiming = BaziCalculator.resolveBirthTiming(civilInput)
        val solarTiming = BaziCalculator.resolveBirthTiming(solarInput)
        val longitudeOnly = LocalDateTime.ofInstant(
            civilTiming.birthInstant.plusMillis((beijing.longitudeEast * 4.0 * 60_000.0).roundToLong()),
            ZoneOffset.UTC
        )

        assertNotEquals(longitudeOnly.minute, solarTiming.effectiveDateTime.minute)
        assertNotEquals(civilTiming.effectiveDateTime, solarTiming.effectiveDateTime)
    }

    @Test
    fun lateZiAdvancesDayButKeepsZiHour() {
        val beijing = requireNotNull(citiesById["beijing"])
        val chart = BaziCalculator.calculate(
            input = BaziInput(
                birthYear = 2000,
                birthMonth = 1,
                birthDay = 7,
                birthHour = 23,
                birthMinute = 30,
                city = beijing,
                gender = Gender.MALE,
                useTrueSolarTime = false,
                distinguishLateZiHour = true
            ),
            hiddenStemsMap = hiddenStems
        )

        assertEquals("乙丑", chart.dayPillar.nameZh)
        assertEquals("丙子", chart.hourPillar?.nameZh)
    }

    private fun toInput(case: TestVectorCase): BaziInput {
        val birthDate = LocalDate.parse(case.birthDate)
        val timeParts = case.birthTime?.split(":") ?: emptyList()
        val hour = timeParts.getOrNull(0)?.toIntOrNull()
        val minute = timeParts.getOrNull(1)?.toIntOrNull()

        return BaziInput(
            birthYear = birthDate.year,
            birthMonth = birthDate.monthValue,
            birthDay = birthDate.dayOfMonth,
            birthHour = hour,
            birthMinute = minute,
            city = case.birthCityId?.let { citiesById[it] },
            gender = if (case.gender == "female") Gender.FEMALE else Gender.MALE,
            useTrueSolarTime = case.useTrueSolarTime,
            distinguishLateZiHour = false
        )
    }

    private fun loadResource(name: String): String {
        val classLoader = requireNotNull(javaClass.classLoader) {
            "Missing class loader for test resources"
        }
        return requireNotNull(classLoader.getResourceAsStream(name)) {
            "Missing test resource: $name"
        }.bufferedReader().use { it.readText() }
    }
}

@Serializable
private data class TestVectorsFile(
    val cases: List<TestVectorCase>
)

@Serializable
private data class TestVectorCase(
    val id: String,
    @SerialName("birth_date") val birthDate: String,
    @SerialName("birth_time") val birthTime: String? = null,
    @SerialName("birth_city_id") val birthCityId: String? = null,
    val gender: String = "male",
    @SerialName("use_true_solar_time") val useTrueSolarTime: Boolean = false,
    val expected: ExpectedPillars
)

@Serializable
private data class ExpectedPillars(
    @SerialName("year_pillar") val yearPillar: String? = null,
    @SerialName("month_pillar") val monthPillar: String? = null,
    @SerialName("day_pillar") val dayPillar: String? = null,
    @SerialName("hour_pillar") val hourPillar: String? = null
)
