import SwiftUI
import SwiftData

struct AstrologyView: View {

    @State private var viewModel = AstrologyViewModel()
    @State private var showingCityPicker = false
    @State private var citySearchText = ""
    @State private var showRevealAnim = false

    @Environment(\.modelContext) private var modelContext

    private var isZh: Bool { AppLanguageOption.isChinese }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 20) {
                introCard
                inputCard

                if let reading = viewModel.reading {
                    AstrologyResultView(
                        reading: reading,
                        hasSaved: viewModel.hasSavedCurrentReading,
                        onSave: { viewModel.saveToHistory(context: modelContext) }
                    )
                }

                if let errorMessage = viewModel.errorMessage {
                    AstrologyErrorPanel(message: errorMessage, isZh: isZh)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 32)
        }
        .fortuneBackground()
        .overlay {
            if showRevealAnim {
                MysticalCalcOverlay(
                    label: isZh ? "观星中..." : "Reading the stars...",
                    symbols: ["♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓"]
                )
                .transition(AnyTransition.opacity)
                .task {
                    try? await Task.sleep(for: .milliseconds(1900))
                    withAnimation(.easeOut(duration: 0.3)) { showRevealAnim = false }
                }
            }
        }
        .animation(.easeInOut(duration: 0.25), value: showRevealAnim)
        .navigationTitle(String.appLocalized("home_astrology_title"))
        .navigationBarTitleDisplayMode(.inline)
        .fortuneNavigationBackButton()
        .fortuneTabBarHidden()
        .sheet(isPresented: $showingCityPicker) {
            AstrologyCityPickerSheet(
                searchText: $citySearchText,
                cities: viewModel.cities,
                selectedCity: viewModel.selectedCity,
                isZh: isZh
            ) { city in
                viewModel.setCity(city)
            }
        }
    }

    private var introCard: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("✦")
                .font(AppFonts.titleLarge)
                .foregroundStyle(AppColors.accentGold)

            Text(isZh ? "用出生时间与地点，生成一张真正离线的本命盘" : "Use birth time and place to reveal a fully offline natal chart")
                .font(AppFonts.headlineMedium)
                .foregroundStyle(AppColors.textPrimary)

            Text(
                isZh
                    ? "这不是简单的今日运势模板，而是按出生日期、出生时间、IANA 时区与地点经纬度计算太阳、月亮、上升、主要行星、宫位与相位，再生成解读。"
                    : "This is no longer a lightweight daily horoscope template. The reading is built from your birth date, birth time, IANA time zone, location coordinates, planetary placements, houses, and major aspects."
            )
            .font(AppFonts.bodyMedium)
            .foregroundStyle(AppColors.textSecondary)
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
    }

    private var inputCard: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(isZh ? "出生资料" : "Birth Details")
                .font(AppFonts.headlineMedium)
                .foregroundStyle(AppColors.textPrimary)

            VStack(alignment: .leading, spacing: 10) {
                Text(isZh ? "出生日期" : "Birth Date")
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.textPrimary)
                DatePicker(
                    "",
                    selection: Binding(
                        get: { viewModel.selectedBirthDate },
                        set: { viewModel.updateBirthDate($0) }
                    ),
                    displayedComponents: .date
                )
                .datePickerStyle(.compact)
                .labelsHidden()
            }

            VStack(alignment: .leading, spacing: 10) {
                Text(isZh ? "出生时间" : "Birth Time")
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.textPrimary)
                DatePicker(
                    "",
                    selection: Binding(
                        get: { viewModel.timePickerDate },
                        set: { viewModel.timePickerDate = $0 }
                    ),
                    displayedComponents: .hourAndMinute
                )
                .datePickerStyle(.compact)
                .labelsHidden()
            }

            VStack(alignment: .leading, spacing: 10) {
                Text(isZh ? "出生城市" : "Birth City")
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.textPrimary)

                Button {
                    showingCityPicker = true
                } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(viewModel.selectedCity.map { isZh ? $0.nameZh : $0.nameEn } ?? (isZh ? "选择城市" : "Choose a city"))
                                .font(AppFonts.bodyMedium)
                                .foregroundStyle(AppColors.textPrimary)
                            Text(viewModel.selectedCity?.timeZoneId ?? (isZh ? "需要城市来计算上升与宫位" : "A city is required for rising sign and houses"))
                                .font(AppFonts.bodySmall)
                                .foregroundStyle(AppColors.textSecondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right")
                            .foregroundStyle(AppColors.textMuted)
                    }
                    .padding(.horizontal, 14)
                    .padding(.vertical, 14)
                    .background(AppColors.backgroundBase)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .buttonStyle(.plain)
            }

            if let sign = viewModel.inferredSunSign {
                AstroBadgeRow(
                    badges: [
                        (isZh ? "太阳星座预览" : "Sun Sign Preview", sign.symbol + " " + sign.localizedName)
                    ]
                )
            }

            if let city = viewModel.selectedCity {
                AstrologyCityMetadataPanel(city: city, isZh: isZh)
            } else {
                AstrologyInfoPanel(
                    icon: "mappin.and.ellipse",
                    title: isZh ? "需要出生城市" : "Birth city required",
                    message: isZh
                        ? "上升点、宫位和很多角度关系都依赖出生地与法定时区。请选择城市后再生成。"
                        : "The rising sign, houses, and many angular relationships depend on birthplace and legal time zone. Choose a city before generating."
                )
            }

            Button {
                showRevealAnim = true
                viewModel.generate()
            } label: {
                HStack(spacing: 10) {
                    if viewModel.isGenerating {
                        ProgressView()
                            .tint(AppColors.backgroundDeep)
                    }
                    Text(isZh ? "生成本命盘解读" : "Generate Natal Chart Reading")
                        .font(AppFonts.titleMedium)
                }
                .foregroundStyle(AppColors.backgroundDeep)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(AppColors.accentGold)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .disabled(!viewModel.canGenerate)
            .opacity(viewModel.canGenerate ? 1 : 0.6)
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
    }
}

