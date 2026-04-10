import Foundation

struct HistoryTrajectoryInsight: Identifiable {
    let id = UUID()
    let title: String
    let body: String
}

struct HistoryTrajectorySnapshot {
    let headline: String
    let insights: [HistoryTrajectoryInsight]
}

enum HistoryTrajectoryBuilder {

    static func build(records: [ReadingRecord]) -> HistoryTrajectorySnapshot? {
        guard !records.isEmpty else { return nil }
        let isZh = AppLanguageOption.isChinese
        let recentRecords = Array(records.prefix(18))
        let decoder = JSONDecoder()

        var themeCounts: [String: Int] = [:]
        var cardCounts: [String: Int] = [:]
        var inwardSignals = 0
        var outwardSignals = 0
        var groundedSignals = 0
        var airySignals = 0
        var recordTypeCounts: [ReadingType: Int] = [:]

        for record in recentRecords {
            recordTypeCounts[record.type, default: 0] += 1
            switch record.type {
            case .tarot:
                guard let detail = try? decoder.decode(TarotReadingDetail.self, from: Data(record.detailJSON.utf8)) else { continue }
                if let themeId = detail.themeId {
                    themeCounts[normalizedThemeName(themeId, isZh: isZh), default: 0] += 1
                }
                for card in detail.drawnCards {
                    cardCounts[isZh ? card.cardNameZh : card.cardNameEn, default: 0] += 1
                }
                let reversedCount = detail.drawnCards.filter { !$0.isUpright }.count
                if reversedCount >= max(1, detail.drawnCards.count / 2) {
                    inwardSignals += 1
                } else {
                    outwardSignals += 1
                }
            case .astrology:
                guard let detail = try? decoder.decode(HoroscopeReadingDetail.self, from: Data(record.detailJSON.utf8)),
                      let balance = detail.elementBalance else { continue }
                switch balance.dominantElementKey {
                case "water": inwardSignals += 1
                case "fire": outwardSignals += 1
                case "earth": groundedSignals += 1
                case "air": airySignals += 1
                default: break
                }
            case .bazi:
                groundedSignals += 1
            }
        }

        let headline: String
        if isZh {
            headline = "最近 \(recentRecords.count) 次记录里，你更像是在反复确认自己当下真正关心什么。"
        } else {
            headline = "Across your last \(recentRecords.count) readings, a clearer pattern is starting to repeat."
        }

        var insights: [HistoryTrajectoryInsight] = []

        if let themePulse = themeCounts.max(by: { $0.value < $1.value }) {
            insights.append(
                HistoryTrajectoryInsight(
                    title: isZh ? "最近最常出现的主题" : "Most Repeated Theme",
                    body: isZh
                        ? "你最近最常回到「\(themePulse.key)」这个命题，说明这不是一次性的好奇，而是这段时间真正牵动你的主轴。"
                        : "You keep returning to “\(themePulse.key),” which suggests this is not a passing curiosity but a live theme in your life right now."
                )
            )
        }

        let repeatedCards = cardCounts
            .filter { $0.value >= 2 }
            .sorted { lhs, rhs in lhs.value == rhs.value ? lhs.key < rhs.key : lhs.value > rhs.value }
            .prefix(3)
            .map(\.key)

        if !repeatedCards.isEmpty {
            let cardLine = repeatedCards.joined(separator: isZh ? "、" : ", ")
            insights.append(
                HistoryTrajectoryInsight(
                    title: isZh ? "反复出现的牌" : "Cards Echoing Back",
                    body: isZh
                        ? "\(cardLine)已经不止一次出现，它们更像是在重复提醒你同一个核心功课。"
                        : "\(cardLine) has shown up more than once, which usually means the same lesson keeps asking for your attention."
                )
            )
        }

        insights.append(
            HistoryTrajectoryInsight(
                title: isZh ? "最近的情绪天气" : "Recent Emotional Weather",
                body: emotionalWeatherLine(
                    inwardSignals: inwardSignals,
                    outwardSignals: outwardSignals,
                    groundedSignals: groundedSignals,
                    airySignals: airySignals,
                    isZh: isZh
                )
            )
        )

        if let dominantType = recordTypeCounts.max(by: { $0.value < $1.value })?.key {
            insights.append(
                HistoryTrajectoryInsight(
                    title: isZh ? "接下来最适合的仪式" : "Best Next Ritual",
                    body: nextRitualLine(for: dominantType, isZh: isZh)
                )
            )
        }

        return HistoryTrajectorySnapshot(headline: headline, insights: insights)
    }

    private static func normalizedThemeName(_ raw: String, isZh: Bool) -> String {
        switch raw {
        case "general":
            return isZh ? "综合主题" : "General Focus"
        case "love", "relationship":
            return isZh ? "关系主题" : "Relationship Focus"
        case "career":
            return isZh ? "事业主题" : "Career Focus"
        case "wealth":
            return isZh ? "财富主题" : "Wealth Focus"
        default:
            return raw
        }
    }

    private static func emotionalWeatherLine(
        inwardSignals: Int,
        outwardSignals: Int,
        groundedSignals: Int,
        airySignals: Int,
        isZh: Bool
    ) -> String {
        let counts = [
            ("inward", inwardSignals),
            ("outward", outwardSignals),
            ("grounded", groundedSignals),
            ("airy", airySignals)
        ]
        let dominant = counts.max { $0.1 < $1.1 }?.0 ?? "grounded"
        switch dominant {
        case "inward":
            return isZh
                ? "你最近的记录更偏向向内整理，像是在慢慢收拢情绪、看清自己真正要回应的感受。"
                : "Your recent readings lean inward, suggesting a period of emotional sorting and quieter self-honesty."
        case "outward":
            return isZh
                ? "最近的能量更偏向向外推进，像是在把犹豫慢慢转成行动，事情会因为你更主动而松动。"
                : "The recent current leans outward, turning hesitation into movement and asking you to meet life more directly."
        case "airy":
            return isZh
                ? "最近更像是思绪活跃期，很多问题需要先说清楚、看清楚，再决定往哪里走。"
                : "This looks like a mentally active phase where clarity in language and perspective matters more than speed."
        default:
            return isZh
                ? "最近的主旋律是稳住节奏，把基础、边界和日常秩序慢慢重新摆正。"
                : "The strongest recent note is about steadiness: rebuilding rhythm, boundaries, and practical structure."
        }
    }

    private static func nextRitualLine(for type: ReadingType, isZh: Bool) -> String {
        switch type {
        case .tarot:
            return isZh
                ? "你最近最适合继续用塔罗追踪同一个主题，尤其适合改用不同牌阵去照亮同一件事。"
                : "Tarot looks like your clearest next ritual right now, especially if you revisit the same question through a different spread."
        case .astrology:
            return isZh
                ? "你最近更适合回到本命盘，把术语翻成人话，看看真正该被你长期经营的关系与方向。"
                : "A return to your natal chart may be most useful now, especially if you translate the symbols into everyday choices and relationship patterns."
        case .bazi:
            return isZh
                ? "你最近更适合看长期节奏和人生结构，慢一点，但会更稳。"
                : "A slower ritual around long-range structure seems most helpful now, even if it moves more quietly."
        }
    }
}
