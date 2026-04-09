import Foundation
import UserNotifications

@MainActor
final class LocalNotificationScheduler {

    static let shared = LocalNotificationScheduler()

    private let reminderIdentifier = "fortune-pocket-daily-reminder"

    private init() {}

    func requestAuthorization() async -> Bool {
        do {
            return try await UNUserNotificationCenter.current().requestAuthorization(
                options: [.alert, .sound, .badge]
            )
        } catch {
            return false
        }
    }

    func scheduleDailyReminder(hour: Int, minute: Int) async {
        cancelDailyReminder()

        let content = UNMutableNotificationContent()
        content.title = String.appLocalized("notification_title")
        content.body = String.appLocalized("notification_body")
        content.sound = .default

        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = minute

        let trigger = UNCalendarNotificationTrigger(
            dateMatching: dateComponents,
            repeats: true
        )

        let request = UNNotificationRequest(
            identifier: reminderIdentifier,
            content: content,
            trigger: trigger
        )

        try? await UNUserNotificationCenter.current().add(request)
    }

    func cancelDailyReminder() {
        UNUserNotificationCenter.current().removePendingNotificationRequests(
            withIdentifiers: [reminderIdentifier]
        )
    }

    func syncFromPreferences() async {
        let defaults = UserDefaults.standard
        let isEnabled = defaults.bool(forKey: AppPreferenceKeys.notificationEnabled)
        guard isEnabled else {
            cancelDailyReminder()
            return
        }

        let hour = defaults.object(forKey: AppPreferenceKeys.notificationHour) as? Int ?? 9
        let minute = defaults.object(forKey: AppPreferenceKeys.notificationMinute) as? Int ?? 0
        await scheduleDailyReminder(hour: hour, minute: minute)
    }
}
