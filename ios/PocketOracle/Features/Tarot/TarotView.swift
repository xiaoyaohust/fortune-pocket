import SwiftUI
import SwiftData

struct TarotView: View {
    @State private var vm = TarotViewModel()
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            TarotTopBar(
                title: String.appLocalized("home_tarot_title"),
                showBack: shouldShowBack
            ) { handleBack() }
                .padding(.top, 2)
                .padding(.horizontal, 20)
                .padding(.bottom, 10)

            Group {
                switch vm.phase {
                case .idle:
                    TarotIdleView(vm: vm)
                        .transition(.asymmetric(
                            insertion: .opacity,
                            removal: .opacity
                        ))
                case .shuffling:
                    TarotShuffleAnimView { vm.commitShuffle() }
                        .transition(.opacity)
                case .picking(let deck):
                    TarotPickDeckView(vm: vm, deck: deck)
                        .transition(.opacity)
                case .result(let reading):
                    TarotResultView(reading: reading, onDone: {
                        vm.saveToHistory(context: modelContext)
                        withAnimation { vm.reset() }
                    }, animateReveal: true)
                    .transition(.opacity)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .animation(.easeInOut(duration: 0.38), value: vm.phaseIndex)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(AppColors.gradientBackground.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .fortuneTabBarHidden()
    }

    private var shouldShowBack: Bool {
        switch vm.phase {
        case .idle, .result: return true
        case .shuffling, .picking: return false
        }
    }

    private func handleBack() {
        switch vm.phase {
        case .idle: dismiss()
        case .result: withAnimation { vm.reset() }
        default: break
        }
    }
}

// MARK: - Top Bar

private struct TarotTopBar: View {
    let title: String
    let showBack: Bool
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            if showBack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(AppColors.accentGold)
                        .frame(width: 40, height: 40)
                        .background(AppColors.backgroundElevated.opacity(0.92))
                        .clipShape(Circle())
                        .overlay(Circle().stroke(AppColors.accentGold.opacity(0.25), lineWidth: 1))
                }
                .buttonStyle(.plain)
            } else {
                Color.clear.frame(width: 40, height: 40)
            }

            Text(title)
                .font(AppFonts.headlineMedium)
                .foregroundStyle(AppColors.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)

            Color.clear.frame(width: 40, height: 40)
        }
    }
}

// MARK: - Idle (theme selection)

private struct TarotIdleView: View {
    let vm: TarotViewModel
    private var isZh: Bool { AppLanguageOption.isChinese }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                // Spread info
                VStack(spacing: 10) {
                    Text(vm.selectedTheme.localizedEntryTitle)
                        .font(AppFonts.headlineMedium)
                        .foregroundStyle(AppColors.textPrimary)
                        .multilineTextAlignment(.center)
                    Text(vm.selectedTheme.localizedEntrySubtitle)
                        .font(AppFonts.bodySmall)
                        .foregroundStyle(AppColors.textSecondary)
                        .multilineTextAlignment(.center)
                    Text(vm.selectedTheme.localizedPromptDescription)
                        .font(AppFonts.bodySmall)
                        .foregroundStyle(AppColors.textSecondary.opacity(0.9))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 16)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity)

                // Theme grid
                LazyVGrid(
                    columns: [GridItem(.flexible()), GridItem(.flexible())],
                    spacing: 10
                ) {
                    ForEach(TarotQuestionTheme.allCases) { theme in
                        Button { vm.setTheme(theme) } label: {
                            Text(theme.localizedName)
                                .font(AppFonts.caption)
                                .foregroundStyle(
                                    vm.selectedTheme == theme
                                        ? AppColors.backgroundDeep : AppColors.textPrimary
                                )
                                .frame(maxWidth: .infinity)
                                .frame(height: 46)
                                .background(
                                    vm.selectedTheme == theme
                                        ? AppColors.accentGold : AppColors.backgroundElevated
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 14))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 14).stroke(
                                        vm.selectedTheme == theme
                                            ? AppColors.accentGold : AppColors.accentGold.opacity(0.15),
                                        lineWidth: 1
                                    )
                                )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.top, 18)

                VStack(alignment: .leading, spacing: 12) {
                    Text(isZh ? "选择牌阵" : "Choose a spread")
                        .font(AppFonts.titleMedium)
                        .foregroundStyle(AppColors.textPrimary)

                    LazyVGrid(
                        columns: [GridItem(.flexible()), GridItem(.flexible())],
                        spacing: 10
                    ) {
                        ForEach(vm.selectedTheme.availableSpreadStyles) { style in
                            Button { vm.setSpreadStyle(style) } label: {
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(style.localizedName())
                                        .font(AppFonts.bodySmall)
                                        .foregroundStyle(
                                            vm.selectedSpreadStyle == style
                                                ? AppColors.backgroundDeep : AppColors.textPrimary
                                        )
                                        .frame(maxWidth: .infinity, alignment: .leading)

                                    Text(style.localizedSubtitle())
                                        .font(AppFonts.caption)
                                        .foregroundStyle(
                                            vm.selectedSpreadStyle == style
                                                ? AppColors.backgroundDeep.opacity(0.82)
                                                : AppColors.textSecondary
                                        )
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .lineLimit(2)
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 12)
                                .frame(maxWidth: .infinity, minHeight: 78, alignment: .leading)
                                .background(
                                    vm.selectedSpreadStyle == style
                                        ? AppColors.accentGold : AppColors.backgroundElevated
                                )
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16).stroke(
                                        vm.selectedSpreadStyle == style
                                            ? AppColors.accentGold : AppColors.accentGold.opacity(0.15),
                                        lineWidth: 1
                                    )
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    Text(vm.selectedSpreadStyle.localizedDescription())
                        .font(AppFonts.bodySmall)
                        .foregroundStyle(AppColors.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.top, 18)

                // Face-down cards preview
                HStack(spacing: 12) {
                    ForEach(0..<vm.requiredPickCount, id: \.self) { _ in
                        TarotCardView(isFaceDown: true, size: .small)
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 18)

                // Instruction
                VStack(spacing: 6) {
                    Text(isZh
                         ? "先确认主题与牌阵，再静心洗牌。你会抽出 \(vm.requiredPickCount) 张牌，得到更贴近问题的回答。"
                         : "Choose your focus and spread first, then shuffle. You'll draw \(vm.requiredPickCount) card(s) for a clearer answer.")
                        .font(AppFonts.bodyMedium)
                        .foregroundStyle(AppColors.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)

                    Text(String.appLocalized("tarot_orientation_prompt"))
                        .font(AppFonts.caption)
                        .foregroundStyle(AppColors.accentGold.opacity(0.88))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                }
                .padding(.top, 18)

                // Shuffle button
                Button { vm.startShuffle() } label: {
                    HStack(spacing: 8) {
                        Text("✦")
                        Text(isZh ? "洗牌开始" : "Shuffle & Draw")
                    }
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.backgroundDeep)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(AppColors.accentGold)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .padding(.top, 18)

                if let err = vm.errorMessage {
                    Text(err)
                        .font(AppFonts.caption)
                        .foregroundStyle(AppColors.error.opacity(0.85))
                        .multilineTextAlignment(.center)
                        .padding(.top, 12)
                        .padding(.horizontal, 20)
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)
            .padding(.horizontal, 20)
            .padding(.bottom, 28)
        }
    }
}

