import SwiftUI
import SwiftData

struct BaziView: View {

    @State private var vm = BaziViewModel()
    @State private var showCityPicker = false
    @State private var citySearchText = ""
    @Environment(\.modelContext) private var modelContext

    private var isZh: Bool { AppLanguageOption.isChinese }

    private var precisionTags: [String] {
        var tags: [String] = []
        if vm.includesBirthHour {
            tags.append(isZh ? "分钟级时刻" : "Minute Precision")
            if vm.selectedCity == nil {
                tags.append(isZh ? "北京时间估算" : "Asia/Shanghai Fallback")
            } else {
                tags.append("IANA / DST")
            }
            tags.append(
                vm.useTrueSolarTime && vm.selectedCity != nil
                    ? (isZh ? "真太阳时" : "True Solar Time")
                    : (isZh ? "民用时" : "Civil Time")
            )
            if vm.distinguishLateZi {
                tags.append(isZh ? "晚子时规则" : "Late Zi Rule")
            }
        } else {
            tags.append(isZh ? "日期级估算" : "Date-only Estimate")
            tags.append(isZh ? "时柱留空" : "Hour Pillar Omitted")
        }
        return tags
    }

    private var precisionTitle: String {
        if !vm.includesBirthHour {
            return isZh ? "当前按日期级别排盘" : "Currently using a date-only chart"
        }
        if vm.selectedCity == nil {
            return isZh ? "当前按法定民用时估算" : "Currently estimating with standard legal time"
        }
        if vm.useTrueSolarTime {
            return isZh ? "当前按真太阳时排盘" : "Currently using true solar time"
        }
        return isZh ? "当前按城市民用时排盘" : "Currently using city civil time"
    }

    private var precisionDescription: String {
        if !vm.includesBirthHour {
            return isZh
                ? "未填写出生时刻时，系统只计算年、月、日三柱，时柱保持未知，更适合做日级别参考。"
                : "Without a birth time, the engine calculates only year, month, and day pillars. The hour pillar remains unknown, so treat the result as day-level guidance."
        }
        if vm.selectedCity == nil {
            return isZh
                ? "未选城市时，系统会以北京时间 `Asia/Shanghai / UTC+8` 作为法定时兜底，不应用 DST，也不能开启真太阳时。国际出生或节气边界案例建议补充城市。"
                : "When no city is selected, the engine falls back to Beijing legal time `Asia/Shanghai / UTC+8`. DST is not applied and true solar time stays unavailable. Add a city for international births or boundary cases."
        }
        if vm.useTrueSolarTime {
            return isZh
                ? "当前会先按 IANA 时区处理法定时与 DST，再叠加经度修正与均时差，得到更接近地方真太阳时的有效时刻。"
                : "The engine first resolves legal time and DST from the IANA time zone, then adds longitude correction and equation of time to derive local apparent solar time."
        }
        return isZh
            ? "当前使用所选城市的 IANA 时区与 DST 规则，还原出生地民用时。若你希望按地方太阳时排盘，可开启真太阳时。"
            : "The engine is using the selected city's IANA time zone and DST rules to recover local civil time. Enable true solar time if you want a solar-time-based chart."
    }

