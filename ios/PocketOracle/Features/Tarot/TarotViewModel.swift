import Foundation
import SwiftData

@Observable
final class TarotViewModel {

    enum Phase { case idle, result(TarotReading) }

    var phase:        Phase        = .idle
    var selectedTheme: TarotQuestionTheme = .general
    var isGenerating: Bool         = false
    var errorMessage: String?      = nil

    // MARK: - Actions

    func setTheme(_ theme: TarotQuestionTheme) {
        selectedTheme = theme
        errorMessage = nil
        if case .result = phase {
            phase = .idle
        }
    }

    func draw() {
        guard !isGenerating else { return }
        isGenerating = true
        errorMessage = nil
        do {
            let reading = try TarotReadingGenerator.generate(theme: selectedTheme)
            phase = .result(reading)
        } catch {
            UserFacingErrorMapper.log(error, context: .tarot)
            errorMessage = UserFacingErrorMapper.message(for: error, context: .tarot)
        }
        isGenerating = false
    }

    func saveToHistory(context: ModelContext) {
        guard case .result(let reading) = phase else { return }
        guard let jsonData = try? JSONEncoder().encode(TarotReadingGenerator.toDetail(reading: reading)),
              let jsonStr  = String(data: jsonData, encoding: .utf8) else { return }

        let isZh      = AppLanguageOption.isChinese
        let cardNames = reading.drawnCards
            .map { isZh ? $0.card.nameZh : $0.card.nameEn }
            .joined(separator: isZh ? "、" : ", ")

        context.insert(ReadingRecord(
            type:       .tarot,
            title:      isZh ? "塔罗 · \(reading.theme.localizedName)" : "Tarot · \(reading.theme.localizedName)",
            summary:    cardNames,
            detailJSON: jsonStr
        ))
        try? context.save()
    }

    func reset() {
        phase        = .idle
        errorMessage = nil
    }
}
