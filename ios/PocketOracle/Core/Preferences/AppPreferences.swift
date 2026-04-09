import Foundation

enum AppPreferenceKeys {
    static let notificationEnabled = "notification_enabled"
    static let notificationHour = "notification_hour"
    static let notificationMinute = "notification_minute"
    static let userBirthdayTimestamp = "user_birthday"
    static let appLanguage = "app_language"
}

enum AppLanguageOption: String, CaseIterable, Identifiable {
    case system
    case zh
    case en

    var id: String { rawValue }

    static var current: AppLanguageOption {
        let rawValue = UserDefaults.standard.string(forKey: AppPreferenceKeys.appLanguage)
        return AppLanguageOption(rawValue: rawValue ?? AppLanguageOption.system.rawValue) ?? .system
    }

    static var isChinese: Bool {
        switch current {
        case .system:
            return Locale.current.language.languageCode?.identifier == "zh"
        case .zh:
            return true
        case .en:
            return false
        }
    }

    var locale: Locale {
        switch self {
        case .system:
            return .current
        case .zh:
            return Locale(identifier: "zh-Hans")
        case .en:
            return Locale(identifier: "en")
        }
    }

    var localizationBundle: Bundle {
        switch self {
        case .system:
            return .main
        case .zh:
            return Bundle(path: Bundle.main.path(forResource: "zh-Hans", ofType: "lproj") ?? "") ?? .main
        case .en:
            return Bundle(path: Bundle.main.path(forResource: "en", ofType: "lproj") ?? "") ?? .main
        }
    }

    func localizedString(forKey key: String) -> String {
        let localized = localizationBundle.localizedString(forKey: key, value: nil, table: nil)
        guard localized != key else {
            return Bundle.main.localizedString(forKey: key, value: nil, table: nil)
        }
        return localized
    }

    static func localized(_ key: String) -> String {
        current.localizedString(forKey: key)
    }
}

enum AppPreferences {

    static var savedBirthday: Date? {
        let timestamp = UserDefaults.standard.double(forKey: AppPreferenceKeys.userBirthdayTimestamp)
        guard timestamp > 0 else { return nil }
        return Date(timeIntervalSince1970: timestamp)
    }

    static func storeBirthday(_ date: Date?) {
        if let date {
            UserDefaults.standard.set(date.timeIntervalSince1970, forKey: AppPreferenceKeys.userBirthdayTimestamp)
        } else {
            UserDefaults.standard.removeObject(forKey: AppPreferenceKeys.userBirthdayTimestamp)
        }
    }
}

extension String {
    static func appLocalized(_ key: String) -> String {
        AppLanguageOption.localized(key)
    }
}
