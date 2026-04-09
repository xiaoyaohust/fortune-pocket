import SwiftUI
import SwiftData

struct HistoryView: View {
    @Query(sort: \ReadingRecord.createdAt, order: .reverse)
    private var records: [ReadingRecord]
    @Environment(\.modelContext) private var modelContext

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
                List {
                    ForEach(records) { record in
                        NavigationLink {
                            HistoryDetailView(record: record)
                        } label: {
                            HistoryRecordRow(record: record)
                        }
                        .listRowBackground(AppColors.backgroundElevated)
                        .swipeActions(edge: .trailing) {
                            Button(role: .destructive) {
                                delete(record)
                            } label: {
                                Text(String.appLocalized("history_delete"))
                            }
                        }
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
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
        .padding(.vertical, 8)
    }
}

private struct HistoryDetailView: View {
    let record: ReadingRecord

    private var presentation: ReadingPresentation? {
        ReadingPresentationBuilder.presentation(for: record)
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