private struct AstrologyResultView: View {
    let reading: HoroscopeReading
    let hasSaved: Bool
    let onSave: () -> Void

    private let shareText: String
    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12)
    ]

    init(reading: HoroscopeReading, hasSaved: Bool, onSave: @escaping () -> Void) {
        self.reading = reading
        self.hasSaved = hasSaved
        self.onSave = onSave
        self.shareText = ReadingPresentationBuilder.shareText(for: reading)
    }

    private var isZh: Bool { AppLanguageOption.isChinese }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            VStack(alignment: .leading, spacing: 8) {
                Text(reading.chartSignature)
                    .font(AppFonts.headlineLarge)
                    .foregroundStyle(AppColors.textPrimary)

                Text("\(reading.birthDateText) · \(reading.birthTimeText) · \(reading.birthCityName)")
                    .font(AppFonts.bodySmall)
                    .foregroundStyle(AppColors.textSecondary)

                Text(reading.timeZoneId)
                    .font(AppFonts.caption)
                    .foregroundStyle(AppColors.accentGold)

                Text(reading.chartSummary)
                    .font(AppFonts.bodyMedium)
                    .foregroundStyle(AppColors.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            AstroBadgeRow(
                badges: [
                    (isZh ? "太阳" : "Sun", reading.sign.symbol + " " + reading.sign.localizedName),
                    (isZh ? "月亮" : "Moon", reading.moonSign.symbol + " " + reading.moonSign.localizedName),
                    (isZh ? "上升" : "Rising", (reading.risingSign?.symbol ?? "↑") + " " + (reading.risingSign?.localizedName ?? ""))
                ]
            )

            if !reading.planetPlacements.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text(isZh ? "行星落座与宫位" : "Planet Placements")
                        .font(AppFonts.titleMedium)
                        .foregroundStyle(AppColors.textPrimary)

                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(reading.planetPlacements) { item in
                            PlanetPlacementCard(placement: item, isZh: isZh)
                        }
                    }
                }
            }

            if !reading.majorAspects.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    Text(isZh ? "主要相位" : "Major Aspects")
                        .font(AppFonts.titleMedium)
                        .foregroundStyle(AppColors.textPrimary)

                    ForEach(reading.majorAspects) { item in
                        AstrologyInfoPanel(
                            icon: "sparkles",
                            title: aspectTitle(item, isZh: isZh),
                            message: aspectMessage(item, isZh: isZh)
                        )
                    }
                }
            }

            if !reading.houseFocus.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    Text(isZh ? "宫位焦点" : "House Focus")
                        .font(AppFonts.titleMedium)
                        .foregroundStyle(AppColors.textPrimary)
                    ForEach(reading.houseFocus) { item in
                        AstrologyInfoPanel(
                            icon: "scope",
                            title: item.localizedTitle,
                            message: item.localizedSummary
                        )
                    }
                }
            }

            AstrologySectionCard(title: isZh ? "整体画像" : "Overall Profile", content: reading.overall)
            AstrologySectionCard(title: isZh ? "感情与亲密关系" : "Love & Intimacy", content: reading.love)
            AstrologySectionCard(title: isZh ? "事业与成长方向" : "Career & Direction", content: reading.career)
            AstrologySectionCard(title: isZh ? "财富与安全感" : "Wealth & Security", content: reading.wealth)
            AstrologySectionCard(title: isZh ? "社交与沟通" : "Social & Communication", content: reading.social)
            AstrologySectionCard(title: isZh ? "给你的建议" : "Guidance", content: reading.advice)

            AstroBadgeRow(
                badges: [
                    (isZh ? "主导元素" : "Dominant Element", reading.elementBalance.localizedDominantElement),
                    (isZh ? "幸运色" : "Lucky Color", reading.luckyColor),
                    (isZh ? "幸运数字" : "Lucky Number", "\(reading.luckyNumber)")
                ]
            )

            HStack(spacing: 12) {
                ShareLink(item: shareText) {
                    Label(String.appLocalized("share"), systemImage: "square.and.arrow.up")
                        .font(AppFonts.titleMedium)
                        .foregroundStyle(AppColors.textPrimary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(AppColors.backgroundBase)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16)
                                .stroke(AppColors.accentGold.opacity(0.35), lineWidth: 1)
                        )
                }

                Button(action: onSave) {
                    Text(hasSaved
                        ? (isZh ? "已保存到历史" : "Saved to History")
                        : (isZh ? "保存到历史" : "Save to History"))
                        .font(AppFonts.titleMedium)
                        .foregroundStyle(AppColors.backgroundDeep)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(AppColors.accentGold)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                .disabled(hasSaved)
                .opacity(hasSaved ? 0.7 : 1)
            }
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
    }

    private func aspectTitle(_ item: AstrologyAspect, isZh: Bool) -> String {
        "\(item.firstPlanetId.localizedName) · \(item.type.localizedName) · \(item.secondPlanetId.localizedName)"
    }

    private func aspectMessage(_ item: AstrologyAspect, isZh: Bool) -> String {
        let orbText = String(format: "%.1f°", item.orbDegrees)
        return isZh
            ? "容许度 \(orbText)。这个角度会放大这两颗行星之间的互动感。"
            : "Orb \(orbText). This angle amplifies the dialogue between the two planets."
    }
}

