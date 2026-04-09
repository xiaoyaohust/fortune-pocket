package com.fortunepocket.core.content.astrology

import com.fortunepocket.core.content.bazi.BaziDataLoader
import com.fortunepocket.core.content.bazi.BirthCity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AstrologyChartCalculatorTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val signs by lazy {
        json.decodeFromString<com.fortunepocket.core.model.ZodiacSignsData>(loadResource("signs.json")).signs
    }

    private val citiesById by lazy {
        BaziDataLoader.parseCities(loadResource("cities.json")).associateBy(BirthCity::id)
    }

    private val vectors by lazy {
        json.decodeFromString<AstrologyVectorFile>(loadResource("natal_chart_test_vectors.json"))
    }

    @Test
    fun sharedTestVectorsMatchExpectedPlacementsAndAspects() {
        for (case in vectors.cases) {
            val city = requireNotNull(citiesById[case.birthCityId]) { "Missing city for ${case.id}" }
            val snapshot = AstrologyChartCalculator.calculate(
                input = case.toInput(city),
                signs = signs,
                isZh = false
            )

            assertEquals("${case.id} sun sign", case.expected.sunSignId, snapshot.sunSign.id)
            assertEquals("${case.id} moon sign", case.expected.moonSignId, snapshot.moonSign.id)
            assertEquals("${case.id} rising sign", case.expected.risingSignId, snapshot.risingSign.id)
            assertEquals("${case.id} house focus", case.expected.houseFocus, snapshot.houseFocus.map { it.house })

            for (expectedPlacement in case.expected.placements) {
                val placement = requireNotNull(
                    snapshot.planetPlacements.firstOrNull { it.planetId.name.lowercase(Locale.US) == expectedPlacement.planetId }
                ) { "Missing placement ${expectedPlacement.planetId} for ${case.id}" }
                assertEquals("${case.id} ${expectedPlacement.planetId} sign", expectedPlacement.signId, placement.signId)
                assertEquals("${case.id} ${expectedPlacement.planetId} house", expectedPlacement.house, placement.house)
                assertEquals("${case.id} ${expectedPlacement.planetId} retrograde", expectedPlacement.retrograde, placement.isRetrograde)
            }

            val actualAspects = snapshot.majorAspects.map {
                AspectExpectation(
                    firstPlanetId = it.firstPlanetId.name.lowercase(Locale.US),
                    secondPlanetId = it.secondPlanetId.name.lowercase(Locale.US),
                    type = it.type.name.lowercase(Locale.US)
                )
            }
            assertEquals("${case.id} major aspects", case.expected.majorAspects, actualAspects)
        }
    }

    private fun AstrologyVectorCase.toInput(city: BirthCity): AstrologyInput {
        val dateParts = birthDate.split("-").map { it.toInt() }
        val timeParts = birthTime.split(":").map { it.toInt() }
        return AstrologyInput(
            birthYear = dateParts[0],
            birthMonth = dateParts[1],
            birthDay = dateParts[2],
            birthHour = timeParts[0],
            birthMinute = timeParts[1],
            city = city
        )
    }

    private fun loadResource(name: String): String {
        val classLoader = requireNotNull(javaClass.classLoader) { "Missing class loader for test resources" }
        return requireNotNull(classLoader.getResourceAsStream(name)) {
            "Missing test resource: $name"
        }.bufferedReader().use { it.readText() }
    }
}

@Serializable
private data class AstrologyVectorFile(
    val cases: List<AstrologyVectorCase>
)

@Serializable
private data class AstrologyVectorCase(
    val id: String,
    @SerialName("birth_date") val birthDate: String,
    @SerialName("birth_time") val birthTime: String,
    @SerialName("birth_city_id") val birthCityId: String,
    val expected: AstrologyExpectedChart
)

@Serializable
private data class AstrologyExpectedChart(
    @SerialName("sun_sign_id") val sunSignId: String,
    @SerialName("moon_sign_id") val moonSignId: String,
    @SerialName("rising_sign_id") val risingSignId: String,
    @SerialName("house_focus") val houseFocus: List<Int>,
    val placements: List<PlacementExpectation>,
    @SerialName("major_aspects") val majorAspects: List<AspectExpectation>
)

@Serializable
private data class PlacementExpectation(
    @SerialName("planet_id") val planetId: String,
    @SerialName("sign_id") val signId: String,
    val house: Int,
    val retrograde: Boolean
)

@Serializable
private data class AspectExpectation(
    @SerialName("first_planet_id") val firstPlanetId: String,
    @SerialName("second_planet_id") val secondPlanetId: String,
    val type: String
)
