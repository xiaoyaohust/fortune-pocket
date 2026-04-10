import SwiftUI
import UIKit

struct OracleShareCardPayload {
    let eyebrow: String
    let title: String
    let subtitle: String
    let headline: String
    let summary: String
    let guidance: String
    let chips: [String]
}

enum OracleShareCardBuilder {

    static func payload(for reading: TarotReading) -> OracleShareCardPayload {
        let isZh = AppLanguageOption.isChinese
        return OracleShareCardPayload(
            eyebrow: isZh ? "Fortune Pocket · 塔罗" : "Fortune Pocket · Tarot",
            title: reading.theme.localizedName,
            subtitle: reading.date.localizedDateString,
            headline: firstParagraph(of: reading.overallEnergy),
            summary: firstParagraph(of: reading.focusInsight),
            guidance: firstParagraph(of: reading.advice),
            chips: reading.drawnCards.map {
                let cardName = isZh ? $0.card.nameZh : $0.card.nameEn
                let orientation = $0.isUpright ? (isZh ? "正位" : "Upright") : (isZh ? "逆位" : "Reversed")
                return "\(cardName) · \(orientation)"
            }
        )
    }

    static func payload(for reading: HoroscopeReading) -> OracleShareCardPayload {
        let isZh = AppLanguageOption.isChinese
        return OracleShareCardPayload(
            eyebrow: isZh ? "Fortune Pocket · 本命盘" : "Fortune Pocket · Natal Chart",
            title: reading.chartSignature,
            subtitle: "\(reading.birthDateText) · \(reading.birthCityName)",
            headline: firstParagraph(of: reading.chartSummary),
            summary: firstParagraph(of: reading.overall),
            guidance: firstParagraph(of: reading.advice),
            chips: [
                "\(isZh ? "太阳" : "Sun") · \(reading.sign.localizedName)",
                "\(isZh ? "月亮" : "Moon") · \(reading.moonSign.localizedName)",
                "\(isZh ? "上升" : "Rising") · \((reading.risingSign?.localizedName ?? ""))"
            ].filter { !$0.hasSuffix("· ") }
        )
    }

    static func payload(for chart: BaziChart) -> OracleShareCardPayload {
        let isZh = AppLanguageOption.isChinese
        let pillars = [chart.yearPillar.nameZh, chart.monthPillar.nameZh, chart.dayPillar.nameZh, chart.hourPillar?.nameZh]
            .compactMap { $0 }
            .joined(separator: " · ")
        let dayMaster = isZh ? chart.dayPillar.stemZh : chart.dayPillar.stemEn
        let dominantElement: String = {
            let scores = [
                (isZh ? "木" : "Wood", chart.fiveElements.wood),
                (isZh ? "火" : "Fire", chart.fiveElements.fire),
                (isZh ? "土" : "Earth", chart.fiveElements.earth),
                (isZh ? "金" : "Metal", chart.fiveElements.metal),
                (isZh ? "水" : "Water", chart.fiveElements.water)
            ]
            return scores.max(by: { $0.1 < $1.1 })?.0 ?? (isZh ? "木" : "Wood")
        }()
        let summary = isZh
            ? "这张盘以\(dayMaster)日主为核心，当前五行里 \(dominantElement) 的力量更显眼，说明你更适合先从自己的节奏和稳定感出发理解今天。"
            : "This chart is anchored by the \(dayMaster) day master, with \(dominantElement) currently standing out in the five-element balance. Start by understanding today's rhythm through steadiness and pacing."
        let guidance = isZh
            ? "先看四柱之间的结构，再结合大运与五行强弱去理解你今天更适合稳住哪一块。"
            : "Read the structure of the four pillars first, then let the major cycles and element balance show where today's steadier focus belongs."
        return OracleShareCardPayload(
            eyebrow: isZh ? "Fortune Pocket · 八字" : "Fortune Pocket · Bazi",
            title: isZh ? "四柱命盘" : "Four Pillars Chart",
            subtitle: pillars,
            headline: isZh ? "\(dayMaster)日主 · \(chart.cycleDirection)" : "\(dayMaster) Day Master · \(chart.cycleDirection)",
            summary: firstParagraph(of: summary),
            guidance: firstParagraph(of: guidance),
            chips: [
                "\(isZh ? "起运" : "Starts") · \(chart.startingAge)",
                "\(isZh ? "木" : "Wood") \(chart.fiveElements.wood)",
                "\(isZh ? "火" : "Fire") \(chart.fiveElements.fire)",
                "\(isZh ? "土" : "Earth") \(chart.fiveElements.earth)"
            ]
        )
    }

