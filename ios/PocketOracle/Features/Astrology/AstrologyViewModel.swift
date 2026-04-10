import Foundation
import Observation
import SwiftData

@Observable
final class AstrologyViewModel {

    var signs: [ZodiacSign] = []
    var cities: [BirthCity] = []
    var selectedBirthDate: Date
    var birthHour: Int = 12
    var birthMinute: Int = 0
    var selectedCity: BirthCity?
    var reading: HoroscopeReading?
    var isLoading = true
    var isGenerating = false
    var errorMessage: String?
    var hasSavedCurrentReading = false

    init() {
        self.selectedBirthDate = Calendar.current.date(
            from: DateComponents(year: 1998, month: 8, day: 8)
        ) ?? Date()
        if let savedBirthday = AppPreferences.savedBirthday {
            self.selectedBirthDate = savedBirthday
        }
        loadReferenceData()
    }

    var inferredSunSign: ZodiacSign? {
        let month = Calendar.current.component(.month, from: selectedBirthDate)
        let day = Calendar.current.component(.day, from: selectedBirthDate)
        return ZodiacSign.sign(for: month, day: day, in: signs)
    }

    var timePickerDate: Date {
        get {
            Calendar.current.date(from: DateComponents(hour: birthHour, minute: birthMinute)) ?? Date()
        }
        set {
            birthHour = Calendar.current.component(.hour, from: newValue)
            birthMinute = Calendar.current.component(.minute, from: newValue)
            clearGeneratedResult()
        }
    }

    var canGenerate: Bool {
        !isLoading && selectedCity != nil && !isGenerating
    }

    func updateBirthDate(_ date: Date) {
        selectedBirthDate = date
        clearGeneratedResult()
    }

    func setCity(_ city: BirthCity?) {
        selectedCity = city
        clearGeneratedResult()
    }

    func generate() {
        guard !isGenerating else { return }
        guard let city = selectedCity else {
            errorMessage = AppLanguageOption.isChinese
                ? "请先选择出生城市，才能计算上升点、宫位与相位。"
                : "Choose a birth city first so we can calculate the rising sign, houses, and aspects."
            return
        }

        let calendar = Calendar.current
        let input = AstrologyInput(
            birthYear: calendar.component(.year, from: selectedBirthDate),
            birthMonth: calendar.component(.month, from: selectedBirthDate),
            birthDay: calendar.component(.day, from: selectedBirthDate),
            birthHour: birthHour,
            birthMinute: birthMinute,
            city: city
        )

        isGenerating = true
        errorMessage = nil
        do {
            reading = try HoroscopeReadingGenerator.generate(input: input)
            hasSavedCurrentReading = false
        } catch {
            UserFacingErrorMapper.log(error, context: .astrologyGenerate)
            errorMessage = UserFacingErrorMapper.message(for: error, context: .astrologyGenerate)
        }
        isGenerating = false
    }

    func saveToHistory(context: ModelContext) {
        guard !hasSavedCurrentReading, let reading else { return }

        let detail = HoroscopeReadingDetail(
            signId: reading.sign.id,
            signNameZh: reading.sign.nameZh,
            signNameEn: reading.sign.nameEn,
            date: reading.birthDateText,
            birthTime: reading.birthTimeText,
            birthCityName: reading.birthCityName,
            timeZoneId: reading.timeZoneId,
            moonSignId: reading.moonSign.id,
            moonSignNameZh: reading.moonSign.nameZh,
            moonSignNameEn: reading.moonSign.nameEn,
            risingSignId: reading.risingSign?.id,
            risingSignNameZh: reading.risingSign?.nameZh,
            risingSignNameEn: reading.risingSign?.nameEn,
            chartSignature: reading.chartSignature,
            chartSummary: reading.chartSummary,
            personalityCore: reading.personalityCore,
            relationshipPattern: reading.relationshipPattern,
            strengths: reading.strengths,
            growthEdge: reading.growthEdge,
            currentTheme: reading.currentTheme,
            overall: reading.overall,
            love: reading.love,
            career: reading.career,
            wealth: reading.wealth,
            social: reading.social,
            advice: reading.advice,
            luckyColor: reading.luckyColor,
            luckyNumber: reading.luckyNumber,
            planetPlacements: reading.planetPlacements,
            majorAspects: reading.majorAspects,
            elementBalance: reading.elementBalance,
            houseFocus: reading.houseFocus
        )

        guard let jsonData = try? JSONEncoder().encode(detail),
              let jsonString = String(data: jsonData, encoding: .utf8) else {
            return
        }

        let isZh = AppLanguageOption.isChinese
        context.insert(
            ReadingRecord(
                type: .astrology,
                title: isZh
                    ? "星盘 · \(reading.sign.nameZh)"
                    : "Natal Chart · \(reading.sign.nameEn)",
                summary: firstSentence(of: reading.chartSummary),
                detailJSON: jsonString
            )
        )
        try? context.save()
        hasSavedCurrentReading = true
    }

    func clearGeneratedResult() {
        reading = nil
        errorMessage = nil
        hasSavedCurrentReading = false
    }

    private func loadReferenceData() {
        isLoading = true
        errorMessage = nil
        do {
            signs = try ContentLoader.shared.loadZodiacSigns().signs
            cities = try BaziDataLoader.loadCities()
        } catch {
            UserFacingErrorMapper.log(error, context: .astrologyLoad)
            errorMessage = UserFacingErrorMapper.message(for: error, context: .astrologyLoad)
        }
        isLoading = false
    }

    private func firstSentence(of text: String) -> String {
        for separator in ["。", ". ", "！", "？"] {
            if let range = text.range(of: separator) {
                return String(text[..<range.upperBound])
            }
        }
        return text
    }
}