private struct AstrologySectionCard: View {
    let title: String
    let content: String

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(AppFonts.titleMedium)
                .foregroundStyle(AppColors.textPrimary)
            Text(content)
                .font(AppFonts.bodyMedium)
                .foregroundStyle(AppColors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(16)
        .background(AppColors.backgroundBase)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private struct AstrologyInfoPanel: View {
    let icon: String
    let title: String
    let message: String

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(AppColors.accentGold)
                .frame(width: 18, height: 18)
                .padding(.top, 1)

            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(AppFonts.bodyMedium)
                    .foregroundStyle(AppColors.textPrimary)
                Text(message)
                    .font(AppFonts.bodySmall)
                    .foregroundStyle(AppColors.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(14)
        .background(AppColors.backgroundBase)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

private struct AstrologyErrorPanel: View {
    let message: String
    let isZh: Bool

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(AppColors.error)
                .frame(width: 18, height: 18)
                .padding(.top, 2)

            VStack(alignment: .leading, spacing: 5) {
                Text(isZh ? "出了点小问题" : "Something went wrong")
                    .font(AppFonts.bodyMedium)
                    .foregroundStyle(AppColors.textPrimary)
                Text(message)
                    .font(AppFonts.bodySmall)
                    .foregroundStyle(AppColors.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(14)
        .background(AppColors.backgroundElevated)
        .overlay(
            RoundedRectangle(cornerRadius: 14)
                .stroke(AppColors.error.opacity(0.28), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

private struct AstroBadgeRow: View {
    let badges: [(String, String)]

    var body: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
            ForEach(Array(badges.enumerated()), id: \.offset) { _, badge in
                VStack(spacing: 4) {
                    Text(badge.0)
                        .font(AppFonts.caption)
                        .foregroundStyle(AppColors.textSecondary)
                    Text(badge.1)
                        .font(AppFonts.bodyMedium)
                        .foregroundStyle(AppColors.textPrimary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .padding(.horizontal, 10)
                .background(AppColors.backgroundBase)
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
        }
    }
}

private struct PlanetPlacementCard: View {
    let placement: AstrologyPlanetPlacement
    let isZh: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(placement.localizedPlanetName)
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.textPrimary)
                Spacer()
                if placement.isRetrograde {
                    Text("Rx")
                        .font(AppFonts.caption)
                        .foregroundStyle(AppColors.accentGold)
                }
            }

            Text(placement.localizedSignName)
                .font(AppFonts.bodyMedium)
                .foregroundStyle(AppColors.textSecondary)

            Text(String(format: isZh ? "%.1f° · 第%@宫" : "%.1f° · House %@", placement.degreeInSign, placement.house.map(String.init) ?? "-"))
                .font(AppFonts.bodySmall)
                .foregroundStyle(AppColors.textMuted)
        }
        .padding(14)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.backgroundBase)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

private struct AstrologyCityMetadataPanel: View {
    let city: BirthCity
    let isZh: Bool

    private var offsetLabel: String {
        let hours = city.utcOffsetHours
        let sign = hours >= 0 ? "+" : "-"
        let absHours = abs(hours)
        let whole = Int(absHours.rounded(.down))
        let minutes = Int(((absHours - Double(whole)) * 60.0).rounded())
        return String(format: "UTC%@%02d:%02d", sign, whole, minutes)
    }

    var body: some View {
        AstrologyInfoPanel(
            icon: "globe.asia.australia",
            title: isZh ? "\(city.nameZh) · \(city.country)" : "\(city.nameEn) · \(city.country)",
            message: isZh
                ? "IANA 时区：\(city.timeZoneId)\n法定时区偏移：\(offsetLabel)。系统会按这个时区的历史 DST 规则恢复民用出生时。"
                : "IANA time zone: \(city.timeZoneId)\nStandard legal offset: \(offsetLabel). The engine restores civil birth time through this zone's historical DST rules."
        )
    }
}

private struct AstrologyCityPickerSheet: View {
    @Binding var searchText: String

    let cities: [BirthCity]
    let selectedCity: BirthCity?
    let isZh: Bool
    let onSelect: (BirthCity) -> Void

    @Environment(\.dismiss) private var dismiss

    private static let popularCityIDs = [
        "beijing", "shanghai", "guangzhou", "shenzhen", "chengdu",
        "hongkong", "taipei", "singapore", "tokyo",
        "london", "new_york", "los_angeles", "seattle", "sydney"
    ]

    private var normalizedQuery: String {
        searchText.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var popularCities: [BirthCity] {
        Self.popularCityIDs.compactMap { id in
            cities.first(where: { $0.id == id })
        }
    }

    private var searchResults: [BirthCity] {
        guard !normalizedQuery.isEmpty else { return [] }
        return cities
            .filter { city in
                city.id.localizedCaseInsensitiveContains(normalizedQuery)
                || city.nameZh.localizedCaseInsensitiveContains(normalizedQuery)
                || city.nameEn.localizedCaseInsensitiveContains(normalizedQuery)
                || city.country.localizedCaseInsensitiveContains(normalizedQuery)
                || city.timeZoneId.localizedCaseInsensitiveContains(normalizedQuery)
                || city.searchAliases.contains(where: { $0.localizedCaseInsensitiveContains(normalizedQuery) })
            }
            .sorted { lhs, rhs in
                let lhsPopular = Self.popularCityIDs.contains(lhs.id)
                let rhsPopular = Self.popularCityIDs.contains(rhs.id)
                if lhsPopular != rhsPopular { return lhsPopular }
                return isZh ? lhs.nameZh < rhs.nameZh : lhs.nameEn < rhs.nameEn
            }
    }

    var body: some View {
        NavigationStack {
            List {
                if normalizedQuery.isEmpty {
                    if !popularCities.isEmpty {
                        Section(isZh ? "常用城市" : "Popular Cities") {
                            ForEach(popularCities) { city in
                                cityRow(city)
                            }
                        }
                    }
                    Section(isZh ? "搜索提示" : "Search Hint") {
                        Text(
                            isZh
                                ? "目前已收录 \(cities.count) 个城市与区域中心，直接搜索城市名、英文名、国家代码或时区会更快。"
                                : "\(cities.count) cities and regional hubs are currently included. Searching by city name, English name, country code, or time zone is the fastest path."
                        )
                        .font(AppFonts.bodySmall)
                        .foregroundStyle(AppColors.textSecondary)
                        .listRowBackground(AppColors.backgroundElevated)
                    }
                } else {
                    Section((isZh ? "搜索结果" : "Search Results") + " (\(searchResults.count))") {
                        if searchResults.isEmpty {
                            Text(
                                isZh
                                    ? "没有匹配的城市，请尝试中文名、英文名、国家代码或时区，例如“邵阳”或“Seattle”。"
                                    : "No matching city. Try a Chinese name, English name, country code, or time zone, for example “Shaoyang” or “Seattle”."
                            )
                            .font(AppFonts.bodySmall)
                            .foregroundStyle(AppColors.textSecondary)
                            .listRowBackground(AppColors.backgroundElevated)
                        } else {
                            ForEach(searchResults) { city in
                                cityRow(city)
                            }
                        }
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .background(AppColors.backgroundDeep)
            .searchable(
                text: $searchText,
                prompt: isZh
                    ? "搜索城市、英文名、国家或时区，如邵阳 / Seattle"
                    : "Search city, English name, country, or time zone, e.g. Shaoyang / Seattle"
            )
            .navigationTitle(isZh ? "选择出生城市" : "Choose Birth City")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(isZh ? "关闭" : "Close") {
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }

    @ViewBuilder
    private func cityRow(_ city: BirthCity) -> some View {
        Button {
            onSelect(city)
            dismiss()
        } label: {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(isZh ? city.nameZh : city.nameEn)
                        .font(AppFonts.bodyMedium)
                        .foregroundStyle(AppColors.textPrimary)
                    Text("\(city.timeZoneId) · \(city.country)")
                        .font(AppFonts.bodySmall)
                        .foregroundStyle(AppColors.textSecondary)
                }
                Spacer()
                if selectedCity?.id == city.id {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(AppColors.accentGold)
                }
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
        .listRowBackground(AppColors.backgroundElevated)
    }
}
