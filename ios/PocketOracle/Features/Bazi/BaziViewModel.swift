import Foundation
import Observation
import SwiftData

@Observable
final class BaziViewModel {

    // MARK: - Input state
    var selectedBirthDate: Date
    var includesBirthHour   = false
    var selectedBirthHour   = 12
    var selectedBirthMinute = 0
    var selectedCity:       BirthCity?
    var selectedGender:     Gender = .male
    var useTrueSolarTime    = false
    var distinguishLateZi   = false

    // MARK: - Output state
    var chart:    BaziChart?
    var cities:   [BirthCity] = []
    var isLoading = false
    var errorMessage: String?
    var hasSavedCurrentChart = false

    init() {
        let defaultDate = Calendar.current.date(
            from: DateComponents(year: 1996, month: 6, day: 18)
        ) ?? Date()
        self.selectedBirthDate = AppPreferences.savedBirthday ?? defaultDate

        // Load cities
        if let loaded = try? BaziDataLoader.loadCities() {
            self.cities = loaded
            self.selectedCity = nil
        }
    }

    // MARK: - Input mutations

    func updateBirthDate(_ date: Date) {
        selectedBirthDate = date
        clearResult()
    }

    func setBirthHourEnabled(_ enabled: Bool) {
        includesBirthHour = enabled
        clearResult()
    }

    func setBirthHour(_ hour: Int)     { selectedBirthHour = hour;   clearResult() }
    func setBirthMinute(_ min: Int)    { selectedBirthMinute = min;   clearResult() }
    func setCity(_ city: BirthCity?) {
        selectedCity = city
        if city == nil { useTrueSolarTime = false }
        clearResult()
    }
    func setGender(_ gender: Gender)   { selectedGender = gender;     clearResult() }
    func setTrueSolarTime(_ on: Bool)  { useTrueSolarTime = on;       clearResult() }
    func setDistinguishLateZi(_ on: Bool) { distinguishLateZi = on;   clearResult() }

    // MARK: - Generate

    func generate() {
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil

        let cal   = Calendar(identifier: .gregorian)
        let comps = cal.dateComponents([.year, .month, .day], from: selectedBirthDate)
        let input = BaziInput(
            birthYear:              comps.year!,
            birthMonth:             comps.month!,
            birthDay:               comps.day!,
            birthHour:              includesBirthHour ? selectedBirthHour : nil,
            birthMinute:            includesBirthHour ? selectedBirthMinute : nil,
            city:                   selectedCity,
            gender:                 selectedGender,
            useTrueSolarTime:       useTrueSolarTime && includesBirthHour,
            distinguishLateZiHour:  distinguishLateZi && includesBirthHour
        )

        do {
            chart = try BaziCalculator.calculate(input: input)
            hasSavedCurrentChart = false
        } catch {
            UserFacingErrorMapper.log(error, context: .bazi)
            errorMessage = UserFacingErrorMapper.message(for: error, context: .bazi)
        }

        isLoading = false
    }

    // MARK: - Save to history

    func saveToHistory(context: ModelContext) {
        guard !hasSavedCurrentChart, let chart else { return }

        let isZh = AppLanguageOption.isChinese
        let dayMaster = isZh ? chart.dayPillar.stemZh : chart.dayPillar.stemEn
        let title  = isZh ? "八字 · \(dayMaster)日主" : "Bazi · \(dayMaster) Day Master"
        let summary = isZh
            ? "四柱：\(chart.yearPillar.nameZh) \(chart.monthPillar.nameZh) \(chart.dayPillar.nameZh)"
            : "Four Pillars: \(chart.yearPillar.nameEn) \(chart.monthPillar.nameEn) \(chart.dayPillar.nameEn)"

        // Encode as a structured JSON
        struct ChartSnapshot: Codable {
            let year: String; let month: String; let day: String; let hour: String?
            let dayMasterElement: String; let cycleDirection: String; let startingAge: Int
        }
        let snapshot = ChartSnapshot(
            year:             chart.yearPillar.nameZh,
            month:            chart.monthPillar.nameZh,
            day:              chart.dayPillar.nameZh,
            hour:             chart.hourPillar?.nameZh,
            dayMasterElement: ElementNamesZh[StemElements[chart.dayPillar.stemIndex]],
            cycleDirection:   chart.cycleDirection,
            startingAge:      chart.startingAge
        )
        guard let data = try? JSONEncoder().encode(snapshot),
              let json = String(data: data, encoding: .utf8) else { return }

        context.insert(ReadingRecord(
            type: .bazi,
            title: title,
            summary: summary,
            detailJSON: json
        ))
        try? context.save()
        hasSavedCurrentChart = true
    }

    private func clearResult() {
        chart = nil
        errorMessage = nil
        hasSavedCurrentChart = false
    }
}
