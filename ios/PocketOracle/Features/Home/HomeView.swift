import SwiftUI

struct HomeView: View {

    @State private var viewModel = HomeViewModel()

    var body: some View {
        GeometryReader { geo in
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {

                    // ── Header ──────────────────────────────────────────────────
                    HomeHeaderView()
                        .padding(.horizontal, 24)
                        .padding(.top, 12)

                    // ── Daily Quote ──────────────────────────────────────────────
                    Group {
                        if viewModel.isLoading {
                            QuoteCardShimmer()
                        } else if let quote = viewModel.dailyQuote {
                            QuoteCardView(quote: quote)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 20)

                    // ── Section title ────────────────────────────────────────────
                    HStack(spacing: 8) {
                        Text("✦")
                            .font(AppFonts.labelSmall)
                            .foregroundStyle(AppColors.accentGold)
                        Text(String.appLocalized("today_section_title"))
                            .font(AppFonts.labelSmall)
                            .foregroundStyle(AppColors.textSecondary)
                            .textCase(.uppercase)
                            .tracking(2)
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 22)

                    // ── Feature entry cards ──────────────────────────────────────
                    VStack(spacing: 14) {
                        NavigationLink { TarotView() } label: {
                            FeatureEntryCard(
                                symbol: "moon.stars.fill",
                                symbolColor: AppColors.accentGold,
                                title: String.appLocalized("home_tarot_title"),
                                subtitle: String.appLocalized("home_tarot_desc")
                            )
                        }
                        NavigationLink { AstrologyView() } label: {
                            FeatureEntryCard(
                                symbol: "sparkles",
                                symbolColor: AppColors.accentPurple,
                                title: String.appLocalized("home_astrology_title"),
                                subtitle: String.appLocalized("home_astrology_desc")
                            )
                        }
                        NavigationLink { BaziView() } label: {
                            FeatureEntryCard(
                                symbol: "leaf.fill",
                                symbolColor: AppColors.accentRose,
                                title: String.appLocalized("home_bazi_title"),
                                subtitle: String.appLocalized("home_bazi_desc")
                            )
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 14)

                    Spacer(minLength: 28)
                }
                .frame(minHeight: geo.size.height)
            }
        }
        .background(AppColors.gradientBackground.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
    }
}

// MARK: - Header

private struct HomeHeaderView: View {
    private var isZh: Bool {
        AppLanguageOption.isChinese
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Text("✦")
                    .font(AppFonts.labelSmall)
                    .foregroundStyle(AppColors.accentGold.opacity(0.7))
                Text(isZh ? "口袋占卜屋" : "POCKET ORACLE")
                    .font(AppFonts.labelSmall)
                    .foregroundStyle(AppColors.textMuted)
                    .tracking(3)
            }
            Text(isZh ? "口袋占卜屋" : "Pocket Oracle")
                .font(AppFonts.displayMedium)
                .foregroundStyle(AppColors.textPrimary)
            Text(Date().localizedDateString)
                .font(AppFonts.bodySmall)
                .foregroundStyle(AppColors.textSecondary)
        }
    }
}

// MARK: - Quote Card

private struct QuoteCardView: View {
    let quote: DailyQuote

    var body: some View {
        VStack(spacing: 0) {
            // Stars decoration
            HStack(spacing: 12) {
                Rectangle()
                    .fill(AppColors.accentGold.opacity(0.3))
                    .frame(height: 0.5)
                Text("✦ ✧ ✦")
                    .font(AppFonts.labelSmall)
                    .foregroundStyle(AppColors.accentGold.opacity(0.6))
                Rectangle()
                    .fill(AppColors.accentGold.opacity(0.3))
                    .frame(height: 0.5)
            }
            .padding(.horizontal, 20)
            .padding(.top, 12)

            // Quote text
            Text(quote.localizedText)
                .font(AppFonts.bodyMedium)
                .foregroundStyle(AppColors.textPrimary)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .padding(.horizontal, 24)
                .padding(.top, 12)
                .padding(.bottom, 10)

            // Attribution
            Text("— Fortune Pocket")
                .font(AppFonts.caption)
                .foregroundStyle(AppColors.accentGold.opacity(0.7))
                .padding(.bottom, 12)
        }
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(AppColors.backgroundElevated)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(
                            LinearGradient(
                                colors: [AppColors.accentGold.opacity(0.5), AppColors.accentGold.opacity(0.15)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            ),
                            lineWidth: 1
                        )
                )
        )
    }
}

// MARK: - Quote Shimmer (loading state)

private struct QuoteCardShimmer: View {
    @State private var phase: CGFloat = 0

    var body: some View {
        RoundedRectangle(cornerRadius: 20)
            .fill(AppColors.backgroundElevated)
            .frame(height: 140)
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(AppColors.accentGold.opacity(0.2), lineWidth: 1)
            )
    }
}

// MARK: - Feature Entry Card

private struct FeatureEntryCard: View {
    let symbol: String
    let symbolColor: Color
    let title: String
    let subtitle: String

    var body: some View {
        HStack(spacing: 16) {
            // Icon
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(symbolColor.opacity(0.15))
                    .frame(width: 48, height: 48)
                Image(systemName: symbol)
                    .font(.system(size: 20, weight: .light))
                    .foregroundStyle(symbolColor)
            }

            // Text
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.textPrimary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.9)
                Text(subtitle)
                    .font(AppFonts.bodySmall)
                    .foregroundStyle(AppColors.textSecondary)
                    .multilineTextAlignment(.leading)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Spacer()

            // Chevron
            Image(systemName: "chevron.right")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(AppColors.textMuted)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(AppColors.backgroundElevated)
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(AppColors.divider, lineWidth: 0.5)
                )
        )
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        HomeView()
    }
    .modelContainer(for: ReadingRecord.self, inMemory: true)
}
