import SwiftUI

/// Typography scale for Fortune Pocket (iOS).
/// Uses system SF Pro — no custom font needed for MVP.
/// Matches the design token scale in docs/design-tokens.md.
enum AppFonts {

    // MARK: - Display (large titles, hero text)
    static let displayLarge  = Font.system(size: 34, weight: .light, design: .default)
    static let displayMedium = Font.system(size: 28, weight: .light, design: .default)

    // MARK: - Headline (section headers)
    static let headlineLarge  = Font.system(size: 24, weight: .regular, design: .default)
    static let headlineMedium = Font.system(size: 20, weight: .regular, design: .default)

    // MARK: - Title (card titles, list items)
    static let titleLarge  = Font.system(size: 18, weight: .medium, design: .default)
    static let titleMedium = Font.system(size: 16, weight: .medium, design: .default)

    // MARK: - Body (reading content)
    static let bodyLarge  = Font.system(size: 16, weight: .regular, design: .default)
    static let bodyMedium = Font.system(size: 15, weight: .regular, design: .default)
    static let bodySmall  = Font.system(size: 14, weight: .regular, design: .default)

    // MARK: - Caption / Labels
    static let caption    = Font.system(size: 13, weight: .regular, design: .default)
    static let labelSmall = Font.system(size: 11, weight: .medium, design: .default)

    // MARK: - Accent (card names, quotes — serif feel)
    static let accentLarge  = Font.system(size: 22, weight: .light, design: .serif)
    static let accentMedium = Font.system(size: 17, weight: .light, design: .serif)
    static let accentSmall  = Font.system(size: 14, weight: .light, design: .serif)
}
