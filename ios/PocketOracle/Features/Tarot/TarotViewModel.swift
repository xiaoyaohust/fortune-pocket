import Foundation
import SwiftData

@Observable
final class TarotViewModel {

    enum Phase {
        case idle
        case shuffling(deck: [TarotCard])
        case picking(deck: [TarotCard])
        case result(TarotReading)
    }

    var phase: Phase = .idle
    var selectedTheme: TarotQuestionTheme = .general
    var pickedIndices: [Int] = []
    var pickedUprights: [Bool] = []
    var isGenerating = false
    var errorMessage: String? = nil

    // Stable ID for animation transitions
    var phaseIndex: Int {
        switch phase {
        case .idle:      return 0
        case .shuffling: return 1
        case .picking:   return 2
        case .result:    return 3
        }
    }

    // MARK: - Theme

    func setTheme(_ theme: TarotQuestionTheme) {
        selectedTheme = theme
        errorMessage = nil
        if case .result = phase { phase = .idle }
    }

    // MARK: - Shuffle → Pick flow

    func startShuffle() {
        guard let cardsData = try? ContentLoader.shared.loadTarotCards() else {
            errorMessage = AppLanguageOption.isChinese
                ? "无法加载牌组数据"
                : "Unable to load card data"
            return
        }
        pickedIndices = []
        pickedUprights = []
        errorMessage = nil
        phase = .shuffling(deck: cardsData.cards.shuffled())
    }

    func commitShuffle() {
        guard case .shuffling(let deck) = phase else { return }
        phase = .picking(deck: deck)
    }

    // MARK: - Picking

    func togglePick(at index: Int) {
        if let pos = pickedIndices.firstIndex(of: index) {
            pickedIndices.remove(at: pos)
            pickedUprights.remove(at: pos)
        } else if pickedIndices.count < 3 {
            pickedIndices.append(index)
            pickedUprights.append(Bool.random())
        }
    }

    func reveal() {
        guard case .picking(let deck) = phase, pickedIndices.count == 3 else { return }
        guard !isGenerating else { return }
        isGenerating = true
        errorMessage = nil
        let cards = pickedIndices.map { deck[$0] }
        do {
            let reading = try TarotReadingGenerator.generate(
                pickedCards: cards,
                uprightStates: pickedUprights,
                theme: selectedTheme
            )
            phase = .result(reading)
        } catch {
            UserFacingErrorMapper.log(error, context: .tarot)
            errorMessage = UserFacingErrorMapper.message(for: error, context: .tarot)
        }
        isGenerating = false
    }

    // MARK: - Save & Reset

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
        phase = .idle
        pickedIndices = []
        pickedUprights = []
        errorMessage = nil
    }
}
