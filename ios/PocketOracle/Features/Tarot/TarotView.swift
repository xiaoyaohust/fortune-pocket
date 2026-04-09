import SwiftUI
import SwiftData

struct TarotView: View {
    @State private var vm = TarotViewModel()
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    private var isResult: Bool {
        if case .result = vm.phase { return true }
        return false
    }

    var body: some View {
        GeometryReader { proxy in
            VStack(spacing: 0) {
                TarotTopBar(title: String.appLocalized("home_tarot_title")) {
                    dismiss()
                }
                .padding(.top, 2)
                .padding(.horizontal, 20)
                .padding(.bottom, 10)

                Group {
                    switch vm.phase {
                    case .idle:
                        DrawPromptView(vm: vm)
                            .transition(.opacity)
                    case .result(let reading):
                        TarotResultView(reading: reading) {
                            vm.saveToHistory(context: modelContext)
                            withAnimation { vm.reset() }
                        }
                        .transition(.opacity)
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            .background(AppColors.gradientBackground.ignoresSafeArea())
        }
        .animation(.easeInOut(duration: 0.45), value: isResult)
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
        .fortuneTabBarHidden()
    }
}

// MARK: - Draw Prompt Screen

private struct DrawPromptView: View {
    let vm: TarotViewModel

    private var isZh: Bool {
        AppLanguageOption.isChinese
    }

    var body: some View {
        GeometryReader { proxy in
            let compactHeight = proxy.size.height < 720
            let cardSize: CardSize = .small
            let sectionSpacing = compactHeight ? 14.0 : 18.0
            let bottomPadding = 28.0

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    VStack(spacing: 8) {
                        Text(vm.selectedTheme.localizedSpreadName)
                            .font(AppFonts.headlineMedium)
                            .foregroundStyle(AppColors.textPrimary)
                            .multilineTextAlignment(.center)
                        Text(vm.selectedTheme.localizedSpreadSubtitle)
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

                    LazyVGrid(
                        columns: [GridItem(.flexible()), GridItem(.flexible())],
                        spacing: 10
                    ) {
                        ForEach(TarotQuestionTheme.allCases) { theme in
                            Button {
                                vm.setTheme(theme)
                            } label: {
                                Text(theme.localizedName)
                                    .font(AppFonts.caption)
                                    .foregroundStyle(
                                        vm.selectedTheme == theme
                                            ? AppColors.backgroundDeep
                                            : AppColors.textPrimary
                                    )
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 46)
                                    .background(
                                        vm.selectedTheme == theme
                                            ? AppColors.accentGold
                                            : AppColors.backgroundElevated
                                    )
                                    .clipShape(RoundedRectangle(cornerRadius: 14))
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 14)
                                            .stroke(
                                                vm.selectedTheme == theme
                                                    ? AppColors.accentGold
                                                    : AppColors.accentGold.opacity(0.15),
                                                lineWidth: 1
                                            )
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.top, sectionSpacing)

                    HStack(spacing: 12) {
                        ForEach(0..<3, id: \.self) { _ in
                            TarotCardView(isFaceDown: true, size: cardSize)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, sectionSpacing)

                    Text(
                        isZh
                            ? "先确认你想询问的主题，再静心抽出三张牌。\n结果会围绕牌阵位置与主题本身来展开。"
                            : "Choose your focus first, then draw three cards.\nThe reading will follow the spread positions and your selected theme."
                    )
                    .font(AppFonts.bodyMedium)
                    .foregroundStyle(AppColors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 20)
                    .padding(.top, sectionSpacing)

                    Text(String.appLocalized("tarot_orientation_prompt"))
                        .font(AppFonts.caption)
                        .foregroundStyle(AppColors.accentGold.opacity(0.88))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 20)
                        .padding(.top, 12)

                    Group {
                        if vm.isGenerating {
                            ProgressView()
                                .tint(AppColors.accentGold)
                                .scaleEffect(1.25)
                                .frame(maxWidth: .infinity)
                                .frame(height: 56)
                        } else {
                            Button { vm.draw() } label: {
                                Text(String.appLocalized("tarot_draw_button"))
                                    .font(AppFonts.titleMedium)
                                    .foregroundStyle(AppColors.backgroundDeep)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 56)
                                    .background(AppColors.accentGold)
                                    .clipShape(RoundedRectangle(cornerRadius: 16))
                            }
                        }
                    }
                    .padding(.top, sectionSpacing)

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
                .padding(.bottom, bottomPadding)
            }
        }
    }
}

private struct TarotTopBar: View {
    let title: String
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Button(action: onBack) {
                Image(systemName: "chevron.left")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(AppColors.accentGold)
                    .frame(width: 40, height: 40)
                    .background(AppColors.backgroundElevated.opacity(0.92))
                    .clipShape(Circle())
                    .overlay(
                        Circle()
                            .stroke(AppColors.accentGold.opacity(0.25), lineWidth: 1)
                    )
            }
            .buttonStyle(.plain)

            Text(title)
                .font(AppFonts.headlineMedium)
                .foregroundStyle(AppColors.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)

            Color.clear
                .frame(width: 40, height: 40)
        }
    }
}