    private var selectedCityLabel: String {
        guard let city = vm.selectedCity else {
            return isZh ? "未选择城市（默认按北京时间估算）" : "No city selected (defaults to Beijing legal time)"
        }
        return isZh ? city.nameZh : city.nameEn
    }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 20) {
                introCard
                inputCard
                if let chart = vm.chart {
                    BaziChartView(
                        chart:    chart,
                        hasSaved: vm.hasSavedCurrentChart,
                        onSave:   { vm.saveToHistory(context: modelContext) }
                    )
                }
                if let err = vm.errorMessage {
                    Text(err)
                        .font(AppFonts.caption)
                        .foregroundStyle(AppColors.error)
                        .padding(.horizontal, 4)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 40)
        }
        .fortuneBackground()
        .navigationTitle(isZh ? "八字排盘" : "Four Pillars")
        .navigationBarTitleDisplayMode(.inline)
        .fortuneNavigationBackButton()
        .fortuneTabBarHidden()
        .sheet(isPresented: $showCityPicker) {
            BaziCityPickerSheet(
                searchText: $citySearchText,
                cities: vm.cities,
                selectedCity: vm.selectedCity,
                isZh: isZh,
                onSelect: { city in
                    vm.setCity(city)
                    showCityPicker = false
                }
            )
        }
    }

    // MARK: - Intro card

    private var introCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("☯︎").font(AppFonts.titleLarge).foregroundStyle(AppColors.accentGold)
            Text(isZh ? "四柱八字" : "Four Pillars (Bazi)")
                .font(AppFonts.headlineMedium).foregroundStyle(AppColors.textPrimary)
            Text(isZh
                 ? "依节气推算四柱，含大运、十神、藏干、五行力量。仅供参考。"
                 : "Solar-term-based four-pillar chart with major cycles, ten gods, hidden stems, and five element balance. For reference only.")
                .font(AppFonts.bodyMedium).foregroundStyle(AppColors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
    }

    // MARK: - Input card

    private var inputCard: some View {
        VStack(alignment: .leading, spacing: 18) {

            // Birth date
            VStack(alignment: .leading, spacing: 8) {
                Label(isZh ? "出生日期" : "Birth Date", systemImage: "calendar")
                    .font(AppFonts.titleMedium).foregroundStyle(AppColors.textPrimary)
                DatePicker(
                    "", selection: Binding(get: { vm.selectedBirthDate }, set: { vm.updateBirthDate($0) }),
                    displayedComponents: .date
                )
                .datePickerStyle(.compact).labelsHidden()
            }

            GoldDivider()

            precisionPanel

            GoldDivider()

            // Gender
            VStack(alignment: .leading, spacing: 8) {
                Label(isZh ? "性别" : "Gender", systemImage: "person")
                    .font(AppFonts.titleMedium).foregroundStyle(AppColors.textPrimary)
                Picker("", selection: Binding(get: { vm.selectedGender }, set: { vm.setGender($0) })) {
                    Text(isZh ? "男" : "Male").tag(Gender.male)
                    Text(isZh ? "女" : "Female").tag(Gender.female)
                }
                .pickerStyle(.segmented)
            }

            GoldDivider()

            // Birth time
            Toggle(isOn: Binding(get: { vm.includesBirthHour }, set: { vm.setBirthHourEnabled($0) })) {
                Label(isZh ? "出生时间（建议填写）" : "Birth Time (recommended)", systemImage: "clock")
                    .font(AppFonts.titleMedium).foregroundStyle(AppColors.textPrimary)
            }
            .tint(AppColors.accentGold)

            if vm.includesBirthHour {
                HStack(spacing: 12) {
                    // Hour
                    VStack(alignment: .leading, spacing: 4) {
                        Text(isZh ? "时" : "Hour")
                            .font(AppFonts.caption).foregroundStyle(AppColors.textSecondary)
                        Picker("", selection: Binding(get: { vm.selectedBirthHour }, set: { vm.setBirthHour($0) })) {
                            ForEach(0..<24, id: \.self) { h in
                                Text(String(format: "%02d", h)).tag(h)
                            }
                        }
                        .pickerStyle(.wheel).frame(height: 100).clipped()
                    }
                    .frame(maxWidth: .infinity)

                    // Minute
                    VStack(alignment: .leading, spacing: 4) {
                        Text(isZh ? "分" : "Minute")
                            .font(AppFonts.caption).foregroundStyle(AppColors.textSecondary)
                        Picker("", selection: Binding(get: { vm.selectedBirthMinute }, set: { vm.setBirthMinute($0) })) {
                            ForEach(0..<60, id: \.self) { m in
                                Text(String(format: "%02d", m)).tag(m)
                            }
                        }
                        .pickerStyle(.wheel).frame(height: 100).clipped()
                    }
                    .frame(maxWidth: .infinity)
                }

                GoldDivider()

                // City
                VStack(alignment: .leading, spacing: 8) {
                    Label(isZh ? "出生城市与时区" : "Birth City & Time Zone", systemImage: "mappin")
                        .font(AppFonts.titleMedium).foregroundStyle(AppColors.textPrimary)
                    Button {
                        citySearchText = ""
                        showCityPicker = true
                    } label: {
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(selectedCityLabel)
                                    .font(AppFonts.bodyMedium)
                                    .foregroundStyle(AppColors.textPrimary)
                                Text(
                                    isZh
                                        ? "搜索城市、英文名、国家或时区"
                                        : "Search by city, English name, country, or time zone"
                                )
                                .font(AppFonts.bodySmall)
                                .foregroundStyle(AppColors.textSecondary)
                            }
                            Spacer()
                            Image(systemName: "magnifyingglass")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundStyle(AppColors.accentGold)
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 14)
                        .background(AppColors.backgroundBase)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .buttonStyle(.plain)

                    if let city = vm.selectedCity {
                        CityMetadataPanel(city: city, isZh: isZh)
                    } else {
                        BaziInfoPanel(
                            icon: "globe.asia.australia.fill",
                            title: isZh ? "当前兜底时区：Asia/Shanghai" : "Current fallback zone: Asia/Shanghai",
                            message: isZh
                                ? "如果未选城市，系统按北京时间 `UTC+8` 民用时估算。这适合中国大陆常见场景，但不适合海外出生或需处理 DST 的案例。"
                                : "Without a selected city, the engine falls back to Beijing legal time `UTC+8`. This is acceptable for common mainland-China cases, but not for overseas births or DST-sensitive charts."
                        )
                    }
                }

                GoldDivider()

                // Options
                Toggle(isOn: Binding(get: { vm.useTrueSolarTime }, set: { vm.setTrueSolarTime($0) })) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(isZh ? "真太阳时（经度 + 均时差）" : "True Solar Time (Longitude + EoT)")
                            .font(AppFonts.titleMedium).foregroundStyle(AppColors.textPrimary)
                        Text(
                            isZh
                                ? "基于城市经度与均时差修正地方太阳时，需要先选择城市"
                                : "Adjust local apparent solar time with longitude and equation of time; requires a city"
                        )
                            .font(AppFonts.caption).foregroundStyle(AppColors.textSecondary)
                    }
                }
                .tint(AppColors.accentGold)
                .disabled(vm.selectedCity == nil)

                Toggle(isOn: Binding(get: { vm.distinguishLateZi }, set: { vm.setDistinguishLateZi($0) })) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(isZh ? "晚子时归次日" : "Late Zi Hour → Next Day")
                            .font(AppFonts.titleMedium).foregroundStyle(AppColors.textPrimary)
                        Text(
                            isZh
                                ? (vm.selectedBirthHour == 23
                                    ? "当前出生时刻落在 23:00–23:59，开启后会将其并入次日"
                                    : "仅在 23:00–23:59 生效，用于区分早子时/晚子时")
                                : (vm.selectedBirthHour == 23
                                    ? "The selected birth time is within 23:00–23:59; enabling this moves it to the next day"
                                    : "Only applies to 23:00–23:59 when distinguishing early vs late Zi hour")
                        )
                            .font(AppFonts.caption).foregroundStyle(AppColors.textSecondary)
                    }
                }
                .tint(AppColors.accentGold)
            }
            else {
                BaziInfoPanel(
                    icon: "clock.badge.questionmark",
                    title: isZh ? "为什么时柱是空的？" : "Why is the hour pillar empty?",
                    message: isZh
                        ? "你当前没有填写出生时间，所以系统只按年、月、日三柱排盘，时柱会保持未知。若知道大概时分，建议补上，盘面会完整很多。"
                        : "You have not entered a birth time yet, so the engine is using only the year, month, and day pillars. Add an approximate time if you know it for a much fuller chart."
                )

                Button {
                    vm.setBirthHourEnabled(true)
                } label: {
                    Text(isZh ? "补充出生时间" : "Add birth time")
                        .font(AppFonts.bodyMedium)
                        .foregroundStyle(AppColors.accentGold)
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .background(AppColors.backgroundBase)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(AppColors.accentGold.opacity(0.24), lineWidth: 1)
                        )
                }
                .buttonStyle(.plain)
            }

            // Analyze button
            Button(action: vm.generate) {
                HStack(spacing: 10) {
                    if vm.isLoading {
                        ProgressView().tint(AppColors.backgroundDeep)
                    }
                    Text(isZh ? "排盘" : "Calculate Chart")
                        .font(AppFonts.titleMedium)
                }
                .foregroundStyle(AppColors.backgroundDeep)
                .frame(maxWidth: .infinity).frame(height: 56)
                .background(AppColors.accentGold)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .disabled(vm.isLoading)
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
    }

    private var precisionPanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(isZh ? "排盘精度与时制" : "Precision & Time Basis", systemImage: "scope")
                .font(AppFonts.titleMedium)
                .foregroundStyle(AppColors.textPrimary)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(precisionTags, id: \.self) { tag in
                        PrecisionChip(text: tag)
                    }
                }
                .padding(.vertical, 1)
            }

            BaziInfoPanel(
                icon: "clock.badge.exclamationmark",
                title: precisionTitle,
                message: precisionDescription
            )
        }
    }
}

