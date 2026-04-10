import SwiftUI

struct DailyRitualView: View {
    let ritual: DailyRitual
    let onDismiss: () -> Void

    private var isZh: Bool { AppLanguageOption.isChinese }

    var body: some View {
        NavigationStack {
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 20) {
                    ritualHero
                    tarotCard
                    energyCard
                    transitCard
                    ritualPrompt
                }
                .padding(.horizontal, 20)
                .padding(.top, 20)
                .padding(.bottom, 32)
            }
            .background(AppColors.gradientBackground.ignoresSafeArea())
            .navigationTitle(isZh ? "今日仪式" : "Today's Ritual")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(isZh ? "完成" : "Done", action: onDismiss)
                        .foregroundStyle(AppColors.accentGold)
                }
            }
        }
    }

    private var ritualHero: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(isZh ? "今日入口" : "Today's Doorway")
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

            HStack(spacing: 10) {
                Image(systemName: ritual.destination.icon)
                    .foregroundStyle(AppColors.accentGold)
                VStack(alignment: .leading, spacing: 4) {
                    Text(ritual.destination.localizedTitle)
                        .font(AppFonts.titleMedium)
                        .foregroundStyle(AppColors.textPrimary)
                    Text(ritual.destinationReason)
                        .font(AppFonts.bodySmall)
                        .foregroundStyle(AppColors.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .padding(14)
            .background(AppColors.backgroundBase.opacity(0.9))
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 24)
    }

    private var tarotCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            sectionLabel(ritual.tarot.title)

            HStack(spacing: 16) {
                TarotCardView(
                    card: ritual.tarot.card,
                    isUpright: ritual.tarot.isUpright,
                    isFaceDown: false,
                    size: .small
                )

                VStack(alignment: .leading, spacing: 8) {
                    Text("\(ritual.tarot.card.localizedName) · \(ritual.tarot.isUpright ? (isZh ? "正位" : "Upright") : (isZh ? "逆位" : "Reversed"))")
                        .font(AppFonts.titleMedium)
                        .foregroundStyle(AppColors.textPrimary)

                    if !ritual.tarot.keywords.isEmpty {
                        FlexibleTagRow(tags: ritual.tarot.keywords)
                    }

                    Text(ritual.tarot.insight)
                        .font(AppFonts.bodySmall)
                        .foregroundStyle(AppColors.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 22)
    }

    private var energyCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionLabel(ritual.energyTitle)
            Text(ritual.energyBody)
                .font(AppFonts.bodyMedium)
                .foregroundStyle(AppColors.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            Text("“\(ritual.quote.localizedText)”")
                .font(AppFonts.bodySmall)
                .foregroundStyle(AppColors.textSecondary)
                .italic()
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 22)
    }

    private var transitCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionLabel(ritual.transit.title)
            if let signName = ritual.transit.signName {
                Text(signName)
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.accentGold)
            }
            Text(ritual.transit.summary)
                .font(AppFonts.bodyMedium)
                .foregroundStyle(AppColors.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            if ritual.transit.needsProfileCompletion {
                Text(isZh ? "补充生日后，这里的每日摘要会更贴近你。" : "Save a birthday to make this daily note feel more personal.")
                    .font(AppFonts.caption)
                    .foregroundStyle(AppColors.textSecondary)
            }
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 22)
    }

    private var ritualPrompt: some View {
        VStack(alignment: .leading, spacing: 12) {
            sectionLabel(ritual.promptTitle)
            Text(ritual.promptBody)
                .font(AppFonts.bodyMedium)
                .foregroundStyle(AppColors.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 22)
    }

    private func sectionLabel(_ text: String) -> some View {
        Label(text, systemImage: "sparkles")
            .font(AppFonts.titleMedium)
            .foregroundStyle(AppColors.accentGold)
    }
}

private struct FlexibleTagRow: View {
    let tags: [String]

    var body: some View {
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