// MARK: - Shuffle Animation

private struct TarotShuffleAnimView: View {
    let onComplete: () -> Void
    @State private var animState: Int = 0
    @State private var glowPulse = false
    @State private var dotCount: Int = 0
    private var isZh: Bool { AppLanguageOption.isChinese }

    // 5 cards × 3 animation states: (xOffset, yOffset, rotationDegrees)
    private let cardStates: [[(x: CGFloat, y: CGFloat, rot: Double)]] = [
        [(-100, 8, -13), (-50, 4, -6), (0, 0, 0), (50, 4, 6), (100, 8, 13)],
        [(28, -65, 18), (-82, 12, -22), (55, 28, 8), (-22, -52, -11), (72, 6, 20)],
        [(-5, -7, -2), (-2, -4, -1), (0, 0, 0), (2, -4, 1), (5, -7, 2)]
    ]

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            Text("✦")
                .font(.system(size: 52))
                .foregroundStyle(AppColors.accentGold)
                .scaleEffect(glowPulse ? 1.14 : 1.0)
                .animation(.easeInOut(duration: 1.1).repeatForever(autoreverses: true), value: glowPulse)
                .padding(.bottom, 32)

            // Animated card fan
            ZStack {
                ForEach(0..<5, id: \.self) { i in
                    let st = cardStates[min(animState, 2)][i]
                    CardBackView(size: .medium)
                        .offset(x: st.x, y: st.y)
                        .rotationEffect(.degrees(st.rot))
                        .animation(
                            .spring(duration: 0.52, bounce: 0.22).delay(Double(i) * 0.04),
                            value: animState
                        )
                        .zIndex(Double(i))
                }
            }
            .frame(width: 320, height: 210)

            VStack(spacing: 8) {
                Text((isZh ? "洗牌中" : "Shuffling") + String(repeating: "·", count: dotCount + 1))
                    .font(AppFonts.bodyMedium)
                    .foregroundStyle(AppColors.textSecondary)
                    .animation(nil, value: dotCount)
                Text(isZh ? "静心专注，聆听内在的问题" : "Clear your mind and focus on your question")
                    .font(AppFonts.caption)
                    .foregroundStyle(AppColors.textMuted)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
            .padding(.top, 32)

            Spacer()

            Button(isZh ? "跳过" : "Skip") { onComplete() }
                .font(AppFonts.caption)
                .foregroundStyle(AppColors.textMuted)
                .padding(.bottom, 44)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .task {
            glowPulse = true
            try? await Task.sleep(for: .milliseconds(750))
            withAnimation { animState = 1 }
            try? await Task.sleep(for: .milliseconds(750))
            withAnimation { animState = 2 }
            try? await Task.sleep(for: .milliseconds(700))
            onComplete()
        }
        .task {
            for _ in 0..<20 {
                try? await Task.sleep(for: .milliseconds(400))
                dotCount = (dotCount + 1) % 3
            }
        }
    }
}

