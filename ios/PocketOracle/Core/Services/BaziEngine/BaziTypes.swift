import Foundation

// MARK: - Stem / Branch constants

/// 天干 Heavenly Stems (index 0–9: 甲乙丙丁戊己庚辛壬癸)
let StemNames    = ["甲","乙","丙","丁","戊","己","庚","辛","壬","癸"]
let StemNamesEn  = ["Jia","Yi","Bing","Ding","Wu","Ji","Geng","Xin","Ren","Gui"]
/// 五行 element for each stem (0=wood,1=fire,2=earth,3=metal,4=water)
let StemElements = [0,0,1,1,2,2,3,3,4,4]
/// Polarity for each stem (0=yang,1=yin)
let StemPolarity = [0,1,0,1,0,1,0,1,0,1]

/// 地支 Earthly Branches (index 0–11: 子丑寅卯辰巳午未申酉戌亥)
let BranchNames   = ["子","丑","寅","卯","辰","巳","午","未","申","酉","戌","亥"]
let BranchNamesEn = ["Zi","Chou","Yin","Mao","Chen","Si","Wu","Wei","Shen","You","Xu","Hai"]
/// 五行 element for each branch
let BranchElements = [4,2,0,0,2,1,1,2,3,3,2,4]
/// Polarity for each branch
let BranchPolarity = [0,1,0,1,0,1,0,1,0,1,0,1]

/// 五行 element names
let ElementNamesZh = ["木","火","土","金","水"]
let ElementNamesEn = ["Wood","Fire","Earth","Metal","Water"]

/// 十神 Ten God names (zh)
let TenGodNamesZh = ["比肩","劫财","食神","伤官","偏财","正财","七杀","正官","偏印","正印"]
let TenGodNamesEn = ["Peer","Rob Wealth","Eating God","Hurt Officer",
                     "Indirect Wealth","Direct Wealth","7-Kill","Officer","Indirect Seal","Seal"]

// MARK: - Input

enum Gender { case male, female }

let DefaultBaziTimeZoneId = "Asia/Shanghai"

struct BirthCity: Codable, Identifiable {
    let id:               String
    let nameZh:           String
    let nameEn:           String
    let country:          String
    let searchAliases:    [String]
    let latitudeNorth:    Double
    let longitudeEast:    Double
    let timeZoneId:       String
    let utcOffsetHours:   Double

    enum CodingKeys: String, CodingKey {
        case id, country
        case nameZh         = "name_zh"
        case nameEn         = "name_en"
        case searchAliases  = "search_aliases"
        case latitudeNorth  = "latitude_north"
        case longitudeEast  = "longitude_east"
        case timeZoneId     = "time_zone_id"
        case utcOffsetHours = "utc_offset_hours"
    }

    init(
        id: String,
        nameZh: String,
        nameEn: String,
        country: String,
        searchAliases: [String] = [],
        latitudeNorth: Double,
        longitudeEast: Double,
        timeZoneId: String,
        utcOffsetHours: Double
    ) {
        self.id = id
        self.nameZh = nameZh
        self.nameEn = nameEn
        self.country = country
        self.searchAliases = searchAliases
        self.latitudeNorth = latitudeNorth
        self.longitudeEast = longitudeEast
        self.timeZoneId = timeZoneId
        self.utcOffsetHours = utcOffsetHours
    }
}

struct CitiesData: Codable {
    let version: String
    let cities: [BirthCity]
}

struct BaziInput {
    /// Gregorian birth date (year/month/day components, no time zone attached)
    var birthYear:  Int
    var birthMonth: Int
    var birthDay:   Int
    /// Local clock time at birth location (hour 0–23, minute 0–59). nil = unknown.
    var birthHour:   Int?
    var birthMinute: Int?
    /// Birth location. nil = use standard timezone/no true-solar-time correction.
    var city: BirthCity?
    var gender: Gender
    /// Apply true solar time (真太阳时) correction using longitude.
    var useTrueSolarTime: Bool
    /// Treat 23:00–23:59 as late-子时 of the NEXT calendar day (晚子时归次日).
    var distinguishLateZiHour: Bool
}

// MARK: - Pillar

