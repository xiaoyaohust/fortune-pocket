import Foundation

struct ReadingPresentation {
    let title: String
    let subtitle: String
    let sections: [ReadingPresentationSection]
    let shareText: String
}

struct ReadingPresentationSection: Identifiable {
    let id = UUID()
    let title: String
    let body: String
}

enum ReadingPresentationBuilder {

    static func presentation(for record: ReadingRecord) -> ReadingPresentation? {
        switch record.type {
        case .tarot:
            return tarotPresentation(for: record)
        case .astrology:
            return horoscopePresentation(for: record)
        case .bazi:
            return baziPresentation(for: record)
        }
    }

    static func shareText(for reading: TarotReading) -> String {
        let isZh = AppLanguageOption.isChinese
        let cardSummary = reading.drawnCards.map { drawnCard in
            let cardName = isZh ? drawnCard.card.nameZh : drawnCard.card.nameEn
            let orientation = drawnCard.isUpright
                ? (isZh ? "正位" : "upright")
                : (isZh ? "逆位" : "reversed")
            return "\(drawnCard.positionLabel): \(cardName) (\(orientation))"
        }.joined(separator: "\n")

        let positionSummary = reading.positionReadings.map { item in
            "\(item.positionLabel): \(firstParagraph(of: item.interpretation))"
        }.joined(separator: "\n")

        return [
            isZh ? "Fortune Pocket · 今日塔罗" : "Fortune Pocket · Tarot Reading",
            reading.theme.localizedName,
            reading.spreadName,
            reading.date.localizedDateString,
            "",
            cardSummary,
            "",
            "\(isZh ? "整体能量" : "Overall Energy"): \(firstParagraph(of: reading.overallEnergy))",
            "\(reading.theme.localizedFocusTitle): \(firstParagraph(of: reading.focusInsight))",
            positionSummary,
            "\(isZh ? "今日建议" : "Today's Guidance"): \(firstParagraph(of: reading.advice))",
            "\(isZh ? "幸运提示" : "Lucky Hint"): \(reading.luckyItemName) / \(reading.luckyColorName) / \(reading.luckyNumber)"
        ].joined(separator: "\n")
    }

    static func shareText(for reading: HoroscopeReading) -> String {
        let isZh = AppLanguageOption.isChinese
        let placementSummary = reading.planetPlacements.prefix(5).map { item in
            let houseText = item.house.map { isZh ? "第\($0)宫" : "House \($0)" } ?? ""
            return "\(item.localizedPlanetName): \(item.localizedSignName) \(houseText)"
        }.joined(separator: "\n")
        let aspectSummary = reading.majorAspects.prefix(3).map { aspect in
            "\(aspect.firstPlanetId.localizedName) \(aspect.type.localizedName) \(aspect.secondPlanetId.localizedName)"
        }.joined(separator: "\n")
        return [
            isZh ? "Fortune Pocket · 本命盘" : "Fortune Pocket · Natal Chart",
            reading.chartSignature,
            "\(reading.birthDateText) · \(reading.birthTimeText)",
            reading.birthCityName,
            "",
            firstParagraph(of: reading.chartSummary),
            "",
            "\(isZh ? "行星落座" : "Placements"):",
            placementSummary,
            aspectSummary.isEmpty ? "" : "\n\(isZh ? "主要相位" : "Major Aspects"):\n\(aspectSummary)",
            "",
            "\(isZh ? "整体画像" : "Overall Profile"): \(firstParagraph(of: reading.overall))",
            "\(isZh ? "感情" : "Love"): \(firstParagraph(of: reading.love))",
            "\(isZh ? "事业" : "Career"): \(firstParagraph(of: reading.career))",
            "\(isZh ? "建议" : "Advice"): \(firstParagraph(of: reading.advice))",
            "\(isZh ? "幸运色" : "Lucky Color"): \(reading.luckyColor)",
            "\(isZh ? "幸运数字" : "Lucky Number"): \(reading.luckyNumber)"
        ]
        .filter { !$0.isEmpty }
        .joined(separator: "\n")
    }

