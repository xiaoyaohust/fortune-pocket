import SwiftUI

struct HomeView: View {

    var viewModel: HomeViewModel
    let onOpenDailyRitual: () -> Void

    private var isZh: Bool { AppLanguageOption.isChinese }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 22) {
                HomeHeaderView()
                    .padding(.top, 12)

                if viewModel.isLoading {
                    RitualHeroSkeleton()
                    DailyPreviewSkeleton()
                } else if let ritual = viewModel.dailyRitual {
                    RitualHeroCard(ritual: ritual, onOpenDailyRitual: onOpenDailyRitual)
                    DailyPreviewStack(ritual: ritual)
                }

                if let errorMessage = viewModel.errorMessage {
                    Text(errorMessage)
                        .font(AppFonts.bodySmall)
                        .foregroundStyle(AppColors.textSecondary)
                        .padding(.horizontal, 4)
                }

                ExploreSection()
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 32)
        }
        .background(AppColors.gradientBackground.ignoresSafeArea())
        .toolbar(.hidden, for: .navigationBar)
        .onAppear {
            viewModel.load()
        }
    }
}

private struct HomeHeaderView: View {
    private var isZh: Bool { AppLanguageOption.isChinese }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 6) {
                Text("✦")
                    .font(AppFonts.labelSmall)
                    .foregroundStyle(AppColors.accentGold.opacity(0.72))
                Text(isZh ? "口袋占卜屋" : "POCKET ORACLE")
                    .font(AppFonts.labelSmall)
                    .foregroundStyle(AppColors.textMuted)
                    .tracking(3)
            }

            Text(isZh ? "今晚想先看哪一道光？" : "Which doorway feels right tonight?")
                .font(AppFonts.displayMedium)
                .foregroundStyle(AppColors.textPrimary)

            Text(Date().localizedDateString)
                .font(AppFonts.bodySmall)
                .foregroundStyle(AppColors.textSecondary)
        }
    }
}

private struct RitualHeroCard: View {
    let ritual: DailyRitual
    let onOpenDailyRitual: () -> Void

    private var isZh: Bool { AppLanguageOption.isChinese }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(isZh ? "今日仪式" : "Today's Ritual")
                .font(AppFonts.labelSmall)
                .foregroundStyle(AppColors.accentGold)
                .tracking(2)

            Text(ritual.ritualTitle)
                .font(AppFonts.displaySmall)
                .foregroundStyle(AppColors.textPrimary)

            Text(ritual.ritualSummary)
                .font(AppFonts.bodyMedium)
                .foregroundStyle(AppColors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            VStack(alignment: .leading, spacing: 10) {
                Label(ritual.destination.localizedTitle, systemImage: ritual.destination.icon)
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.textPrimary)

                Text(ritual.destinationReason)
                    .font(AppFonts.bodySmall)
                    .foregroundStyle(AppColors.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .padding(14)
            .background(AppColors.backgroundBase.opacity(0.92))
            .clipShape(RoundedRectangle(cornerRadius: 18))

            Button(action: onOpenDailyRitual) {
                HStack(spacing: 10) {
                    Image(systemName: "sparkles.rectangle.stack.fill")
                    Text(isZh ? "进入今日仪式" : "Open Today's Ritual")
                }
                .font(AppFonts.titleMedium)
                .foregroundStyle(AppColors.backgroundDeep)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(AppColors.accentGold)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
        .padding(22)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 28)
                .fill(
                    LinearGradient(
                        colors: [
                            AppColors.backgroundElevated,
                            AppColors.backgroundBase.opacity(0.95)
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 28)
                        .stroke(AppColors.accentGold.opacity(0.28), lineWidth: 1)
                )
        )
    }
}

private struct DailyPreviewStack: View {
    let ritual: DailyRitual

    var body: some View {
        VStack(spacing: 14) {
            DailyPreviewCard(
                icon: "moonphase.waning.gibbous",
                title: ritual.tarot.title,
                headline: "\(ritual.tarot.card.localizedName) · \(ritual.tarot.isUpright ? (AppLanguageOption.isChinese ? "正位" : "Upright") : (AppLanguageOption.isChinese ? "逆位" : "Reversed"))",
                detail: ritual.tarot.insight,
                tags: ritual.tarot.keywords
            )

            DailyPreviewCard(
                icon: "sparkles",
                title: ritual.energyTitle,
                headline: ritual.quote.localizedText,
                detail: ritual.energyBody,
                tags: []
            )

            DailyPreviewCard(
                icon: "star.leadinghalf.filled",
                title: ritual.transit.title,
                headline: ritual.transit.signName ?? (AppLanguageOption.isChinese ? "等待你的生日" : "Waiting for your birthday"),
                detail: ritual.transit.summary,
                tags: ritual.transit.needsProfileCompletion
                    ? [AppLanguageOption.isChinese ? "补充生日后更准确" : "Save a birthday for more detail"]
                    : []
            )
        }
    }
}

private struct DailyPreviewCard: View {
    let icon: String
    let title: String
    let headline: String
    let detail: String
    let tags: [String]