// MARK: - BaziChartView

private struct BaziChartView: View {

    let chart:    BaziChart
    let hasSaved: Bool
    let onSave:   () -> Void

    private var isZh: Bool { AppLanguageOption.isChinese }

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            pillarsGrid
            GoldDivider()
            interpretationSection
            GoldDivider()
            tenGodsSection
            GoldDivider()
            fiveElementsSection
            GoldDivider()
            majorCyclesSection
            GoldDivider()
            actionButtons
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
    }

    // MARK: Pillars grid

    private var pillarsGrid: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(isZh ? "四柱命盘" : "Four Pillars Chart")
                .font(AppFonts.headlineMedium).foregroundStyle(AppColors.textPrimary)

            HStack(spacing: 8) {
                PillarColumn(
                    label:  isZh ? "年柱" : "Year",
                    pillar: chart.yearPillar,
                    hiddenStems: chart.hiddenStems[chart.yearPillar.branchIndex] ?? []
                )
                PillarColumn(
                    label:  isZh ? "月柱" : "Month",
                    pillar: chart.monthPillar,
                    hiddenStems: chart.hiddenStems[chart.monthPillar.branchIndex] ?? []
                )
                PillarColumn(
                    label:  isZh ? "日柱" : "Day",
                    pillar: chart.dayPillar,
                    hiddenStems: chart.hiddenStems[chart.dayPillar.branchIndex] ?? [],
                    isDayMaster: true
                )
                if let hp = chart.hourPillar {
                    PillarColumn(
                        label:  isZh ? "时柱" : "Hour",
                        pillar: hp,
                        hiddenStems: chart.hiddenStems[hp.branchIndex] ?? []
                    )
                } else {
                    unknownHourColumn
                }
            }
        }
    }

    private var unknownHourColumn: some View {
        VStack(spacing: 6) {
            Text(isZh ? "时柱" : "Hour")
                .font(AppFonts.caption).foregroundStyle(AppColors.textSecondary)
            RoundedRectangle(cornerRadius: 12)
                .fill(AppColors.backgroundBase)
                .frame(maxWidth: .infinity).frame(height: 90)
                .overlay(
                    VStack(spacing: 4) {
                        Text(isZh ? "未填" : "Missing")
                            .font(AppFonts.bodyMedium).foregroundStyle(AppColors.textSecondary)
                        Text(isZh ? "补充出生时间后可计算" : "Add birth time to calculate")
                            .font(AppFonts.caption).foregroundStyle(AppColors.textMuted)
                    }
                )
        }
        .frame(maxWidth: .infinity)
    }

    private var interpretationSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(isZh ? "命盘讲解" : "Chart Guide")
                .font(AppFonts.titleMedium)
                .foregroundStyle(AppColors.accentGold)

            ForEach(BaziInsightBuilder.sections(for: chart, isZh: isZh)) { section in
                BaziInfoPanel(
                    icon: section.icon,
                    title: section.title,
                    message: section.body
                )
            }
        }
    }

    // MARK: Ten gods

    private var tenGodsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(isZh ? "十神" : "Ten Gods")
                .font(AppFonts.titleMedium).foregroundStyle(AppColors.accentGold)

            let pillars: [(String, TenGodEntry?, TenGodEntry)] = [
                (isZh ? "年" : "Year", chart.tenGods.yearStemGod,  chart.tenGods.yearBranchGod),
                (isZh ? "月" : "Month",chart.tenGods.monthStemGod, chart.tenGods.monthBranchGod),
                (isZh ? "时" : "Hour", chart.tenGods.hourStemGod,  chart.tenGods.hourBranchGod ?? TenGodEntry(name:"—", nameEn:"—")),
            ]

            ForEach(pillars, id: \.0) { label, stemGod, branchGod in
                HStack {
                    Text(label)
                        .font(AppFonts.caption).foregroundStyle(AppColors.textSecondary)
                        .frame(width: 26, alignment: .leading)
                    Text(isZh ? "天干：\(stemGod?.name ?? "—")" : "Stem: \(stemGod?.nameEn ?? "—")")
                        .font(AppFonts.bodySmall).foregroundStyle(AppColors.textPrimary)
                    Spacer()
                    Text(isZh ? "地支：\(branchGod.name)" : "Branch: \(branchGod.nameEn)")
                        .font(AppFonts.bodySmall).foregroundStyle(AppColors.textPrimary)
                }
                .padding(.vertical, 4)
            }
        }
    }

    // MARK: Five elements

    private var fiveElementsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(isZh ? "五行力量" : "Five Element Balance")
                .font(AppFonts.titleMedium).foregroundStyle(AppColors.accentGold)

            let elements: [(String, Int, Color)] = [
                (isZh ? "木" : "Wood",  chart.fiveElements.wood,  Color(hex: "#4CAF50")),
                (isZh ? "火" : "Fire",  chart.fiveElements.fire,  Color(hex: "#F44336")),
                (isZh ? "土" : "Earth", chart.fiveElements.earth, Color(hex: "#FF9800")),
                (isZh ? "金" : "Metal", chart.fiveElements.metal, Color(hex: "#9E9E9E")),
                (isZh ? "水" : "Water", chart.fiveElements.water, Color(hex: "#2196F3")),
            ]
            let total = max(1, chart.fiveElements.total)

            ForEach(elements, id: \.0) { name, value, color in
                HStack(spacing: 10) {
                    Text(name)
                        .font(AppFonts.caption).foregroundStyle(AppColors.textSecondary)
                        .frame(width: 24)
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            RoundedRectangle(cornerRadius: 4)
                                .fill(AppColors.backgroundBase)
                            RoundedRectangle(cornerRadius: 4)
                                .fill(color.opacity(0.8))
                                .frame(width: geo.size.width * CGFloat(value) / CGFloat(total))
                        }
                    }
                    .frame(height: 12)
                    Text("\(value)")
                        .font(AppFonts.caption).foregroundStyle(AppColors.textSecondary)
                        .frame(width: 24, alignment: .trailing)
                }
            }
        }
    }

    // MARK: Major cycles

    private var majorCyclesSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(isZh ? "大运（\(chart.cycleDirection)）" : "Major Cycles (\(chart.cycleDirection))")
                    .font(AppFonts.titleMedium).foregroundStyle(AppColors.accentGold)
                Spacer()
                Text(isZh ? "起运：\(chart.startingAge)岁" : "Starts at age \(chart.startingAge)")
                    .font(AppFonts.caption).foregroundStyle(AppColors.textSecondary)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(chart.majorCycles.prefix(8), id: \.startAge) { cycle in
                        VStack(spacing: 4) {
                            Text(cycle.pillar.stemZh + cycle.pillar.branchZh)
                                .font(AppFonts.titleMedium).foregroundStyle(AppColors.textPrimary)
                            Text("\(cycle.startAge)–\(cycle.startAge + 9)")
                                .font(AppFonts.caption).foregroundStyle(AppColors.textSecondary)
                        }
                        .padding(.horizontal, 12).padding(.vertical, 10)
                        .background(AppColors.backgroundBase)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(.vertical, 2)
            }
        }
    }

    // MARK: Action buttons

    private var actionButtons: some View {
        HStack(spacing: 12) {
            Button(action: onSave) {
                Text(hasSaved
                     ? (isZh ? "已保存" : "Saved")
                     : (isZh ? "保存记录" : "Save"))
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.backgroundDeep)
                    .frame(maxWidth: .infinity).frame(height: 52)
                    .background(AppColors.accentGold)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .disabled(hasSaved)
            .opacity(hasSaved ? 0.7 : 1)
        }
    }
}

