import SwiftUI

extension View {

    /// Apply Fortune Pocket card style (background + border + corner radius)
    func fortuneCard(
        background: Color = AppColors.backgroundBase,
        cornerRadius: CGFloat = 16
    ) -> some View {
        self
            .background(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(background)
                    .overlay(
                        RoundedRectangle(cornerRadius: cornerRadius)
                            .stroke(AppColors.accentGold.opacity(0.35), lineWidth: 1)
                    )
            )
    }

    /// Apply the deep background gradient
    func fortuneBackground() -> some View {
        self.background(AppColors.gradientBackground.ignoresSafeArea())
    }

    /// Replaces the system back title with a compact chevron-only button so
    /// screens remain clean even when the in-app language differs from iOS.
    func fortuneNavigationBackButton() -> some View {
        modifier(FortuneNavigationBackButtonModifier())
    }

    func fortuneTabBarHidden(_ hidden: Bool = true) -> some View {
        preference(key: FortuneTabBarHiddenPreferenceKey.self, value: hidden)
    }
}

struct FortuneTabBarHiddenPreferenceKey: PreferenceKey {
    static var defaultValue = false

    static func reduce(value: inout Bool, nextValue: () -> Bool) {
        value = value || nextValue()
    }
}

private struct FortuneNavigationBackButtonModifier: ViewModifier {
    @Environment(\.dismiss) private var dismiss

    func body(content: Content) -> some View {
        content
            .navigationBarBackButtonHidden(true)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(AppColors.accentGold)
                            .frame(width: 32, height: 32)
                            .contentShape(Rectangle())
                    }
                    .accessibilityLabel(String.appLocalized("back"))
                }
            }
    }
}

extension Text {

    func accentGold() -> Text {
        self.foregroundColor(AppColors.accentGold)
    }

    func secondary() -> Text {
        self.foregroundColor(AppColors.textSecondary)
    }

    func muted() -> Text {
        self.foregroundColor(AppColors.textMuted)
    }
}