    static func shareText(for reading: BaziReading) -> String {
        let isZh = AppLanguageOption.isChinese
        let dayMasterTitle = isZh
            ? "\(reading.stem.nameZh)日主"
            : "\(reading.stem.nameEn) Day Master"

        return [
            isZh ? "Fortune Pocket · 八字能量" : "Fortune Pocket · Bazi Energy",
            dayMasterTitle,
            reading.birthDate.localizedDateString,
            "",
            "\(isZh ? "性格倾向" : "Personality"): \(firstParagraph(of: reading.overallPersonality))",
            "\(isZh ? "感情倾向" : "Love Tendencies"): \(firstParagraph(of: reading.love))",
            "\(isZh ? "事业倾向" : "Career Tendencies"): \(firstParagraph(of: reading.career))",
            "\(isZh ? "财运倾向" : "Wealth Tendencies"): \(firstParagraph(of: reading.wealth))",
            "\(isZh ? "当前阶段建议" : "Current Phase"): \(firstParagraph(of: reading.phaseAdvice))",
            "",
            reading.disclaimer
        ].joined(separator: "\n")
    }

    private static func tarotPresentation(for record: ReadingRecord) -> ReadingPresentation? {
        guard let detail = decode(TarotReadingDetail.self, from: record.detailJSON) else {
            return nil
        }

        let isZh = AppLanguageOption.isChinese
        let theme = detail.themeId.flatMap(TarotQuestionTheme.init(rawValue:)) ?? .general
        let cardsText = detail.drawnCards.map { drawnCard in
            let cardName = isZh ? drawnCard.cardNameZh : drawnCard.cardNameEn
            let orientation = drawnCard.isUpright
                ? (isZh ? "正位" : "Upright")
                : (isZh ? "逆位" : "Reversed")
            return "\(drawnCard.positionLabel) · \(cardName) (\(orientation))"
        }.joined(separator: "\n")

        if let focusInsight = detail.focusInsight,
           let positionReadings = detail.positionReadings,
           !positionReadings.isEmpty {
            let luckyParts = [detail.luckyItem, detail.luckyColor, detail.luckyNumber.map(String.init)]
                .compactMap { $0 }
                .joined(separator: " / ")
            let sections =
                [ReadingPresentationSection(title: isZh ? "抽到的牌" : "Cards Drawn", body: cardsText),
                 ReadingPresentationSection(title: isZh ? "整体能量" : "Overall Energy", body: detail.overallEnergy),
                 ReadingPresentationSection(title: theme.localizedFocusTitle, body: focusInsight)]
                + positionReadings.map {
                    ReadingPresentationSection(title: $0.title, body: $0.body)
                }
                + [ReadingPresentationSection(title: isZh ? "今日建议" : "Today's Guidance", body: detail.adviceTip),
                   ReadingPresentationSection(title: isZh ? "幸运提示" : "Lucky Hint", body: luckyParts)]

            return ReadingPresentation(
                title: record.title,
                subtitle: record.createdAt.localizedDateString,
                sections: sections,
                shareText: [
                    isZh ? "Fortune Pocket · 历史塔罗" : "Fortune Pocket · Saved Tarot Reading",
                    record.title,
                    record.createdAt.localizedDateString,
                    "",
                    cardsText,
                    "",
                    "\(isZh ? "整体能量" : "Overall Energy"): \(firstParagraph(of: detail.overallEnergy))",
                    "\(theme.localizedFocusTitle): \(firstParagraph(of: focusInsight))",
                    "\(isZh ? "今日建议" : "Today's Guidance"): \(firstParagraph(of: detail.adviceTip))"
                ].joined(separator: "\n")
            )
        }

        let sections = [
            ReadingPresentationSection(title: isZh ? "抽到的牌" : "Cards Drawn", body: cardsText),
            ReadingPresentationSection(title: isZh ? "整体能量" : "Overall Energy", body: detail.overallEnergy),
            ReadingPresentationSection(title: isZh ? "感情解读" : "Love", body: detail.loveReading ?? ""),
            ReadingPresentationSection(title: isZh ? "事业解读" : "Career", body: detail.careerReading ?? ""),
            ReadingPresentationSection(title: isZh ? "财运解读" : "Wealth", body: detail.wealthReading ?? ""),
            ReadingPresentationSection(title: isZh ? "今日建议" : "Today's Guidance", body: detail.adviceTip),
            ReadingPresentationSection(title: isZh ? "幸运提示" : "Lucky Hint", body: detail.luckyItem)
        ]

        return ReadingPresentation(
            title: record.title,
            subtitle: record.createdAt.localizedDateString,
            sections: sections,
            shareText: [
                isZh ? "Fortune Pocket · 历史塔罗" : "Fortune Pocket · Saved Tarot Reading",
                record.title,
                record.createdAt.localizedDateString,
                "",
                cardsText,
                "",
                "\(isZh ? "整体能量" : "Overall Energy"): \(firstParagraph(of: detail.overallEnergy))",
                "\(isZh ? "今日建议" : "Today's Guidance"): \(firstParagraph(of: detail.adviceTip))"
            ].joined(separator: "\n")
        )
    }