// MARK: - PillarColumn

private struct PillarColumn: View {
    let label:       String
    let pillar:      Pillar
    let hiddenStems: [HiddenStemEntry]
    var isDayMaster  = false

    private var isZh: Bool { AppLanguageOption.isChinese }

    var body: some View {
        VStack(spacing: 4) {
            Text(label)
                .font(AppFonts.caption)
                .foregroundStyle(isDayMaster ? AppColors.accentGold : AppColors.textSecondary)

            // Stem cell
            RoundedRectangle(cornerRadius: 10)
                .fill(isDayMaster ? AppColors.accentGold.opacity(0.15) : AppColors.backgroundBase)
                .frame(maxWidth: .infinity).frame(height: 44)
                .overlay(
                    VStack(spacing: 0) {
                        Text(pillar.stemZh)
                            .font(AppFonts.titleMedium)
                            .foregroundStyle(isDayMaster ? AppColors.accentGold : AppColors.textPrimary)
                        if !isZh {
                            Text(pillar.stemEn)
                                .font(.system(size: 9))
                                .foregroundStyle(AppColors.textSecondary)
                        }
                    }
                )

            // Branch cell
            RoundedRectangle(cornerRadius: 10)
                .fill(AppColors.backgroundBase)
                .frame(maxWidth: .infinity).frame(height: 44)
                .overlay(
                    VStack(spacing: 0) {
                        Text(pillar.branchZh)
                            .font(AppFonts.titleMedium).foregroundStyle(AppColors.textPrimary)
                        if !isZh {
                            Text(pillar.branchEn)
                                .font(.system(size: 9))
                                .foregroundStyle(AppColors.textSecondary)
                        }
                    }
                )

            // Hidden stems
            VStack(spacing: 2) {
                ForEach(hiddenStems, id: \.stemIndex) { hs in
                    Text(StemNames[hs.stemIndex])
                        .font(.system(size: 10))
                        .foregroundStyle(
                            hs.weight == .main ? AppColors.textPrimary
                            : hs.weight == .mid  ? AppColors.textSecondary
                            : AppColors.textSecondary.opacity(0.6)
                        )
                }
            }
            .frame(minHeight: 36)
        }
        .frame(maxWidth: .infinity)
    }
}

