import Foundation

extension Date {

    /// Day of year (1-365/366). Used for stable daily content selection.
    var dayOfYear: Int {
        Calendar.current.ordinality(of: .day, in: .year, for: self) ?? 1
    }

    /// Localized long date string, e.g. "April 8, 2026" / "2026年4月8日"
    var localizedDateString: String {
        let formatter = DateFormatter()
        formatter.dateStyle = .long
        formatter.timeStyle = .none
        formatter.locale = AppLanguageOption.current.locale
        return formatter.string(from: self)
    }

    /// Short weekday + date, e.g. "Tuesday · April 8"
    var shortDateString: String {
        let formatter = DateFormatter()
        formatter.locale = AppLanguageOption.current.locale
        formatter.setLocalizedDateFormatFromTemplate("EEEMMMd")
        return formatter.string(from: self)
    }
}
