package com.fortunepocket.core.content

import android.content.Context
import com.fortunepocket.core.model.BaziPersonalityTemplates
import com.fortunepocket.core.model.BranchesData
import com.fortunepocket.core.model.DailyQuotesData
import com.fortunepocket.core.model.SpreadsData
import com.fortunepocket.core.model.StemsData
import com.fortunepocket.core.model.TarotCardsData
import com.fortunepocket.core.model.ZodiacSignsData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads shared-content JSON files from assets.
 *
 * The shared-content/data directory is included in assets via app/build.gradle.kts:
 *   sourceSets { main { assets.srcDirs("src/main/assets", "../../shared-content") } }
 *
 * Files are accessed as "data/tarot/cards.json", "data/astrology/signs.json", etc.
 * All parsing results are cached in memory after first load.
 */
@Singleton
class ContentLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Lazy-loaded caches
    private var _tarotCards: TarotCardsData? = null
    private var _spreads: SpreadsData? = null
    private var _zodiacSigns: ZodiacSignsData? = null
    private var _stems: StemsData? = null
    private var _branches: BranchesData? = null
    private var _baziTemplates: BaziPersonalityTemplates? = null
    private var _dailyQuotes: DailyQuotesData? = null
    private var _astrologyTemplatesRaw: String? = null
    private var _readingTemplatesRaw: String? = null
    private var _luckyItemsRaw: String? = null
    private var _luckyColorsRaw: String? = null

    fun loadTarotCards(): TarotCardsData {
        return _tarotCards ?: loadAsset<TarotCardsData>("data/tarot/cards.json").also { _tarotCards = it }
    }

    fun loadSpreads(): SpreadsData {
        return _spreads ?: loadAsset<SpreadsData>("data/tarot/spreads.json").also { _spreads = it }
    }

    /** Raw JSON string for reading-templates.json (decoded privately by TarotReadingGenerator). */
    fun loadReadingTemplatesRaw(): String {
        return _readingTemplatesRaw ?: loadRawAsset("data/tarot/reading-templates.json").also { _readingTemplatesRaw = it }
    }

    /** Raw JSON string for lucky items (decoded privately by TarotReadingGenerator). */
    fun loadLuckyItemsRaw(): String {
        return _luckyItemsRaw ?: loadRawAsset("data/lucky/items.json").also { _luckyItemsRaw = it }
    }

    /** Raw JSON string for lucky colors (decoded privately by TarotReadingGenerator). */
    fun loadLuckyColorsRaw(): String {
        return _luckyColorsRaw ?: loadRawAsset("data/lucky/colors.json").also { _luckyColorsRaw = it }
    }

    fun loadZodiacSigns(): ZodiacSignsData {
        return _zodiacSigns ?: loadAsset<ZodiacSignsData>("data/astrology/signs.json").also { _zodiacSigns = it }
    }

    /** Raw JSON string for astrology daily templates. */
    fun loadAstrologyTemplatesRaw(): String {
        return _astrologyTemplatesRaw ?: loadRawAsset("data/astrology/daily-templates.json")
            .also { _astrologyTemplatesRaw = it }
    }

    fun loadHeavenlyStems(): StemsData {
        return _stems ?: loadAsset<StemsData>("data/bazi/stems.json").also { _stems = it }
    }

    fun loadEarthlyBranches(): BranchesData {
        return _branches ?: loadAsset<BranchesData>("data/bazi/branches.json").also { _branches = it }
    }

    fun loadBaziTemplates(): BaziPersonalityTemplates {
        return _baziTemplates ?: loadAsset<BaziPersonalityTemplates>("data/bazi/personality-templates.json")
            .also { _baziTemplates = it }
    }

    fun loadDailyQuotes(): DailyQuotesData {
        return _dailyQuotes ?: loadAsset<DailyQuotesData>("data/quotes/daily-quotes.json").also { _dailyQuotes = it }
    }

    private inline fun <reified T> loadAsset(path: String): T {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        return json.decodeFromString(text)
    }

    private fun loadRawAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }
}