// MARK: - GoldDivider

private struct GoldDivider: View {
    var body: some View {
        Divider().overlay(AppColors.accentGold.opacity(0.2))
    }
}

private struct PrecisionChip: View {
    let text: String

    var body: some View {
        Text(text)
            .font(AppFonts.caption)
            .foregroundStyle(AppColors.accentGold)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(AppColors.backgroundBase)
            .clipShape(Capsule())
            .overlay(
                Capsule()
                    .stroke(AppColors.accentGold.opacity(0.28), lineWidth: 1)
            )
    }
}

private struct BaziInfoPanel: View {
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

private struct BaziInsightSection: Identifiable {
    let id: String
    let icon: String
    let title: String
    let body: String
}

private enum BaziInsightBuilder {

    static func sections(for chart: BaziChart, isZh: Bool) -> [BaziInsightSection] {
        [
            overviewSection(chart: chart, isZh: isZh),
            hourSection(chart: chart, isZh: isZh),
            elementSection(chart: chart, isZh: isZh),
            tenGodSection(chart: chart, isZh: isZh),
            cycleSection(chart: chart, isZh: isZh)
        ]
    }

    private static func overviewSection(chart: BaziChart, isZh: Bool) -> BaziInsightSection {
        let stemIndex = chart.dayPillar.stemIndex
        let stemName = isZh ? chart.dayPillar.stemZh : chart.dayPillar.stemEn
        let elementName = isZh ? ElementNamesZh[StemElements[stemIndex]] : ElementNamesEn[StemElements[stemIndex]]
        let description = dayMasterDescription(for: stemIndex, isZh: isZh)
        let monthName = isZh ? chart.monthPillar.nameZh : chart.monthPillar.nameEn
        let body = isZh
            ? "这张盘以 \(stemName) 日主为核心，五行属\(elementName)。\(description) 月柱是 \(monthName)，它更像你所处环境与做事节奏的底色，要和日主一起看。"
            : "This chart is anchored by the \(stemName) day master, aligned with the \(elementName) element. \(description) The month pillar \(monthName) adds the surrounding rhythm and context that shape how the day master expresses itself."
        return BaziInsightSection(
            id: "overview",
            icon: "sparkles",
            title: isZh ? "先看日主主轴" : "Start With the Day Master",
            body: body
        )
    }

