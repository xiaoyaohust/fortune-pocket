import SwiftUI

/// Brand color palette for Fortune Pocket.
/// Single source of truth — mirrors docs/design-tokens.md.
enum AppColors {

    // MARK: - Background
    static let backgroundDeep    = Color(hex: "#0E0A1F")
    static let backgroundBase    = Color(hex: "#1A1035")
    static let backgroundElevated = Color(hex: "#251848")

    // MARK: - Accent
    static let accentGold        = Color(hex: "#C9A84C")
    static let accentGoldLight   = Color(hex: "#E8D48B")
    static let accentPurple      = Color(hex: "#7B5EA7")
    static let accentRose        = Color(hex: "#C9748F")

    // MARK: - Text
    static let textPrimary       = Color(hex: "#F0EBE3")
    static let textSecondary     = Color(hex: "#9E96B8")
    static let textMuted         = Color(hex: "#5D5580")

    // MARK: - UI
    static let divider           = Color(hex: "#2E2456")
    static let starGlow          = Color(hex: "#FFE566")

    // MARK: - Semantic
    static let success           = Color(hex: "#27AE60")
    static let warning           = Color(hex: "#F39C12")
    static let error             = Color(hex: "#E74C3C")

    // MARK: - Gradients
    static let gradientGold = LinearGradient(
        colors: [accentGold, accentGoldLight],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    static let gradientBackground = LinearGradient(
        colors: [backgroundDeep, backgroundBase],
        startPoint: .top,
        endPoint: .bottom
    )
}

// MARK: - Hex init for Color
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3:
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6:
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}
