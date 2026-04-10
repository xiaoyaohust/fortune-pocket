import Foundation

struct AstrologyInsightBundle {
    let personalityCore: String
    let relationshipPattern: String
    let strengths: String
    let growthEdge: String
    let currentTheme: String
}

enum AstrologyInsightBuilder {

    static func build(chart: AstrologyChartSnapshot) -> AstrologyInsightBundle {
        let isZh = AppLanguageOption.isChinese
        let rising = chart.risingSign
        let venus = chart.planetPlacements.first(where: { $0.planetId == .venus })
        let mars = chart.planetPlacements.first(where: { $0.planetId == .mars })
        let leadHouse = chart.houseFocus.first
        let hardAspect = chart.majorAspects.first(where: { $0.type == .square || $0.type == .opposition })

        let personalityCore: String
        let relationshipPattern: String
        let strengths: String
        let growthEdge: String
        let currentTheme: String

        if isZh {
            personalityCore = "你的核心驱动力来自\(chart.sunSign.nameZh)的\(chart.sunSign.localizedTraits.first ?? "自我表达")，内在情绪底色带着\(chart.moonSign.nameZh)式的\(chart.moonSign.localizedTraits.first ?? "感受力")。别人初见你时，往往会先接收到\(rising.nameZh)那种\(rising.localizedTraits.first ?? "外在气场")。"
            let venusPart = venus.map { "金星落在\($0.signNameZh)，说明你会被\($0.signNameZh)式的回应方式打动。" } ?? "你的关系感受力，更多从金星与月亮的搭配里流露出来。"
            let marsPart = mars.map { "火星落在\($0.signNameZh)，也让你在靠近或退开时，带着\($0.signNameZh)的节奏。" } ?? "行动层面则更多由火星节奏决定。"
            relationshipPattern = "\(venusPart)\(marsPart) 关系里你最需要的，不只是喜欢本身，而是被真正理解后的安心感。"

            let coreStrengths = [chart.sunSign.strengthsZh.first, rising.strengthsZh.first]
                .compactMap { $0 }
                .joined(separator: "、")
            let elementTone = chart.elementBalance.localizedDominantElement
            strengths = "你最容易被别人感受到的优势，是\(coreStrengths)。再加上\(elementTone)偏强，你一旦进入状态，往往能把气场、判断和执行力串成一条线。"

            let softSpot = chart.moonSign.weaknessesZh.first ?? "过度在意外界回应"
            let tension = hardAspect.map { readableAspect($0) + " " } ?? ""
            growthEdge = "\(tension)这张盘真正的成长课题，不是变得更完美，而是看见自己何时会因为\(softSpot)而过度消耗。越能及时收回注意力，你的能量就越稳定。"

            let houseLine = leadHouse?.localizedSummary ?? "这段时间更适合把精力投向真正重要的主轴，而不是被零碎噪音带着走。"
            currentTheme = "你最近的主题像是在重新整理生活重心。\(houseLine) 所以现在最值得做的，不是多想一步，而是先把最重要的那一步走稳。"
        } else {
            personalityCore = "Your core drive carries the \(chart.sunSign.localizedTraits.first ?? "self-expressive") tone of \(chart.sunSign.nameEn), while your emotional baseline moves more like \(chart.moonSign.nameEn). First impressions often arrive through the \(rising.localizedTraits.first ?? "distinct") presence of your Rising sign in \(rising.nameEn)."
            let venusPart = venus.map { "Venus in \($0.signNameEn) shows what kind of emotional response helps you feel chosen." } ?? "Your relational style is mostly revealed through the Moon and Venus blend in the chart."
            let marsPart = mars.map { "Mars in \($0.signNameEn) also shapes how quickly you move toward or away from closeness." } ?? "Action and desire still follow a clear Mars rhythm in the background."
            relationshipPattern = "\(venusPart) \(marsPart) What you need in love is not just chemistry, but the calm of feeling genuinely understood."

            let coreStrengths = [chart.sunSign.strengthsEn.first, rising.strengthsEn.first]
                .compactMap { $0 }
                .joined(separator: ", ")
            let elementTone = chart.elementBalance.localizedDominantElement
            strengths = "Your most visible strengths are \(coreStrengths). With a chart that leans \(elementTone.lowercased()), you tend to bring presence, judgment, and execution into the same current once you commit."

            let softSpot = chart.moonSign.weaknessesEn.first ?? "over-reading external signals"
            let tension = hardAspect.map { readableAspect($0) + " " } ?? ""
            growthEdge = "\(tension)Your growth edge is less about becoming flawless and more about noticing when \(softSpot) starts draining your energy. The sooner you reclaim your attention, the steadier you become."

            let houseLine = leadHouse?.localizedSummary ?? "This season asks you to return to what matters most instead of scattering energy across noise."
            currentTheme = "Your current chapter feels like a realignment of priorities. \(houseLine) The most helpful move now is not adding more analysis, but steadying the one next step that truly matters."
        }

        return AstrologyInsightBundle(
            personalityCore: personalityCore,
            relationshipPattern: relationshipPattern,
            strengths: strengths,
            growthEdge: growthEdge,
            currentTheme: currentTheme
        )
    }

    private static func readableAspect(_ aspect: AstrologyAspect) -> String {
        let first = aspect.firstPlanetId.localizedName
        let second = aspect.secondPlanetId.localizedName
        let aspectName = aspect.type.localizedName
        if AppLanguageOption.isChinese {
            return "\(first)与\(second)形成\(aspectName)，"
        }
        return "\(first) forms a \(aspectName.lowercased()) with \(second),"
    }

}