    private static func hourSection(chart: BaziChart, isZh: Bool) -> BaziInsightSection {
        if let hourPillar = chart.hourPillar {
            let name = isZh ? hourPillar.nameZh : hourPillar.nameEn
            let body = isZh
                ? "当前已经纳入时柱 \(name)。时柱通常更贴近内在动机、私人节奏、晚年主题，以及一些更细的十神差异，所以现在这张盘是完整四柱。"
                : "The hour pillar \(name) is included. It often refines inner motivation, private rhythm, later-life themes, and finer ten-god differences, so this chart is using the full four pillars."
            return BaziInsightSection(
                id: "hour_present",
                icon: "clock.arrow.circlepath",
                title: isZh ? "时柱已参与排盘" : "Hour Pillar Included",
                body: body
            )
        }

        let body = isZh
            ? "你这次没有填写出生时间，所以当前只排到年、月、日三柱。时柱、部分十神和一些更细的判断会留空；如果知道大概时分，建议补上后再看。"
            : "No birth time was entered, so the engine is only using the year, month, and day pillars. The hour pillar and some finer ten-god detail are intentionally left blank; add an approximate time if you know it."
        return BaziInsightSection(
            id: "hour_missing",
            icon: "clock.badge.questionmark",
            title: isZh ? "当前还是三柱版盘面" : "This Is a Three-Pillar Chart",
            body: body
        )
    }

    private static func elementSection(chart: BaziChart, isZh: Bool) -> BaziInsightSection {
        let sorted = elementPairs(chart: chart, isZh: isZh).sorted { lhs, rhs in
            if lhs.value == rhs.value { return lhs.index < rhs.index }
            return lhs.value > rhs.value
        }
        let strongest = sorted.first!
        let weakest = sorted.last!
        let body = isZh
            ? "五行里目前最强的是\(strongest.name)，最弱的是\(weakest.name)。\(elementHint(for: strongest.index, isStrongest: true, isZh: true)) \(elementHint(for: weakest.index, isStrongest: false, isZh: true)) 这里更适合把“弱”理解成需要有意识补足，而不是简单的好坏判断。"
            : "The strongest element here is \(strongest.name), while the weakest is \(weakest.name). \(elementHint(for: strongest.index, isStrongest: true, isZh: false)) \(elementHint(for: weakest.index, isStrongest: false, isZh: false)) Treat the weaker side as something to support consciously, not as a simple flaw."
        return BaziInsightSection(
            id: "elements",
            icon: "leaf",
            title: isZh ? "五行怎么读" : "Reading the Elements",
            body: body
        )
    }

    private static func tenGodSection(chart: BaziChart, isZh: Bool) -> BaziInsightSection {
        let visibleGods = [
            chart.tenGods.yearStemGod,
            chart.tenGods.monthStemGod,
            chart.tenGods.hourStemGod,
            Optional(chart.tenGods.yearBranchGod),
            Optional(chart.tenGods.monthBranchGod),
            Optional(chart.tenGods.dayBranchGod),
            chart.tenGods.hourBranchGod
        ].compactMap { $0 }

        let dominant = Dictionary(grouping: visibleGods, by: { isZh ? $0.name : $0.nameEn })
            .max { lhs, rhs in
                if lhs.value.count == rhs.value.count {
                    return lhs.key < rhs.key
                }
                return lhs.value.count < rhs.value.count
            }?.value.first

        let title = isZh ? (dominant?.name ?? "十神") : (dominant?.nameEn ?? "Ten Gods")
        let description = dominant.map { tenGodDescription(for: $0, isZh: isZh) }
            ?? (isZh ? "十神是拿其他干支和日主比较之后得到的关系名词，用来看你更常通过哪种方式处理关系、表达与资源。" : "Ten gods are relationship labels created by comparing other stems and branches to the day master. They help show how you tend to handle people, expression, and resources.")
        let body = isZh
            ? "当前盘里更突出的十神是 \(title)。\(description) 读十神时，不必把它理解成绝对命运，它更像是你常用的一种心理与行为模式。"
            : "The most visible ten-god pattern right now is \(title). \(description) Rather than treating ten gods as fixed destiny, it is more useful to read them as recurring behavioral and psychological patterns."
        return BaziInsightSection(
            id: "ten_gods",
            icon: "person.2",
            title: isZh ? "十神怎么理解" : "How to Read the Ten Gods",
            body: body
        )
    }