    private static func firstParagraph(of text: String) -> String {
        for separator in ["\n\n", "。", ". ", "！", "？"] {
            if let range = text.range(of: separator) {
                return String(text[..<range.upperBound]).trimmingCharacters(in: .whitespacesAndNewlines)
            }
        }
        return text
    }
}

enum ShareCardExporter {

    @MainActor
    static func export(payload: OracleShareCardPayload, fileName: String) throws -> URL {
        let view = OracleShareCardView(payload: payload)
            .frame(width: 1080, height: 1620)
            .background(AppColors.gradientBackground)

        let renderer = ImageRenderer(content: view)
        renderer.scale = 2

        guard let image = renderer.uiImage,
              let data = image.pngData() else {
            throw ShareCardExportError.renderFailed
        }

        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent(fileName)
            .appendingPathExtension("png")
        try data.write(to: url, options: .atomic)
        return url
    }
}

enum ShareCardExportError: Error {
    case renderFailed
}

struct ActivityShareSheet: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

private struct OracleShareCardView: View {
    let payload: OracleShareCardPayload

    var body: some View {
        ZStack {
            AppColors.gradientBackground

            VStack(alignment: .leading, spacing: 28) {
                VStack(alignment: .leading, spacing: 14) {
                    Text(payload.eyebrow.uppercased())
                        .font(.system(size: 32, weight: .medium))
                        .foregroundStyle(AppColors.accentGold)
                        .tracking(6)

                    Text(payload.title)
                        .font(.system(size: 74, weight: .light))
                        .foregroundStyle(AppColors.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)

                    Text(payload.subtitle)
                        .font(.system(size: 30, weight: .regular))
                        .foregroundStyle(AppColors.textSecondary)
                }

                VStack(alignment: .leading, spacing: 18) {
                    Text(payload.headline)
                        .font(.system(size: 48, weight: .medium))
                        .foregroundStyle(AppColors.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)

                    Text(payload.summary)
                        .font(.system(size: 28, weight: .regular))
                        .foregroundStyle(AppColors.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)

                    Text(payload.guidance)
                        .font(.system(size: 30, weight: .regular))
                        .foregroundStyle(AppColors.textPrimary)
                        .padding(.top, 8)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(42)
                .background(
                    RoundedRectangle(cornerRadius: 38)
                        .fill(AppColors.backgroundElevated.opacity(0.94))
                        .overlay(
                            RoundedRectangle(cornerRadius: 38)
                                .stroke(AppColors.accentGold.opacity(0.22), lineWidth: 2)
                        )
                )

                if !payload.chips.isEmpty {
                    FlowingChipLayout(tags: payload.chips)
                }

                Spacer()

                HStack {
                    Text("Fortune Pocket")
                        .font(.system(size: 30, weight: .medium))
                        .foregroundStyle(AppColors.accentGold)
                    Spacer()
                    Text(AppLanguageOption.isChinese ? "离线生成 · 可分享" : "Offline • Shareable")
                        .font(.system(size: 26, weight: .regular))
                        .foregroundStyle(AppColors.textSecondary)
                }
            }
            .padding(68)
        }
    }
}

private struct FlowingChipLayout: View {
    let tags: [String]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            ForEach(chunked(tags, size: 2), id: \.self) { row in
                HStack(spacing: 14) {
                    ForEach(row, id: \.self) { tag in
                        Text(tag)
                            .font(.system(size: 24, weight: .medium))
                            .foregroundStyle(AppColors.accentGold)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 14)
                            .background(AppColors.backgroundElevated.opacity(0.88))
                            .clipShape(Capsule())
                    }
                }
            }
        }
    }

    private func chunked(_ values: [String], size: Int) -> [[String]] {
        stride(from: 0, to: values.count, by: size).map {
            Array(values[$0..<min($0 + size, values.count)])
        }
    }
}
