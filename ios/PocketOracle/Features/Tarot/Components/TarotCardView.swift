import SwiftUI

// MARK: - Card size presets

enum CardSize {
    case tiny    // ~52 pt wide  – pick deck grid
    case small   // ~78 pt wide  – result row
    case medium  // ~100 pt wide – draw screen
    case large   // ~130 pt wide – standalone

    var width: CGFloat {
        switch self { case .tiny: 52; case .small: 78; case .medium: 100; case .large: 130 }
    }
    var height:       CGFloat { width * 1.6 }
    var symbolSize:   CGFloat { width * 0.30 }
    var circleDiam:   CGFloat { width * 0.54 }
    var nameFontSize: CGFloat { width * 0.095 }
    var tagFontSize:  CGFloat { width * 0.08 }
}

// MARK: - Public view

/// Renders a tarot card either face-up (using the card's colour palette) or face-down.
/// Pass `card: nil` or `isFaceDown: true` to show the decorative back.
struct TarotCardView: View {
    var card:      TarotCard? = nil
    var isUpright: Bool       = true
    var isFaceDown: Bool      = false
    var size:      CardSize   = .medium

    var body: some View {
        Group {
            if isFaceDown || card == nil {
                CardBackView(size: size)
            } else if let c = card {
                CardFrontView(card: c, isUpright: isUpright, size: size)
            }
        }
    }
}

// MARK: - Front face

private struct CardFrontView: View {
    let card:      TarotCard
    let isUpright: Bool
    let size:      CardSize

    private var primary: Color { Color(hex: card.colorPrimary) }
    private var accent:  Color { Color(hex: card.colorAccent)  }

    private var isZh: Bool {
        AppLanguageOption.isChinese
    }

    var body: some View {
        ZStack {
            // Background gradient using the card's primary colour
            RoundedRectangle(cornerRadius: 12)
                .fill(LinearGradient(
                    colors: [primary, primary.opacity(0.70)],
                    startPoint: .top, endPoint: .bottom
                ))

            // Gold border
            RoundedRectangle(cornerRadius: 12)
                .strokeBorder(AppColors.accentGold.opacity(0.55), lineWidth: 1.2)

            VStack(spacing: size.width * 0.07) {
                // Symbol circle
                ZStack {
                    Circle()
                        .fill(accent.opacity(0.22))
                        .frame(width: size.circleDiam, height: size.circleDiam)
                    Text(card.symbol)
                        .font(.system(size: size.symbolSize))
                        .foregroundStyle(accent)
                }

                // Card name
                Text(isZh ? card.nameZh : card.nameEn)
                    .font(.system(size: size.nameFontSize, weight: .medium))
                    .foregroundStyle(.white.opacity(0.9))
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .padding(.horizontal, 4)

                // Upright / Reversed tag
                HStack(spacing: 4) {
                    Image(systemName: isUpright ? "arrow.up" : "arrow.uturn.down")
                        .font(.system(size: size.tagFontSize - 1, weight: .semibold))
                    Text(isUpright
                         ? String.appLocalized("tarot_upright")
                         : String.appLocalized("tarot_reversed"))
                        .font(.system(size: size.tagFontSize, weight: .semibold))
                }
                .foregroundStyle(accent.opacity(0.92))
                .padding(.horizontal, 6)
                .padding(.vertical, 3)
                .background(Capsule().fill(.white.opacity(0.08)))
            }
        }
        .frame(width: size.width, height: size.height)
    }
}

// MARK: - Back face

struct CardBackView: View {
    let size: CardSize

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 12)
                .fill(LinearGradient(
                    colors: [AppColors.backgroundElevated, Color(hex: "#2A1F50")],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                ))

            RoundedRectangle(cornerRadius: 12)
                .strokeBorder(AppColors.accentGold.opacity(0.35), lineWidth: 1.2)

            // Inner decorative frame
            RoundedRectangle(cornerRadius: 8)
                .strokeBorder(AppColors.accentGold.opacity(0.15), lineWidth: 0.5)
                .padding(6)

            Text("✦")
                .font(.system(size: size.width * 0.38))
                .foregroundStyle(AppColors.accentGold.opacity(0.35))
        }
        .frame(width: size.width, height: size.height)
    }
}

// MARK: - Flip animation (back → front)

/// Shows a face-down card that flips to face-up after `flipDelay` seconds.
struct FlippingTarotCardView: View {
    let drawn: DrawnCard
    let size: CardSize
    var flipDelay: Double = 0

    @State private var flipped = false

    var body: some View {
        ZStack {
            // Back face — rotates away
            TarotCardView(isFaceDown: true, size: size)
                .rotation3DEffect(.degrees(flipped ? 180 : 0), axis: (0, 1, 0))
                .opacity(flipped ? 0 : 1)

            // Front face — rotates in
            TarotCardView(card: drawn.card, isUpright: drawn.isUpright, size: size)
                .rotation3DEffect(.degrees(flipped ? 0 : -180), axis: (0, 1, 0))
                .opacity(flipped ? 1 : 0)
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + flipDelay) {
                withAnimation(.spring(duration: 0.55, bounce: 0.15)) {
                    flipped = true
                }
            }
        }
    }
}