    private static func cycleSection(chart: BaziChart, isZh: Bool) -> BaziInsightSection {
        let body = isZh
            ? "这张盘是\(chart.cycleDirection)，\(chart.startingAge)岁起运。大运可以先理解成每十年环境主题的切换，不是单年的吉凶宣判；先看自己正落在哪一步，再回头对照四柱、十神与五行，会更容易读懂。"
            : "This chart moves in the \(chart.cycleDirection) direction and begins its major cycles at age \(chart.startingAge). Major cycles are better read as ten-year shifts in environment and emphasis, not as one-line verdicts; first find the current cycle, then read it alongside the pillars, ten gods, and element balance."
        return BaziInsightSection(
            id: "cycles",
            icon: "point.topleft.down.curvedto.point.bottomright.up",
            title: isZh ? "大运怎么看" : "How to Read the Major Cycles",
            body: body
        )
    }

    private static func dayMasterDescription(for stemIndex: Int, isZh: Bool) -> String {
        switch stemIndex {
        case 0:
            return isZh ? "甲木常像向上生长的大树，重视方向感、原则和伸展空间。" : "Jia Wood often acts like a tall tree, valuing direction, principles, and room to grow."
        case 1:
            return isZh ? "乙木更像花草藤蔓，细腻、灵活，也很在意环境中的支持与质感。" : "Yi Wood is more like flowers or vines: flexible, subtle, and highly responsive to its environment."
        case 2:
            return isZh ? "丙火带着外放和照亮感，通常更容易把热情与存在感带到表面。" : "Bing Fire tends to radiate outward, bringing warmth, visibility, and expressive presence."
        case 3:
            return isZh ? "丁火像灯烛，敏感而专注，往往更在乎温度、关系和内在的光。" : "Ding Fire is like candlelight: focused, sensitive, and concerned with warmth and inner illumination."
        case 4:
            return isZh ? "戊土更像厚实的山地，稳定、扛事，也容易先从整体结构来判断问题。" : "Wu Earth resembles solid ground or a mountain: steady, reliable, and structurally minded."
        case 5:
            return isZh ? "己土像可塑的田土，擅长承接与调整，常常默默把细节照顾起来。" : "Ji Earth is like cultivated soil: adaptive, receptive, and good at quietly tending detail."
        case 6:
            return isZh ? "庚金更像未经打磨的金属，直接、果断，喜欢清晰边界和执行效率。" : "Geng Metal feels like raw forged metal: direct, decisive, and drawn to clear edges and action."
        case 7:
            return isZh ? "辛金像珠玉与精工，更重质地、审美和分寸感，也更讲究精确。" : "Xin Metal is closer to refined metal or jewelry, emphasizing precision, refinement, and taste."
        case 8:
            return isZh ? "壬水像江海大水，流动性强，格局大，也常通过连接与变化来推进事情。" : "Ren Water resembles rivers and seas: expansive, fluid, and inclined toward movement and connection."
        case 9:
            return isZh ? "癸水像雨露与雾气，感受细，反应快，常在看不见的层面先捕捉变化。" : "Gui Water is like mist or rain: subtle, perceptive, and quick to sense shifts beneath the surface."
        default:
            return ""
        }
    }

    private static func elementPairs(chart: BaziChart, isZh: Bool) -> [(index: Int, name: String, value: Int)] {
        [
            (0, isZh ? "木" : "Wood", chart.fiveElements.wood),
            (1, isZh ? "火" : "Fire", chart.fiveElements.fire),
            (2, isZh ? "土" : "Earth", chart.fiveElements.earth),
            (3, isZh ? "金" : "Metal", chart.fiveElements.metal),
            (4, isZh ? "水" : "Water", chart.fiveElements.water)
        ]
    }

    private static func elementHint(for element: Int, isStrongest: Bool, isZh: Bool) -> String {
        switch (element, isStrongest, isZh) {
        case (0, true, true): return "木强时，成长性、方向感和主动延展会更明显。"
        case (0, false, true): return "木弱时，可以多留意边界里的成长空间与持续性。"
        case (1, true, true): return "火强时，表达、热情与被看见的需求通常更突出。"
        case (1, false, true): return "火弱时，节奏感、自我点燃和情绪温度可能更需要经营。"
        case (2, true, true): return "土强时，稳定、现实感与承托能力会成为明显优势。"
        case (2, false, true): return "土弱时，安全感、落地执行和界限感值得多补一点。"
        case (3, true, true): return "金强时，判断、规则感和收束能力通常更突出。"
        case (3, false, true): return "金弱时，决断、筛选与说清楚底线会更需要刻意练习。"
        case (4, true, true): return "水强时，感知、流动性和适应变化的能力会比较亮眼。"
        case (4, false, true): return "水弱时，弹性、休息和情绪流动感往往更需要照顾。"
        case (0, true, false): return "Strong Wood emphasizes growth, direction, and outward development."
        case (0, false, false): return "Weak Wood often asks for more room to grow steadily within your boundaries."
        case (1, true, false): return "Strong Fire highlights expression, warmth, and the need to be seen."
        case (1, false, false): return "Weak Fire often points to tending motivation, rhythm, and emotional warmth more intentionally."
        case (2, true, false): return "Strong Earth supports steadiness, realism, and reliable containment."
        case (2, false, false): return "Weak Earth suggests giving more care to grounding, security, and follow-through."
        case (3, true, false): return "Strong Metal highlights judgment, structure, and the ability to cut clearly."
        case (3, false, false): return "Weak Metal often means boundaries, clarity, and decisiveness need more conscious support."
        case (4, true, false): return "Strong Water highlights sensitivity, adaptability, and movement."
        case (4, false, false): return "Weak Water often asks for more rest, flexibility, and emotional flow."
        default: return ""
        }
    }