struct Pillar: Equatable {
    let stemIndex:   Int  // 0–9
    let branchIndex: Int  // 0–11

    var stemZh:    String { StemNames[stemIndex]   }
    var branchZh:  String { BranchNames[branchIndex] }
    var nameZh:    String { stemZh + branchZh }

    var stemEn:    String { StemNamesEn[stemIndex]   }
    var branchEn:  String { BranchNamesEn[branchIndex] }
    var nameEn:    String { stemEn + branchEn }

    var element:   Int    { StemElements[stemIndex] }
    var polarity:  Int    { StemPolarity[stemIndex] }

    init(_ stem: Int, _ branch: Int) {
        stemIndex   = ((stem % 10) + 10) % 10
        branchIndex = ((branch % 12) + 12) % 12
    }
}

// MARK: - Hidden Stem

struct HiddenStemEntry {
    let stemIndex: Int
    let weight:    HiddenStemWeight
}

enum HiddenStemWeight: String, Codable {
    case main, mid, minor
    var scoreZh: String {
        switch self { case .main: "主气"; case .mid: "中气"; case .minor: "余气" }
    }
}

// MARK: - Ten God

struct TenGodEntry {
    let name:  String  // zh name
    let nameEn:String
}

// MARK: - Five Elements

struct FiveElementStrength {
    var wood:  Int = 0
    var fire:  Int = 0
    var earth: Int = 0
    var metal: Int = 0
    var water: Int = 0

    var total: Int { wood + fire + earth + metal + water }

    subscript(element: Int) -> Int {
        get { [wood, fire, earth, metal, water][element] }
    }

    mutating func add(_ element: Int, _ value: Int = 1) {
        switch element {
        case 0: wood  += value
        case 1: fire  += value
        case 2: earth += value
        case 3: metal += value
        case 4: water += value
        default: break
        }
    }
}

// MARK: - Major Cycle

struct MajorCycle {
    let startAge: Int   // age when this 10-year cycle begins
    let pillar:   Pillar
}

// MARK: - 时辰 (Shichen)

struct Shichen {
    let branchIndex: Int  // 0(子)–11(亥)

    /// Display label in Chinese: "子时", "丑时", …
    var labelZh: String { BranchNames[branchIndex] + "时" }
    var labelEn: String { BranchNamesEn[branchIndex] + " Hour" }

    /// The two-hour window in local time
    var startHour: Int { ((branchIndex * 2) + 23) % 24 }
    var endHour:   Int { (startHour + 1) % 24 }

    static func from(hour: Int, distinguishLateZi: Bool) -> (shichen: Shichen, isLateZi: Bool) {
        let isLateZi = distinguishLateZi && hour == 23
        let branch: Int
        if hour == 23 {
            branch = 0  // 子时 (either early next day's or late current day's)
        } else {
            branch = (hour + 1) / 2
        }
        return (Shichen(branchIndex: branch), isLateZi)
    }
}

// MARK: - Full Chart

struct BaziChart {
    let input:       BaziInput
    let yearPillar:  Pillar
    let monthPillar: Pillar
    let dayPillar:   Pillar
    let hourPillar:  Pillar?   // nil if birth time unknown
    let hiddenStems: [Int: [HiddenStemEntry]]  // branchIndex → hidden stems
    let tenGods:     TenGods
    let fiveElements: FiveElementStrength
    let majorCycles: [MajorCycle]
    let startingAge: Int   // age at which major cycles begin (in years, fraction ignored)
    let cycleDirection: String  // "顺排" or "逆排"

    var allPillars: [Pillar] {
        [yearPillar, monthPillar, dayPillar] + (hourPillar.map { [$0] } ?? [])
    }
}

struct TenGods {
    /// Ten god of each pillar's stem relative to day master
    let yearStemGod:  TenGodEntry?
    let monthStemGod: TenGodEntry?
    let hourStemGod:  TenGodEntry?
    /// Ten god of each pillar's main hidden stem (branch main qi)
    let yearBranchGod:  TenGodEntry
    let monthBranchGod: TenGodEntry
    let dayBranchGod:   TenGodEntry
    let hourBranchGod:  TenGodEntry?
}
