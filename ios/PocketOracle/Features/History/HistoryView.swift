import SwiftUI
import SwiftData

struct HistoryView: View {
    @Query(sort: \ReadingRecord.createdAt, order: .reverse)
    private var records: [ReadingRecord]
    @Environment(\.modelContext) private var modelContext

    private var trajectorySnapshot: HistoryTrajectorySnapshot? {
        HistoryTrajectoryBuilder.build(records: records)
    }

    var body: some View {
        Group {
            if records.isEmpty {
                VStack(spacing: 16) {
                    Text("✦")
                        .font(AppFonts.titleLarge)
                        .foregroundStyle(AppColors.accentGold)
                    Text(String.appLocalized("history_empty"))
                        .font(AppFonts.bodyMedium)
                        .foregroundStyle(AppColors.textSecondary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(AppColors.gradientBackground.ignoresSafeArea())
            } else {
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 16) {
                        if let trajectorySnapshot {
                            HistoryTrajectoryCard(snapshot: trajectorySnapshot)
                        }

                        ForEach(records) { record in
                            HStack(spacing: 12) {
                                NavigationLink {
                                    HistoryDetailView(record: record)
                                } label: {
                                    HistoryRecordRow(record: record)
                                }
                                .buttonStyle(.plain)

                                Button(role: .destructive) {
                                    delete(record)
                                } label: {
                                    Image(systemName: "trash")
                                        .font(.system(size: 14, weight: .semibold))
                                        .foregroundStyle(AppColors.error)
                                        .frame(width: 40, height: 40)
                                        .background(AppColors.backgroundElevated)
                                        .clipShape(Circle())
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 20)
                    .padding(.bottom, 32)
                }
                .background(AppColors.gradientBackground.ignoresSafeArea())
            }
        }
        .navigationTitle(String.appLocalized("nav_history"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private func delete(_ record: ReadingRecord) {
        modelContext.delete(record)
        try? modelContext.save()
    }
}

private struct HistoryRecordRow: View {
    let record: ReadingRecord

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                Text(record.type.icon)
                    .font(AppFonts.titleLarge)
                VStack(alignment: .leading, spacing: 4) {
                    Text(record.title)
                        .font(AppFonts.titleMedium)
                        .foregroundStyle(AppColors.textPrimary)
                    Text(record.createdAt.shortDateString)
                        .font(AppFonts.caption)
                        .foregroundStyle(AppColors.textSecondary)
                }
            }

            Text(record.summary)
                .font(AppFonts.bodySmall)
                .foregroundStyle(AppColors.textSecondary)
                .lineLimit(2)
        }
        .padding(16)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
    }
}

private struct HistoryTrajectoryCard: View {
    let snapshot: HistoryTrajectorySnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(AppLanguageOption.isChinese ? "成长轨迹" : "Growth Trajectory")
                .font(AppFonts.labelSmall)
                .foregroundStyle(AppColors.accentGold)
                .tracking(2)

            Text(snapshot.headline)
                .font(AppFonts.headlineMedium)
                .foregroundStyle(AppColors.textPrimary)
                .fixedSize(horizontal: false, vertical: true)

            ForEach(snapshot.insights) { insight in
                VStack(alignment: .leading, spacing: 6) {
                    Text(insight.title)
                        .font(AppFonts.titleMedium)
                        .foregroundStyle(AppColors.accentGold)
                    Text(insight.body)
                        .font(AppFonts.bodyMedium)
                        .foregroundStyle(AppColors.textSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(14)
                .background(AppColors.backgroundBase)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 22)
    }
}

private struct HistoryDetailView: View {
    let record: ReadingRecord

    // Decoded once on init — avoids repeated JSON parsing on every render
    private let presentation: ReadingPresentation?

    init(record: ReadingRecord) {
        self.record = record
        self.presentation = ReadingPresentationBuilder.presentation(for: record)
    }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 18) {
                if let presentation {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(presentation.title)
                            .font(AppFonts.headlineLarge)
                            .foregroundStyle(AppColors.textPrimary)
                        Text(presentation.subtitle)
                            .font(AppFonts.bodySmall)
                            .foregroundStyle(AppColors.textSecondary)
                    }
                    .padding(20)
                    .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)

                    ForEach(presentation.sections) { section in
                        VStack(alignment: .leading, spacing: 10) {
                            Text(section.title)
                                .font(AppFonts.titleMedium)
                                .foregroundStyle(AppColors.accentGold)
                            Text(section.body)
                                .font(AppFonts.bodyMedium)
                                .foregroundStyle(AppColors.textPrimary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                        .padding(16)
                        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 18)
                    }

                    ShareLink(item: presentation.shareText) {
                        Label(String.appLocalized("share"), systemImage: "square.and.arrow.up")
                            .font(AppFonts.titleMedium)
                            .foregroundStyle(AppColors.backgroundDeep)
                            .frame(maxWidth: .infinity)
                            .frame(height: 56)
                            .background(AppColors.accentGold)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                } else {
                    Text(record.summary)
                        .font(AppFonts.bodyMedium)
                        .foregroundStyle(AppColors.textSecondary)
                        .padding(20)
                        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 32)
        }
        .background(AppColors.gradientBackground.ignoresSafeArea())
        .navigationTitle(record.type.localizedName)
        .navigationBarTitleDisplayMode(.inline)
        .fortuneNavigationBackButton()
    }
}
