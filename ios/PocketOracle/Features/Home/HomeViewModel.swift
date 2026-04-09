import Foundation
import Observation

@Observable
final class HomeViewModel {

    // MARK: - State
    var dailyQuote: DailyQuote?
    var isLoading: Bool = true
    var errorMessage: String?

    // MARK: - Private
    private let contentLoader = ContentLoader.shared

    // MARK: - Init
    init() { load() }

    // MARK: - Load
    func load() {
        isLoading = true
        errorMessage = nil
        do {
            let quotesData = try contentLoader.loadDailyQuotes()
            let index = (Date().dayOfYear - 1) % quotesData.quotes.count
            dailyQuote = quotesData.quotes[index]
        } catch {
            UserFacingErrorMapper.log(error, context: .home)
            errorMessage = UserFacingErrorMapper.message(for: error, context: .home)
        }
        isLoading = false
    }
}
