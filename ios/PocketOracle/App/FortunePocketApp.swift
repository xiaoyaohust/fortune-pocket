import SwiftUI
import SwiftData

@main
struct FortunePocketApp: App {
    @AppStorage(AppPreferenceKeys.appLanguage) private var appLanguage = AppLanguageOption.system.rawValue

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(
                    \.locale,
                    (AppLanguageOption(rawValue: appLanguage) ?? .system).locale
                )
                .id(appLanguage)
                .preferredColorScheme(.dark)   // Always dark — brand requires it
                .task {
                    await LocalNotificationScheduler.shared.syncFromPreferences()
                }
        }
        .modelContainer(for: ReadingRecord.self)
    }
}