    private static func tenGodDescription(for god: TenGodEntry, isZh: Bool) -> String {
        let name = god.name
        switch name {
        case "比肩":
            return isZh ? "它常对应自我主张、并肩竞争和按自己的步调做事。" : "It often points to self-assertion, peer dynamics, and moving at your own pace."
        case "劫财":
            return isZh ? "它更像强烈的主动争取，也提醒你留意资源分配和边界。" : "It carries a stronger impulse to contend or claim, and asks for care around resource boundaries."
        case "食神":
            return isZh ? "它偏向自然表达、创作输出、享受过程和温和地释放能力。" : "It leans toward natural expression, output, creativity, and releasing talent with ease."
        case "伤官":
            return isZh ? "它常带来锋利表达、反思权威和不想被框住的冲劲。" : "It often brings sharper expression, challenge to authority, and a dislike of constraint."
        case "偏财":
            return isZh ? "它更像对机会、资源调度和外部回报的敏感度。" : "It reflects sensitivity to opportunity, flexible resources, and external gain."
        case "正财":
            return isZh ? "它强调稳定经营、现实责任和一步一步把事情落地。" : "It emphasizes steady management, practical responsibility, and grounded results."
        case "七杀":
            return isZh ? "它会让人对压力、挑战和迅速应战更有反应。" : "It can show strong responsiveness to pressure, challenge, and decisive action."
        case "正官":
            return isZh ? "它通常和秩序、规范、责任感以及社会角色感有关。" : "It is often linked to order, rules, responsibility, and social role."
        case "偏印":
            return isZh ? "它偏向直觉、防御、本能理解和跳脱常规的思路。" : "It leans toward intuition, protective instinct, and unconventional understanding."
        case "正印":
            return isZh ? "它常和学习、支持、修复感与被滋养的经验有关。" : "It is often tied to learning, support, repair, and the feeling of being resourced."
        default:
            return isZh ? "它是相对于日主的一种关系名称。" : "It is one relationship label relative to the day master."
        }
    }
}

private struct CityMetadataPanel: View {
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
        VStack(alignment: .leading, spacing: 6) {
            Text(isZh ? "\(city.nameZh) · \(city.country)" : "\(city.nameEn) · \(city.country)")
                .font(AppFonts.bodyMedium)
                .foregroundStyle(AppColors.textPrimary)
            Text(city.timeZoneId)
                .font(AppFonts.bodySmall)
                .foregroundStyle(AppColors.accentGold)
            Text(
                isZh
                    ? "法定时区偏移：\(offsetLabel)。如该时区历史上存在 DST，系统会按 IANA 时区库自动处理。"
                    : "Standard legal offset: \(offsetLabel). Historical DST, when applicable, is resolved from the IANA time-zone database."
            )
            .font(AppFonts.bodySmall)
            .foregroundStyle(AppColors.textSecondary)
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(14)
        .background(AppColors.backgroundBase)
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

private struct BaziCityPickerSheet: View {
    @Binding var searchText: String

    let cities: [BirthCity]
    let selectedCity: BirthCity?
    let isZh: Bool
    let onSelect: (BirthCity?) -> Void

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
                Section {
                    CityOptionRow(
                        title: isZh ? "不选城市（按北京时间估算）" : "No city (Beijing legal time)",
                        subtitle: isZh
                            ? "使用 Asia/Shanghai / UTC+8 作为兜底时区"
                            : "Use Asia/Shanghai / UTC+8 as the fallback time zone",
                        isSelected: selectedCity == nil
                    ) {
                        onSelect(nil)
                    }
                }

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
                    Section(
                        (isZh ? "搜索结果" : "Search Results")
                        + " (\(searchResults.count))"
                    ) {
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
        CityOptionRow(
            title: isZh ? city.nameZh : city.nameEn,
            subtitle: "\(city.timeZoneId) · \(city.country)",
            isSelected: selectedCity?.id == city.id
        ) {
            onSelect(city)
        }
    }
}

private struct CityOptionRow: View {
    let title: String
    let subtitle: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(AppFonts.bodyMedium)
                        .foregroundStyle(AppColors.textPrimary)
                    Text(subtitle)
                        .font(AppFonts.bodySmall)
                        .foregroundStyle(AppColors.textSecondary)
                }
                Spacer()
                if isSelected {
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
