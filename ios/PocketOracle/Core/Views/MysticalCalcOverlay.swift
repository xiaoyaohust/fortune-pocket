import SwiftUI

/// Full-screen mystical "calculating" overlay shown during Bazi / Astrology generation.
struct MysticalCalcOverlay: View {
    let label: String
    let symbols: [String]

    @State private var rotation: Double = 0
    @State private var innerRotation: Double = 0
    @State private var glowPulse = false
    @State private var appeared = false

    var body: some View {
        ZStack {
            AppColors.backgroundDeep.opacity(0.93)
                .ignoresSafeArea()

            VStack(spacing: 28) {
                ZStack {
                    // Outer ring — 12 symbols orbiting
                    ForEach(0..<symbols.count, id: \.self) { i in
                        Text(symbols[i])
                            .font(.system(size: 15, weight: .light))
                            .foregroundStyle(AppColors.accentGold.opacity(0.65))
                            .offset(y: -78)
                            .rotationEffect(.degrees(Double(i) * (360.0 / Double(symbols.count)) + rotation))
                    }

                    // Inner counter-ring (smaller, counter-rotating)
                    ForEach(0..<6, id: \.self) { i in
                        Circle()
                            .fill(AppColors.accentGold.opacity(0.18))
                            .frame(width: 4, height: 4)
                            .offset(y: -44)
                            .rotationEffect(.degrees(Double(i) * 60 + innerRotation))
                    }

                    // Center glyph
                    Text(symbols.first ?? "✦")
                        .font(.system(size: 42))
                        .foregroundStyle(AppColors.accentGold)
                        .scaleEffect(glowPulse ? 1.12 : 1.0)
                        .animation(.easeInOut(duration: 1.1).repeatForever(autoreverses: true), value: glowPulse)
                }
                .frame(width: 200, height: 200)

                Text(label)
                    .font(AppFonts.bodyMedium)
                    .foregroundStyle(AppColors.textSecondary)
            }
            .opacity(appeared ? 1 : 0)
            .scaleEffect(appeared ? 1 : 0.92)
        }
        .onAppear {
            withAnimation(.easeOut(duration: 0.35)) { appeared = true }
            glowPulse = true
            withAnimation(.linear(duration: 8).repeatForever(autoreverses: false)) {
                rotation = 360
            }
            withAnimation(.linear(duration: 4).repeatForever(autoreverses: false)) {
                innerRotation = -360
            }
        }
    }
}
