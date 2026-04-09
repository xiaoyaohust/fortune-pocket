package com.fortunepocket.core.content

import com.fortunepocket.core.model.DrawnCard
import com.fortunepocket.core.model.SpreadsData
import com.fortunepocket.core.model.TarotCard
import com.fortunepocket.core.model.TarotCardsData
import com.fortunepocket.core.model.TarotQuestionTheme
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TarotReadingGeneratorTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val cardsData by lazy {
        json.decodeFromString<TarotCardsData>(loadResource("cards.json"))
    }

    private val spreadsData by lazy {
        json.decodeFromString<SpreadsData>(loadResource("spreads.json"))
    }

    private val readingTemplatesRaw by lazy { loadResource("reading-templates.json") }
    private val luckyItemsRaw by lazy { loadResource("items.json") }
    private val luckyColorsRaw by lazy { loadResource("colors.json") }

    // ---------------------------------------------------------------------------
    // Structural invariants
    // ---------------------------------------------------------------------------

    @Test
    fun generatedReadingHasCorrectCardCount() {
        val reading = buildTarotReading(cardsData, spreadsData, readingTemplatesRaw, luckyItemsRaw, luckyColorsRaw)
        val spread = spreadsData.spreads.first { it.id == "three_card" }
        assertEquals("drawn card count matches spread.cardCount", spread.cardCount, reading.drawnCards.size)
    }

    @Test
    fun noCardDrawnTwice() {
        val reading = buildTarotReading(cardsData, spreadsData, readingTemplatesRaw, luckyItemsRaw, luckyColorsRaw)
        val ids = reading.drawnCards.map { it.card.id }
        assertEquals("all drawn card IDs are unique", ids.size, ids.toSet().size)
    }

    @Test
    fun positionReadingsCountMatchesCardCount() {
        val reading = buildTarotReading(cardsData, spreadsData, readingTemplatesRaw, luckyItemsRaw, luckyColorsRaw)
        assertEquals(
            "positionReadings count equals drawnCards count",
            reading.drawnCards.size,
            reading.positionReadings.size
        )
    }

    @Test
    fun allStringFieldsNonEmpty() {
        val reading = buildTarotReading(cardsData, spreadsData, readingTemplatesRaw, luckyItemsRaw, luckyColorsRaw)
        assertTrue("overallEnergy not empty", reading.overallEnergy.isNotBlank())
        assertTrue("focusInsight not empty", reading.focusInsight.isNotBlank())
        assertTrue("advice not empty", reading.advice.isNotBlank())
        assertTrue("spreadName not empty", reading.spreadName.isNotBlank())
        assertTrue("spreadDescription not empty", reading.spreadDescription.isNotBlank())
        assertTrue("luckyItemName not empty", reading.luckyItemName.isNotBlank())
        assertTrue("luckyColorName not empty", reading.luckyColorName.isNotBlank())
        reading.positionReadings.forEachIndexed { i, pr ->
            assertTrue("positionReadings[$i].interpretation not empty", pr.interpretation.isNotBlank())
        }
    }

    @Test
    fun luckyNumberInRange() {
        repeat(10) {
            val reading = buildTarotReading(cardsData, spreadsData, readingTemplatesRaw, luckyItemsRaw, luckyColorsRaw)
            assertTrue(
                "luckyNumber ${reading.luckyNumber} should be in 1..9",
                reading.luckyNumber in 1..9
            )
        }
    }

    @Test
    fun allThemesGenerateWithoutError() {
        for (theme in TarotQuestionTheme.entries) {
            val reading = buildTarotReading(
                cardsData, spreadsData, readingTemplatesRaw, luckyItemsRaw, luckyColorsRaw, theme
            )
            assertNotNull("reading for theme $theme should not be null", reading)
            assertTrue("spreadId not empty for theme $theme", reading.spreadId.isNotBlank())
        }
    }

    @Test
    fun spreadIdMatchesTheme() {
        val reading = buildTarotReading(
            cardsData, spreadsData, readingTemplatesRaw, luckyItemsRaw, luckyColorsRaw,
            TarotQuestionTheme.LOVE
        )
        assertEquals("love theme uses love_three_card spread", "love_three_card", reading.spreadId)
    }

    // ---------------------------------------------------------------------------
    // Energy assessment
    // ---------------------------------------------------------------------------

    @Test
    fun assessEnergyReturnsLightWhenTwoPlusLightCards() {
        val lightCard = makeCard(energyUpright = "light", energyReversed = "shadow")
        val cards = listOf(
            DrawnCard(card = lightCard, isUpright = true),
            DrawnCard(card = lightCard, isUpright = true),
            DrawnCard(card = makeCard(energyUpright = "shadow", energyReversed = "shadow"), isUpright = true)
        )
        assertEquals("light", tarotAssessEnergy(cards))
    }

    @Test
    fun assessEnergyReturnsShadowWhenTwoPlusShadowCards() {
        val shadowCard = makeCard(energyUpright = "shadow", energyReversed = "light")
        val cards = listOf(
            DrawnCard(card = shadowCard, isUpright = true),
            DrawnCard(card = shadowCard, isUpright = true),
            DrawnCard(card = makeCard(energyUpright = "light", energyReversed = "light"), isUpright = true)
        )
        assertEquals("shadow", tarotAssessEnergy(cards))
    }

    @Test
    fun assessEnergyReturnsMixedForBalancedCards() {
        val cards = listOf(
            DrawnCard(card = makeCard(energyUpright = "light", energyReversed = "shadow"), isUpright = true),
            DrawnCard(card = makeCard(energyUpright = "shadow", energyReversed = "light"), isUpright = true),
            DrawnCard(card = makeCard(energyUpright = "mixed", energyReversed = "mixed"), isUpright = true)
        )
        assertEquals("mixed", tarotAssessEnergy(cards))
    }

    @Test
    fun assessEnergyAccountsForOrientation() {
        // A card with energyUpright=shadow, energyReversed=light drawn reversed → counts as light.
        val card = makeCard(energyUpright = "shadow", energyReversed = "light")
        val cards = listOf(
            DrawnCard(card = card, isUpright = false), // reversed → light
            DrawnCard(card = card, isUpright = false), // reversed → light
            DrawnCard(card = card, isUpright = true)   // upright  → shadow
        )
        assertEquals("light", tarotAssessEnergy(cards))
    }

    // ---------------------------------------------------------------------------
    // toDetail round-trip
    // ---------------------------------------------------------------------------

    @Test
    fun toDetailPreservesCardCount() {
        val reading = buildTarotReading(cardsData, spreadsData, readingTemplatesRaw, luckyItemsRaw, luckyColorsRaw)
        val detail = toTarotDetail(reading)
        assertEquals(reading.drawnCards.size, detail.drawnCards.size)
    }

    @Test
    fun toDetailPreservesCardIds() {
        val reading = buildTarotReading(cardsData, spreadsData, readingTemplatesRaw, luckyItemsRaw, luckyColorsRaw)
        val detail = toTarotDetail(reading)
        val originalIds = reading.drawnCards.map { it.card.id }
        val detailIds = detail.drawnCards.map { it.cardId }
        assertEquals(originalIds, detailIds)
    }

    @Test
    fun toDetailPreservesPositionReadingsCount() {
        val reading = buildTarotReading(cardsData, spreadsData, readingTemplatesRaw, luckyItemsRaw, luckyColorsRaw)
        val detail = toTarotDetail(reading)
        assertEquals(reading.positionReadings.size, detail.positionReadings.size)
    }

    @Test
    fun toDetailThemeIdMatchesEnum() {
        val reading = buildTarotReading(
            cardsData, spreadsData, readingTemplatesRaw, luckyItemsRaw, luckyColorsRaw,
            TarotQuestionTheme.CAREER
        )
        val detail = toTarotDetail(reading)
        assertEquals("career", detail.themeId)
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun loadResource(name: String): String {
        val classLoader = requireNotNull(javaClass.classLoader) { "Missing class loader" }
        return requireNotNull(classLoader.getResourceAsStream(name)) {
            "Missing test resource: $name"
        }.bufferedReader().use { it.readText() }
    }

    private fun makeCard(
        energyUpright: String = "mixed",
        energyReversed: String = "mixed"
    ) = TarotCard(
        id = "test_${energyUpright}_${energyReversed}",
        number = 1,
        arcana = "minor",
        suit = "cups",
        rank = "ace",
        nameZh = "测试牌",
        nameEn = "Test Card",
        symbol = "☆",
        colorPrimary = "#000000",
        colorAccent = "#FFFFFF",
        keywordsUprightZh = listOf("测试"),
        keywordsUprightEn = listOf("test"),
        keywordsReversedZh = listOf("逆测试"),
        keywordsReversedEn = listOf("reverse test"),
        meaningUprightZh = "这是一张测试牌的正位含义。",
        meaningUprightEn = "This is the upright meaning of a test card.",
        meaningReversedZh = "这是一张测试牌的逆位含义。",
        meaningReversedEn = "This is the reversed meaning of a test card.",
        energyUpright = energyUpright,
        energyReversed = energyReversed
    )
}
