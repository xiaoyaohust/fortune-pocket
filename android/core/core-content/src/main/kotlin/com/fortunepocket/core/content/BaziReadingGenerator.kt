package com.fortunepocket.core.content

import com.fortunepocket.core.model.BaziReading
import com.fortunepocket.core.model.ElementPersonality
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaziReadingGenerator @Inject constructor(
    private val contentLoader: ContentLoader
) {

    fun generate(
        birthDateMillis: Long,
        birthHour: Int?
    ): BaziReading {
        val stems = contentLoader.loadHeavenlyStems().stems.sortedBy { it.index }
        val branches = contentLoader.loadEarthlyBranches().branches
        val templates = contentLoader.loadBaziTemplates()
        val isZh = Locale.getDefault().language == "zh"

        val birthDate = Instant.ofEpochMilli(birthDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val referenceDate = LocalDate.of(1900, 1, 1)
        val daysBetween = ChronoUnit.DAYS.between(referenceDate, birthDate).toInt()
        val stem = stems[positiveModulo(daysBetween, stems.size)]
        val branch = branches.firstOrNull { it.month == birthDate.monthValue }
            ?: branches[positiveModulo(birthDate.monthValue - 1, branches.size)]

        val introPool = if (isZh) templates.overallIntroZh else templates.overallIntroEn
        val intro = if (introPool.isEmpty()) {
            ""
        } else {
            introPool[positiveModulo(daysBetween + branch.index, introPool.size)]
        }

        val phasePool = if (isZh) templates.phaseAdviceZh else templates.phaseAdviceEn
        val phaseSeed = (LocalDate.now().dayOfYear * 31) + (stem.index * 7) + (birthHour ?: 12)
        val elementSummary = elementDescription(
            templates.elementPersonality,
            stem.element,
            isZh
        )
        val monthNuance = if (isZh) branch.nuanceZh else branch.nuanceEn
        val combinedElementDescription = if (isZh) {
            "$elementSummary\n\n月令补充：$monthNuance"
        } else {
            "$elementSummary\n\nSeasonal nuance: $monthNuance"
        }

        return BaziReading(
            birthDate = birthDateMillis,
            birthHour = birthHour,
            stemId = stem.id,
            stemNameZh = stem.nameZh,
            stemNameEn = stem.nameEn,
            stemColorPrimary = stem.colorPrimary,
            disclaimer = if (isZh) templates.disclaimer_zh else templates.disclaimer_en,
            overallPersonality = listOf(
                intro,
                if (isZh) stem.personalityZh else stem.personalityEn
            ).filter { it.isNotBlank() }.joinToString("\n\n"),
            love = if (isZh) stem.loveZh else stem.loveEn,
            career = if (isZh) stem.careerZh else stem.careerEn,
            wealth = if (isZh) stem.wealthZh else stem.wealthEn,
            phaseAdvice = if (phasePool.isEmpty()) {
                ""
            } else {
                phasePool[positiveModulo(phaseSeed, phasePool.size)]
            },
            elementDescription = combinedElementDescription
        )
    }

    private fun elementDescription(
        elementPersonality: ElementPersonality,
        element: String,
        isZh: Boolean
    ): String {
        return when (element) {
            "wood" -> if (isZh) elementPersonality.wood_zh else elementPersonality.wood_en
            "fire" -> if (isZh) elementPersonality.fire_zh else elementPersonality.fire_en
            "earth" -> if (isZh) elementPersonality.earth_zh else elementPersonality.earth_en
            "metal" -> if (isZh) elementPersonality.metal_zh else elementPersonality.metal_en
            "water" -> if (isZh) elementPersonality.water_zh else elementPersonality.water_en
            else -> if (isZh) elementPersonality.wood_zh else elementPersonality.wood_en
        }
    }

    private fun positiveModulo(value: Int, mod: Int): Int {
        return ((value % mod) + mod) % mod
    }
}