// MARK: - Pick Deck

private struct TarotPickDeckView: View {
    let vm: TarotViewModel
    let deck: [TarotCard]
    private var isZh: Bool { AppLanguageOption.isChinese }

    var body: some View {
        VStack(spacing: 0) {
            // Header
            VStack(spacing: 4) {
                Text(
                    isZh
                        ? "从牌海中，选出\(vm.requiredPickCount == 1 ? "一张" : "\(vm.requiredPickCount)张")"
                        : "Choose \(vm.requiredPickCount) card\(vm.requiredPickCount == 1 ? "" : "s") from the spread"
                )
                    .font(AppFonts.bodyMedium)
                    .foregroundStyle(AppColors.textSecondary)
                    .multilineTextAlignment(.center)
                Text(isZh
                     ? "已选 \(vm.pickedIndices.count) / \(vm.requiredPickCount)"
                     : "\(vm.pickedIndices.count) of \(vm.requiredPickCount) selected")
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.accentGold)
                    .contentTransition(.numericText())
                    .animation(.easeInOut(duration: 0.2), value: vm.pickedIndices.count)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 12)

            // Card grid
            GeometryReader { geo in
                let cols = 6
                let hPad: CGFloat = 14
                let gap: CGFloat = 6
                let cardW = (geo.size.width - hPad * 2 - gap * CGFloat(cols - 1)) / CGFloat(cols)

                ScrollView(showsIndicators: false) {
                    LazyVGrid(
                        columns: Array(repeating: GridItem(.flexible(), spacing: gap), count: cols),
                        spacing: gap
                    ) {
                        ForEach(Array(deck.enumerated()), id: \.offset) { idx, _ in
                            let isPicked = vm.pickedIndices.contains(idx)
                            let pickOrder = vm.pickedIndices.firstIndex(of: idx).map { $0 + 1 }

                            PickCardCell(
                                width: cardW,
                                tilt: cardTilt(idx),
                                isPicked: isPicked,
                                pickOrder: pickOrder
                            )
                            .onTapGesture {
                                withAnimation(.spring(duration: 0.22)) {
                                    vm.togglePick(at: idx)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, hPad)
                    .padding(.bottom, 12)
                }
            }

            // Reveal button (slides in once the full spread is selected)
            if vm.pickedIndices.count == vm.requiredPickCount {
                Button {
                    vm.reveal()
                } label: {
                    HStack(spacing: 8) {
                        if vm.isGenerating {
                            ProgressView().tint(AppColors.backgroundDeep)
                        }
                        Text(
                            isZh
                                ? (vm.requiredPickCount == 1 ? "揭示这一张牌" : "翻开这组牌")
                                : (vm.requiredPickCount == 1 ? "Reveal Card" : "Reveal Spread")
                        )
                            .font(AppFonts.titleMedium)
                            .foregroundStyle(AppColors.backgroundDeep)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(AppColors.accentGold)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)
                .padding(.bottom, 14)
                .disabled(vm.isGenerating)
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
    }

    private func cardTilt(_ idx: Int) -> Double {
        let tilts: [Double] = [-4, 3, -2, 5, -3, 4, -5, 2, -6, 3, -1, 5, -3, 4, -2]
        return tilts[idx % tilts.count]
    }
}

private struct PickCardCell: View {
    let width: CGFloat
    let tilt: Double
    let isPicked: Bool
    let pickOrder: Int?

    var height: CGFloat { width * 1.62 }
    var radius: CGFloat { width * 0.12 }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: radius)
                .fill(LinearGradient(
                    colors: [AppColors.backgroundElevated, Color(hex: "#2A1F50")],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                ))

            RoundedRectangle(cornerRadius: radius)
                .strokeBorder(
                    isPicked ? AppColors.accentGold : AppColors.accentGold.opacity(0.28),
                    lineWidth: isPicked ? 2 : 1
                )

            Text("✦")
                .font(.system(size: width * 0.28))
                .foregroundStyle(
                    isPicked
                        ? AppColors.accentGold : AppColors.accentGold.opacity(0.22)
                )
        }
        .frame(width: width, height: height)
        .rotationEffect(.degrees(tilt))
        .scaleEffect(isPicked ? 1.06 : 1.0)
        .animation(.spring(duration: 0.22), value: isPicked)
        .overlay(alignment: .topTrailing) {
            if let order = pickOrder {
                Text("\(order)")
                    .font(.system(size: max(9, width * 0.2), weight: .bold))
                    .foregroundStyle(AppColors.backgroundDeep)
                    .frame(width: max(14, width * 0.28), height: max(14, width * 0.28))
                    .background(AppColors.accentGold)
                    .clipShape(Circle())
                    .offset(x: -2, y: 2)
                    .transition(.scale.combined(with: .opacity))
            }
        }
    }
}
