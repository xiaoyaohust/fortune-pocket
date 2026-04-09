import SwiftUI

/// Root tab container for Fortune Pocket.
/// iOS navigation: custom bottom tab bar + a dedicated NavigationStack per tab.
struct ContentView: View {

    @State private var selectedTab: AppTab = .home
    @State private var isTabBarHidden = false

    var body: some View {
        ZStack {
            AppColors.gradientBackground
                .ignoresSafeArea()

            activeTabStack
                .padding(.bottom, isTabBarHidden ? 0 : 88)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .tint(AppColors.accentGold)
        .overlay(alignment: .bottom) {
            if !isTabBarHidden {
                GeometryReader { proxy in
                    VStack(spacing: 0) {
                        Spacer()
                        FortuneBottomTabBar(
                            selectedTab: $selectedTab,
                            bottomInset: proxy.safeAreaInsets.bottom
                        )
                    }
                }
                .ignoresSafeArea()
            }
        }
        .onPreferenceChange(FortuneTabBarHiddenPreferenceKey.self) { isTabBarHidden = $0 }
    }

    @ViewBuilder
    private var activeTabStack: some View {
        switch selectedTab {
        case .home:
            NavigationStack {
                HomeView()
            }
        case .history:
            NavigationStack {
                HistoryView()
            }
        case .settings:
            NavigationStack {
                SettingsView()
            }
        }
    }
}

enum AppTab: Hashable, CaseIterable {
    case home, history, settings

    var title: String {
        switch self {
        case .home: return String.appLocalized("nav_home")
        case .history: return String.appLocalized("nav_history")
        case .settings: return String.appLocalized("nav_settings")
        }
    }

    var iconName: String {
        switch self {
        case .home: return "book.pages"
        case .history: return "clock"
        case .settings: return "gearshape"
        }
    }

    var selectedIconName: String {
        switch self {
        case .home: return "book.pages.fill"
        case .history: return "clock.fill"
        case .settings: return "gearshape.fill"
        }
    }
}

private struct FortuneBottomTabBar: View {
    @Binding var selectedTab: AppTab
    let bottomInset: CGFloat

    var body: some View {
        VStack(spacing: 0) {
            Rectangle()
                .fill(AppColors.divider.opacity(0.8))
                .frame(height: 0.5)

            HStack(spacing: 0) {
                ForEach(AppTab.allCases, id: \.self) { tab in
                    VStack(spacing: 6) {
                        Rectangle()
                            .fill(selectedTab == tab ? AppColors.accentGold : .clear)
                            .frame(height: 2)
                        Image(systemName: selectedTab == tab ? tab.selectedIconName : tab.iconName)
                            .font(.system(size: 20, weight: .semibold))
                        Text(tab.title)
                            .font(AppFonts.caption)
                    }
                    .foregroundStyle(selectedTab == tab ? AppColors.accentGold : AppColors.textSecondary)
                    .frame(maxWidth: .infinity)
                    .padding(.bottom, 8)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        selectedTab = tab
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, 8)
            .padding(.top, 6)
            .padding(.bottom, max(10, bottomInset == 0 ? 10 : bottomInset - 2))
        }
        .frame(maxWidth: .infinity)
        .background(AppColors.backgroundBase.opacity(0.98))
    }
}

#Preview {
    ContentView()
        .modelContainer(for: ReadingRecord.self, inMemory: true)
}