    private static func horoscopePresentation(for record: ReadingRecord) -> ReadingPresentation? {
        guard let detail = decode(HoroscopeReadingDetail.self, from: record.detailJSON) else {
            return nil
        }

        let isZh = AppLanguageOption.isChinese
        let signName = isZh ? detail.signNameZh : detail.signNameEn
        let coreSigns = [
            isZh ? "太阳：\(signName)" : "Sun: \(signName)",
            detail.moonSignNameZh.map { isZh ? "月亮：\($0)" : "Moon: \(detail.moonSignNameEn ?? $0)" },
            detail.risingSignNameZh.map { isZh ? "上升：\($0)" : "Rising: \(detail.risingSignNameEn ?? $0)" }
        ]
        .compactMap { $0 }
        .joined(separator: "\n")
        let placementText = detail.planetPlacements?.map { item in
            let houseText = item.house.map { isZh ? "第\($0)宫" : "House \($0)" } ?? ""
            return "\(item.localizedPlanetName): \(item.localizedSignName) \(houseText)"
        }
        .joined(separator: "\n")
        let aspectText = detail.majorAspects?.map { item in
            "\(item.firstPlanetId.localizedName) · \(item.type.localizedName) · \(item.secondPlanetId.localizedName)"
        }
        .joined(separator: "\n")

        var sections = [ReadingPresentationSection]()
        sections.append(
            ReadingPresentationSection(
                title: isZh ? "本命盘" : "Natal Chart",
                body: detail.chartSignature ?? coreSigns
            )
        )
        if let chartSummary = detail.chartSummary {
            sections.append(
                ReadingPresentationSection(
                    title: isZh ? "图谱摘要" : "Chart Summary",
                    body: chartSummary
                )
            )
        }
        sections.append(
            ReadingPresentationSection(
                title: isZh ? "核心星位" : "Core Signs",
                body: coreSigns
            )
        )
        if let placementText, !placementText.isEmpty {
            sections.append(
                ReadingPresentationSection(
                    title: isZh ? "行星落座" : "Placements",
                    body: placementText
                )
            )
        }
        if let aspectText, !aspectText.isEmpty {
            sections.append(
                ReadingPresentationSection(
                    title: isZh ? "主要相位" : "Major Aspects",
                    body: aspectText
                )
            )
        }
        sections.append(contentsOf: [
            ReadingPresentationSection(title: isZh ? "整体画像" : "Overall Profile", body: detail.overall),
            ReadingPresentationSection(title: isZh ? "感情与亲密关系" : "Love", body: detail.love),
            ReadingPresentationSection(title: isZh ? "事业与成长方向" : "Career", body: detail.career),
            ReadingPresentationSection(title: isZh ? "财富与安全感" : "Wealth", body: detail.wealth),
            ReadingPresentationSection(title: isZh ? "社交与沟通" : "Social", body: detail.social),
            ReadingPresentationSection(title: isZh ? "给你的建议" : "Guidance", body: detail.advice),
            ReadingPresentationSection(
                title: isZh ? "幸运提示" : "Lucky Hint",
                body: "\(detail.luckyColor) / \(detail.luckyNumber)"
            )
        ])

        return ReadingPresentation(
            title: record.title,
            subtitle: record.createdAt.localizedDateString,
            sections: sections,
            shareText: [
                isZh ? "Fortune Pocket · 已保存本命盘" : "Fortune Pocket · Saved Natal Chart",
                detail.chartSignature ?? signName,
                [detail.date, detail.birthTime, detail.birthCityName].compactMap { $0 }.joined(separator: " · "),
                "",
                detail.chartSummary ?? "",
                "\(isZh ? "整体画像" : "Overall Profile"): \(firstParagraph(of: detail.overall))",
                "\(isZh ? "感情" : "Love"): \(firstParagraph(of: detail.love))",
                "\(isZh ? "事业" : "Career"): \(firstParagraph(of: detail.career))",
                "\(isZh ? "建议" : "Advice"): \(firstParagraph(of: detail.advice))"
            ]
            .filter { !$0.isEmpty }
            .joined(separator: "\n")
        )
    }

