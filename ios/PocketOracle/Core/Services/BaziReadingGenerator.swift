import Foundation

enum BaziReadingGenerator {

    static func generate(
        birthDate: Date,
        birthHour: Int?
    ) throws -> BaziReading {
        let loader = ContentLoader.shared
        let stems = try loader.loadHeavenlyStems().stems.sorted { $0.index < $1.index }
        let branches = try loader.loadEarthlyBranches().branches
        let templates = try loader.loadBaziTemplates()
        let calendar = Calendar.current
        let isZh = AppLanguageOption.isChinese

        let startOfDay = calendar.startOfDay(for: birthDate)
        let referenceDate = calendar.date(from: DateComponents(year: 1900, month: 1, day: 1)) ?? startOfDay
        let daysBetween = calendar.dateComponents([.day], from: referenceDate, to: startOfDay).day ?? 0
        let stem = stems[positiveModulo(daysBetween, stems.count)]

        let birthMonth = calendar.component(.month, from: birthDate)
        let branch = branches.first(where: { $0.month == birthMonth })
            ?? branches[positiveModulo(birthMonth - 1, branches.count)]

        let introPool = isZh ? templates.overallIntroZh : templates.overallIntroEn
        let phasePool = isZh ? templates.phaseAdviceZh : templates.phaseAdviceEn
        let introSeed = positiveModulo(daysBetween + branch.index, max(introPool.count, 1))
        let phaseSeed = (Date().dayOfYear * 31) + (stem.index * 7) + (birthHour ?? 12)
        let intro = introPool.isEmpty ? "" : introPool[introSeed]
        let disclaimer = isZh ? templates.disclaimerZh : templates.disclaimerEn
        let monthNuance = isZh ? branch.nuanceZh : branch.nuanceEn
        let elementSummary = templates.elementPersonality.description(
            element: stem.element,
            isZh: isZh
        )
        let combinedElementDescription = isZh
            ? "\(elementSummary)\n\n月令补充：\(monthNuance)"
            : "\(elementSummary)\n\nSeasonal nuance: \(monthNuance)"

        return BaziReading(
            birthDate: birthDate,
            birthHour: birthHour,
            stem: stem,
            disclaimer: disclaimer,
            overallPersonality: [intro, stem.localizedPersonality]
                .filter { !$0.isEmpty }
                .joined(separator: "\n\n"),
            love: stem.localizedLove,
            career: stem.localizedCareer,
            wealth: stem.localizedWealth,
            phaseAdvice: phasePool.isEmpty
                ? ""
                : phasePool[positiveModulo(phaseSeed, phasePool.count)],
            elementDescription: combinedElementDescription
        )
    }

    private static func positiveModulo(_ value: Int, _ mod: Int) -> Int {
        ((value % mod) + mod) % mod
    }
}
