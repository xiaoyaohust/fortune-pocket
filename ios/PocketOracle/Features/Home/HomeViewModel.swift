import Foundation
import Observation

@Observable
final class HomeViewModel {

    // MARK: - State
    var dailyRitual: DailyRitual?
    var isLoading: Bool = true
    var errorMessage: String?
    private var loadedDayKey: Int?

    // MARK: - Init
    init() { load() }

    // MARK: - Load
    func load(force: Bool = false) {
        let dayKey = Calendar.current.component(.year, from: Date()) * 1000 + Date().dayOfYear
        if !force, loadedDayKey == dayKey, dailyRitual != nil {
            return
        }

        isLoading = true
        errorMessage = nil
        do {
            dailyRitual = try DailyRitualBuilder.build()
            loadedDayKey = dayKey
        } catch {
            UserFacingErrorMapper.log(error, context: .home)
            errorMessage = UserFacingErrorMapper.message(for: error, context: .home)
        }
        isLoading = false
    }
}