    var bodyView: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(title, systemImage: icon)
                .font(AppFonts.titleMedium)
                .foregroundStyle(AppColors.accentGold)

            Text(headline)
                .font(AppFonts.titleMedium)
                .foregroundStyle(AppColors.textPrimary)
                .fixedSize(horizontal: false, vertical: true)

            Text(detail)
                .font(AppFonts.bodySmall)
                .foregroundStyle(AppColors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)

            if !tags.isEmpty {
                HStack(spacing: 8) {
                    ForEach(tags, id: \.self) { tag in
                        Text(tag)
                            .font(AppFonts.caption)
                            .foregroundStyle(AppColors.accentGold)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 6)
                            .background(AppColors.backgroundBase)
                            .clipShape(Capsule())
                    }
                }
            }
        }
        .padding(18)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
    }

    var body: some View { bodyView }
}

private struct ExploreSection: View {
    private var isZh: Bool { AppLanguageOption.isChinese }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 8) {
                Text("✦")
                    .font(AppFonts.labelSmall)
                    .foregroundStyle(AppColors.accentGold)
                Text(isZh ? "继续探索" : "Keep Exploring")
                    .font(AppFonts.labelSmall)
                    .foregroundStyle(AppColors.textSecondary)
                    .tracking(2)
            }

            VStack(spacing: 12) {
                NavigationLink {
                    TarotView()
                } label: {
                    FeatureEntryCard(
                        symbol: "moon.stars.fill",
                        symbolColor: AppColors.accentGold,
                        title: String.appLocalized("home_tarot_title"),
                        subtitle: isZh ? "完整三牌阵、主题聚焦与分享结果" : "Full three-card spreads, themed focus, and shareable results"
                    )
                }

                NavigationLink {
                    AstrologyView()
                } label: {
                    FeatureEntryCard(
                        symbol: "sparkles",
                        symbolColor: AppColors.accentPurple,
                        title: String.appLocalized("home_astrology_title"),
                        subtitle: isZh ? "打开太阳、月亮、上升和主要相位" : "Open your Sun, Moon, Rising, and major aspects"
                    )
                }

                NavigationLink {
                    BaziView()
                } label: {
                    FeatureEntryCard(
                        symbol: "leaf.fill",
                        symbolColor: AppColors.accentRose,
                        title: String.appLocalized("home_bazi_title"),
                        subtitle: isZh ? "从四柱、十神和五行里慢慢看自己" : "Read your chart through pillars, ten gods, and elements"
                    )
                }
            }
        }
    }
}

private struct RitualHeroSkeleton: View {
    var body: some View {
        RoundedRectangle(cornerRadius: 28)
            .fill(AppColors.backgroundElevated)
            .frame(height: 280)
            .overlay(
                RoundedRectangle(cornerRadius: 28)
                    .stroke(AppColors.accentGold.opacity(0.18), lineWidth: 1)
            )
    }
}

private struct DailyPreviewSkeleton: View {
    var body: some View {
        VStack(spacing: 14) {
            ForEach(0..<3, id: \.self) { _ in
                RoundedRectangle(cornerRadius: 20)
                    .fill(AppColors.backgroundElevated)
                    .frame(height: 132)
            }
        }
    }
}

private struct FeatureEntryCard: View {
    let symbol: String
    let symbolColor: Color
    let title: String
    let subtitle: String

    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 14)
                    .fill(symbolColor.opacity(0.15))
                    .frame(width: 48, height: 48)
                Image(systemName: symbol)
                    .font(.system(size: 20, weight: .light))
                    .foregroundStyle(symbolColor)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.textPrimary)
                Text(subtitle)
                    .font(AppFonts.bodySmall)
                    .foregroundStyle(AppColors.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Image(systemName: "chevron.right")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(AppColors.textMuted)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 18)
                .fill(AppColors.backgroundElevated)
                .overlay(
                    RoundedRectangle(cornerRadius: 18)
                        .stroke(AppColors.divider, lineWidth: 0.5)
                )
        )
    }
}

#Preview {
    NavigationStack {
        HomeView(viewModel: HomeViewModel(), onOpenDailyRitual: {})
    }
    .modelContainer(for: ReadingRecord.self, inMemory: true)
}
