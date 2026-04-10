import Foundation
import UIKit
import UserNotifications

extension Notification.Name {
    static let openDailyRitual = Notification.Name("openDailyRitual")
}

final class NotificationAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        guard (response.notification.request.content.userInfo["destination"] as? String) == "daily_ritual" else {
            return
        }

        await MainActor.run {
            NotificationCenter.default.post(name: .openDailyRitual, object: nil)
        }
    }
}