    private static func baziPresentation(for record: ReadingRecord) -> ReadingPresentation? {
        let isZh = AppLanguageOption.isChinese

        // Try new chart-snapshot format first
        if let snapshot = decode(BaziChartSnapshot.self, from: record.detailJSON) {
            let pillarsText = [snapshot.year, snapshot.month, snapshot.day, snapshot.hour]
                .compactMap { $0 }
                .joined(separator: " · ")
            let cycleText = isZh
                ? "\(snapshot.cycleDirection)，起运 \(snapshot.startingAge) 岁"
                : "\(snapshot.cycleDirection), starts at age \(snapshot.startingAge)"
            let sections = [
                ReadingPresentationSection(title: isZh ? "四柱" : "Four Pillars", body: pillarsText),
                ReadingPresentationSection(title: isZh ? "日主五行" : "Day Master Element", body: snapshot.dayMasterElement),
                ReadingPresentationSection(title: isZh ? "大运信息" : "Major Cycle", body: cycleText),
            ]
            return ReadingPresentation(
                title: record.title,
                subtitle: record.createdAt.localizedDateString,
                sections: sections,
                shareText: [
                    isZh ? "Fortune Pocket · 八字排盘" : "Fortune Pocket · Four Pillars Chart",
                    record.title,
                    "",
                    "\(isZh ? "四柱" : "Four Pillars"): \(pillarsText)",
                    "\(isZh ? "日主五行" : "Day Master"): \(snapshot.dayMasterElement)",
                    cycleText
                ].joined(separator: "\n")
            )
        }

        // Fallback: legacy entertainment-text format
        guard let detail = decode(BaziReadingDetail.self, from: record.detailJSON) else {
            return nil
        }
        let birthSummary = [detail.birthDate, detail.birthHour]
            .compactMap { $0 }
            .joined(separator: " · ")
        let sections = [
            ReadingPresentationSection(title: isZh ? "出生信息" : "Birth Info", body: birthSummary),
            ReadingPresentationSection(title: isZh ? "性格倾向" : "Personality", body: detail.overallPersonality),
            ReadingPresentationSection(title: isZh ? "感情倾向" : "Love Tendencies", body: detail.loveTendency),
            ReadingPresentationSection(title: isZh ? "事业倾向" : "Career Tendencies", body: detail.careerTendency),
            ReadingPresentationSection(title: isZh ? "财运倾向" : "Wealth Tendencies", body: detail.wealthTendency),
            ReadingPresentationSection(title: isZh ? "当前阶段建议" : "Current Phase", body: detail.phaseAdvice),
            ReadingPresentationSection(title: isZh ? "五行气质" : "Element Vibe", body: detail.elementDescription)
        ]
        return ReadingPresentation(
            title: record.title,
            subtitle: record.createdAt.localizedDateString,
            sections: sections,
            shareText: [
                isZh ? "Fortune Pocket · 历史八字" : "Fortune Pocket · Saved Bazi Reading",
                record.title, birthSummary, "",
                "\(isZh ? "性格倾向" : "Personality"): \(firstParagraph(of: detail.overallPersonality))",
                "\(isZh ? "当前阶段建议" : "Current Phase"): \(firstParagraph(of: detail.phaseAdvice))"
            ].joined(separator: "\n")
        )
    }

    private static func decode<T: Decodable>(_ type: T.Type, from jsonString: String) -> T? {
        guard let data = jsonString.data(using: .utf8) else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }

    private static func firstParagraph(of text: String) -> String {
        if let range = text.range(of: "\n\n") {
            return String(text[..<range.lowerBound])
        }
        return firstSentence(of: text)
    }

    private static func firstSentence(of text: String) -> String {
        for separator in ["。", ". ", "！", "？"] {
            if let range = text.range(of: separator) {
                return String(text[..<range.upperBound])
            }
        }
        return text
    }
}
