import SwiftUI

struct TarotResultView: View {
    let reading: TarotReading
    let onDone: () -> Void
    var animateReveal: Bool = false

    // Pre-computed once — avoids rebuilding on every render
    private let shareText: String

    init(reading: TarotReading, onDone: @escaping () -> Void, animateReveal: Bool = false) {
        self.reading = reading
        self.onDone = onDone
        self.animateReveal = animateReveal
        self.shareText = ReadingPresentationBuilder.shareText(for: reading)
    }

    private var isZh: Bool {
        AppLanguageOption.isChinese
    }

    private var hasReversedCards: Bool {
        reading.drawnCards.contains { !$0.isUpright }
    }

    var body: some View {
        GeometryReader { proxy in
            let cardSize: CardSize = proxy.size.width >= 360 ? .medium : .small
            let horizontalPadding = proxy.size.width >= 390 ? 24.0 : 18.0
            let bottomPadding = max(24.0, proxy.safeAreaInsets.bottom + 18.0)

            ScrollView {
                VStack(spacing: 0) {
                    headerSection(topPadding: 10)
                    cardsSection(cardSize: cardSize, horizontalPadding: horizontalPadding)
                    if hasReversedCards {
                        orientationHintSection(horizontalPadding: horizontalPadding)
                    }
                    goldDivider(horizontalPadding)
                    ReadingSectionBlock(
                        icon: "sparkles",
                        title: String.appLocalized("tarot_overall_energy"),
                        content: reading.overallEnergy
                    )
                    goldDivider(horizontalPadding)
                    ReadingSectionBlock(
                        icon: "scope",
                        title: reading.theme.localizedFocusTitle,
                        content: reading.focusInsight
                    )

                    ForEach(reading.positionReadings) { item in
                        goldDivider(horizontalPadding)
                        ReadingSectionBlock(
                            icon: aspectIcon(for: item.aspect),
                            title: item.positionLabel,
                            content: item.interpretation
                        )
                    }

                    goldDivider(horizontalPadding)
                    ReadingSectionBlock(
                        icon: "wand.and.stars",
                        title: String.appLocalized("tarot_advice"),
                        content: reading.advice
                    )
                    goldDivider(horizontalPadding)
                    luckySection(horizontalPadding: horizontalPadding)
                    actionButtons(horizontalPadding: horizontalPadding)
                }
                .frame(maxWidth: .infinity, alignment: .top)
                .padding(.bottom, bottomPadding)
            }
            .scrollIndicators(.hidden)
        }
    }

    private func headerSection(topPadding: CGFloat) -> some View {
        VStack(spacing: 8) {
            Text("✦")
                .font(AppFonts.titleLarge)
                .foregroundStyle(AppColors.accentGold)
                .padding(.top, topPadding)
            Text(reading.spreadName)
                .font(AppFonts.headlineLarge)
                .foregroundStyle(AppColors.textPrimary)
            Text(reading.theme.localizedName)
                .font(AppFonts.bodyMedium)
                .foregroundStyle(AppColors.accentGold.opacity(0.85))
            Text(reading.date.localizedDateString)
                .font(AppFonts.bodySmall)
                .foregroundStyle(AppColors.textSecondary)
            Text(reading.spreadDescription)
                .font(AppFonts.bodySmall)
                .foregroundStyle(AppColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 28)
        }
        .padding(.bottom, 20)
    }

    private func cardsSection(cardSize: CardSize, horizontalPadding: CGFloat) -> some View {
        HStack(alignment: .top, spacing: 12) {
            ForEach(Array(reading.drawnCards.enumerated()), id: \.element.id) { idx, drawn in
                VStack(spacing: 8) {
                    if animateReveal {
                        FlippingTarotCardView(
                            drawn: drawn,
                            size: cardSize,
                            flipDelay: Double(idx) * 0.45
                        )
                    } else {
                        TarotCardView(
                            card: drawn.card,
                            isUpright: drawn.isUpright,
                            isFaceDown: false,
                            size: cardSize
                        )
                    }
                    Text(drawn.positionLabel)
                        .font(AppFonts.labelSmall)
                        .foregroundStyle(AppColors.textSecondary)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .minimumScaleFactor(0.9)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, horizontalPadding)
        .padding(.bottom, 20)
    }

    private func orientationHintSection(horizontalPadding: CGFloat) -> some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "info.circle")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(AppColors.accentGold)
                .padding(.top, 1)
            Text(String.appLocalized("tarot_orientation_result"))
                .font(AppFonts.bodySmall)
                .foregroundStyle(AppColors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, horizontalPadding)
        .padding(.bottom, 18)
    }

