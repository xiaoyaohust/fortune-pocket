package com.fortunepocket.core.content.bazi

// MARK: - Stem / Branch constants

val StemNames    = listOf("甲","乙","丙","丁","戊","己","庚","辛","壬","癸")
val StemNamesEn  = listOf("Jia","Yi","Bing","Ding","Wu","Ji","Geng","Xin","Ren","Gui")
/** 五行 element for each stem (0=wood,1=fire,2=earth,3=metal,4=water) */
val StemElements = listOf(0,0,1,1,2,2,3,3,4,4)
/** Polarity for each stem (0=yang, 1=yin) */
val StemPolarity = listOf(0,1,0,1,0,1,0,1,0,1)

val BranchNames   = listOf("子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥")
val BranchNamesEn = listOf("Zi","Chou","Yin","Mao","Chen","Si","Wu","Wei","Shen","You","Xu","Hai")
val BranchElements = listOf(4,2,0,0,2,1,1,2,3,3,2,4)
val BranchPolarity = listOf(0,1,0,1,0,1,0,1,0,1,0,1)

val ElementNamesZh = listOf("木","火","土","金","水")
val ElementNamesEn = listOf("Wood","Fire","Earth","Metal","Water")

val TenGodNamesZh = listOf("比肩","劫财","食神","伤官","偏财","正财","七杀","正官","偏印","正印")
val TenGodNamesEn = listOf(
    "Peer","Rob Wealth","Eating God","Hurt Officer",
    "Indirect Wealth","Direct Wealth","7-Kill","Officer","Indirect Seal","Seal"
)

// MARK: - Input

enum class Gender { MALE, FEMALE }

const val DefaultBaziTimeZoneId = "Asia/Shanghai"

data class BirthCity(
    val id:             String,
    val nameZh:         String,
    val nameEn:         String,
    val country:        String,
    val searchAliases:  List<String> = emptyList(),
    val latitudeNorth:  Double,
    val longitudeEast:  Double,
    val timeZoneId:     String,
    val utcOffsetHours: Double
)

data class BaziInput(
    val birthYear:   Int,
    val birthMonth:  Int,
    val birthDay:    Int,
    /** null = unknown */
    val birthHour:   Int?,
    val birthMinute: Int?,
    val city:        BirthCity?,
    val gender:      Gender,
    val useTrueSolarTime:      Boolean,
    val distinguishLateZiHour: Boolean
)

// MARK: - Pillar

data class Pillar(val stemIndex: Int, val branchIndex: Int) {
    val normalizedStem:   Int get() = ((stemIndex   % 10) + 10) % 10
    val normalizedBranch: Int get() = ((branchIndex % 12) + 12) % 12

    val stemZh:   String get() = StemNames[normalizedStem]
    val branchZh: String get() = BranchNames[normalizedBranch]
    val nameZh:   String get() = stemZh + branchZh

    val stemEn:   String get() = StemNamesEn[normalizedStem]
    val branchEn: String get() = BranchNamesEn[normalizedBranch]
    val nameEn:   String get() = stemEn + branchEn

    val element:  Int get() = StemElements[normalizedStem]
    val polarity: Int get() = StemPolarity[normalizedStem]
}

fun pillar(stem: Int, branch: Int): Pillar =
    Pillar(((stem % 10) + 10) % 10, ((branch % 12) + 12) % 12)

// MARK: - Hidden Stem

enum class HiddenStemWeight(val labelZh: String) {
    MAIN("主气"), MID("中气"), MINOR("余气")
}

data class HiddenStemEntry(val stemIndex: Int, val weight: HiddenStemWeight)

// MARK: - Ten God

data class TenGodEntry(val nameZh: String, val nameEn: String)

// MARK: - Five Elements

data class FiveElementStrength(
    var wood:  Int = 0,
    var fire:  Int = 0,
    var earth: Int = 0,
    var metal: Int = 0,
    var water: Int = 0
) {
    val total: Int get() = wood + fire + earth + metal + water

    operator fun get(element: Int): Int = when(element) {
        0 -> wood; 1 -> fire; 2 -> earth; 3 -> metal; 4 -> water; else -> 0
    }

    fun add(element: Int, value: Int = 1) {
        when(element) {
            0 -> wood  += value
            1 -> fire  += value
            2 -> earth += value
            3 -> metal += value
            4 -> water += value
        }
    }
}

// MARK: - Major Cycle

data class MajorCycle(val startAge: Int, val pillar: Pillar)

// MARK: - 时辰 (Shichen)

data class Shichen(val branchIndex: Int) {
    val labelZh: String get() = BranchNames[branchIndex] + "时"
    val labelEn: String get() = BranchNamesEn[branchIndex] + " Hour"

    companion object {
        fun from(hour: Int, distinguishLateZi: Boolean): Pair<Shichen, Boolean> {
            val isLateZi = distinguishLateZi && hour == 23
            val branch   = if (hour == 23) 0 else (hour + 1) / 2
            return Shichen(branch) to isLateZi
        }
    }
}

// MARK: - Ten Gods Collection

data class TenGods(
    val yearStemGod:    TenGodEntry?,
    val monthStemGod:   TenGodEntry?,
    val hourStemGod:    TenGodEntry?,
    val yearBranchGod:  TenGodEntry,
    val monthBranchGod: TenGodEntry,
    val dayBranchGod:   TenGodEntry,
    val hourBranchGod:  TenGodEntry?
)

// MARK: - Full Chart

data class BaziChart(
    val input:          BaziInput,
    val yearPillar:     Pillar,
    val monthPillar:    Pillar,
    val dayPillar:      Pillar,
    val hourPillar:     Pillar?,
    /** branch index → hidden stems */
    val hiddenStems:    Map<Int, List<HiddenStemEntry>>,
    val tenGods:        TenGods,
    val fiveElements:   FiveElementStrength,
    val majorCycles:    List<MajorCycle>,
    val startingAge:    Int,
    val cycleDirection: String   // "顺排" or "逆排"
) {
    val allPillars: List<Pillar> get() =
        listOfNotNull(yearPillar, monthPillar, dayPillar, hourPillar)
}