    private func luckySection(horizontalPadding: CGFloat) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(String.appLocalized("tarot_lucky"))
                .font(AppFonts.titleMedium)
                .foregroundStyle(AppColors.accentGold)

            HStack(spacing: 0) {
                LuckyCell(
                    label: isZh ? "幸运物" : "Lucky Charm",
                    value: reading.luckyItemName
                )
                verticalGoldLine
                VStack(spacing: 6) {
                    Text(isZh ? "幸运色" : "Lucky Color")
                        .font(AppFonts.labelSmall)
                        .foregroundStyle(AppColors.textSecondary)
                    HStack(spacing: 6) {
                        Circle()
                            .fill(Color(hex: reading.luckyColorHex))
                            .frame(width: 14, height: 14)
                        Text(reading.luckyColorName)
                            .font(AppFonts.bodySmall)
                            .foregroundStyle(AppColors.textPrimary)
                    }
                }
                .frame(maxWidth: .infinity)
                verticalGoldLine
                LuckyCell(
                    label: isZh ? "幸运数字" : "Lucky Number",
                    value: "\(reading.luckyNumber)",
                    valueFont: AppFonts.accentLarge,
                    valueColor: AppColors.accentGold
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, horizontalPadding)
        .padding(.vertical, 20)
    }

    private func actionButtons(horizontalPadding: CGFloat) -> some View {
        HStack(spacing: 12) {
            ShareLink(item: shareText) {
                Label(String.appLocalized("share"), systemImage: "square.and.arrow.up")
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.textPrimary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(AppColors.backgroundElevated)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(
                        RoundedRectangle(cornerRadius: 16)
                            .stroke(AppColors.accentGold.opacity(0.35), lineWidth: 1)
                    )
            }

            Button(action: onDone) {
                Text(isZh ? "保存并完成" : "Save & Done")
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.backgroundDeep)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(AppColors.accentGold)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
        .padding(.horizontal, horizontalPadding)
        .padding(.top, 20)
    }

    private func goldDivider(_ horizontalPadding: CGFloat) -> some View {
        Rectangle()
            .fill(AppColors.accentGold.opacity(0.15))
            .frame(height: 0.5)
            .padding(.horizontal, horizontalPadding)
    }

    private var verticalGoldLine: some View {
        Rectangle()
            .fill(AppColors.accentGold.opacity(0.25))
            .frame(width: 0.5, height: 36)
    }

    private func aspectIcon(for aspect: String) -> String {
        switch aspect {
        case "past": return "clock.arrow.circlepath"
        case "present": return "circle.hexagongrid"
        case "next_step": return "arrow.right.circle"
        case "self_in_love": return "person.crop.circle"
        case "connection": return "link"
        case "gentle_advice": return "heart.text.square"
        case "current_path": return "briefcase"
        case "hidden_block": return "exclamationmark.triangle"
        case "opportunity": return "sparkles.rectangle.stack"
        case "current_flow": return "arrow.left.arrow.right.circle"
        case "resource_pattern": return "chart.pie"
        case "next_move": return "arrow.up.right.circle"
        default: return "circle.grid.cross"
        }
    }
}

private struct ReadingSectionBlock: View {
    let icon: String
    let title: String
    let content: String

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(title, systemImage: icon)
                .font(AppFonts.titleMedium)
                .foregroundStyle(AppColors.accentGold)
            Text(content)
                .font(AppFonts.bodyMedium)
                .foregroundStyle(AppColors.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .lineSpacing(4)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
    }
}

private struct LuckyCell: View {
    let label: String
    let value: String
    var valueFont: Font = AppFonts.bodySmall
    var valueColor: Color = AppColors.textPrimary

    var body: some View {
        VStack(spacing: 6) {
            Text(label)
                .font(AppFonts.labelSmall)
                .foregroundStyle(AppColors.textSecondary)
            Text(value)
                .font(valueFont)
                .foregroundStyle(valueColor)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
    }
}
