import SwiftUI
import SwiftData

struct SettingsView: View {
    @Environment(\.modelContext) private var modelContext

    @AppStorage(AppPreferenceKeys.notificationEnabled) private var notificationEnabled = false
    @AppStorage(AppPreferenceKeys.notificationHour) private var notificationHour = 9
    @AppStorage(AppPreferenceKeys.notificationMinute) private var notificationMinute = 0
    @AppStorage(AppPreferenceKeys.userBirthdayTimestamp) private var birthdayTimestamp = 0.0
    @AppStorage(AppPreferenceKeys.appLanguage) private var appLanguage = AppLanguageOption.system.rawValue

    @State private var showingClearAlert = false
    @State private var notificationMessage: String?

    private var reminderTime: Date {
        get {
            Calendar.current.date(
                from: DateComponents(hour: notificationHour, minute: notificationMinute)
            ) ?? Date()
        }
        nonmutating set {
            notificationHour = Calendar.current.component(.hour, from: newValue)
            notificationMinute = Calendar.current.component(.minute, from: newValue)
        }
    }

    private var storedBirthday: Date {
        get {
            guard birthdayTimestamp > 0 else {
                return AppPreferences.savedBirthday ?? Date()
            }
            return Date(timeIntervalSince1970: birthdayTimestamp)
        }
        nonmutating set {
            birthdayTimestamp = newValue.timeIntervalSince1970
            AppPreferences.storeBirthday(newValue)
        }
    }

    private var versionText: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
    }

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 20) {
                reminderCard
                profileCard
                dataCard
                aboutCard

                if let notificationMessage {
                    Text(notificationMessage)
                        .font(AppFonts.caption)
                        .foregroundStyle(AppColors.textSecondary)
                }
            }
            .padding(.horizontal, 20)
            .padding(.top, 20)
            .padding(.bottom, 32)
        }
        .background(AppColors.gradientBackground.ignoresSafeArea())
        .navigationTitle(String.appLocalized("nav_settings"))
        .navigationBarTitleDisplayMode(.inline)
        .alert(
            String.appLocalized("settings_clear_history"),
            isPresented: $showingClearAlert
        ) {
            Button(String.appLocalized("cancel"), role: .cancel) {}
            Button(String.appLocalized("confirm"), role: .destructive) {
                clearHistory()
            }
        } message: {
            Text(String.appLocalized("settings_clear_confirm"))
        }
        .onChange(of: notificationEnabled) { _, isEnabled in
            Task {
                if isEnabled {
                    let granted = await LocalNotificationScheduler.shared.requestAuthorization()
                    if granted {
                        await LocalNotificationScheduler.shared.scheduleDailyReminder(
                            hour: notificationHour,
                            minute: notificationMinute
                        )
                        notificationMessage = nil
                    } else {
                        notificationEnabled = false
                        notificationMessage = AppLanguageOption.isChinese
                            ? "请先在系统设置中允许通知权限。"
                            : "Please allow notification permission in system settings first."
                    }
                } else {
                    LocalNotificationScheduler.shared.cancelDailyReminder()
                    notificationMessage = nil
                }
            }
        }
        .onChange(of: notificationHour) { _, _ in rescheduleReminderIfNeeded() }
        .onChange(of: notificationMinute) { _, _ in rescheduleReminderIfNeeded() }
        .onChange(of: birthdayTimestamp) { _, newValue in
            if newValue > 0 {
                AppPreferences.storeBirthday(Date(timeIntervalSince1970: newValue))
            }
        }
    }

    private var reminderCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(String.appLocalized("settings_reminder"))
                .font(AppFonts.headlineMedium)
                .foregroundStyle(AppColors.textPrimary)

            Toggle(isOn: $notificationEnabled) {
                Text(String.appLocalized("settings_reminder"))
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(AppColors.textPrimary)
            }
            .tint(AppColors.accentGold)

            DatePicker(
                String.appLocalized("settings_reminder_time"),
                selection: Binding(
                    get: { reminderTime },
                    set: { reminderTime = $0 }
                ),
                displayedComponents: .hourAndMinute
            )
            .datePickerStyle(.compact)
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
    }

    private var profileCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(AppLanguageOption.isChinese ? "语言与生日" : "Language & Birthday")
                .font(AppFonts.headlineMedium)
                .foregroundStyle(AppColors.textPrimary)

            Picker(
                String.appLocalized("settings_language"),
                selection: $appLanguage
            ) {
                Text(String.appLocalized("settings_follow_system")).tag(AppLanguageOption.system.rawValue)
                Text("简体中文").tag(AppLanguageOption.zh.rawValue)
                Text("English").tag(AppLanguageOption.en.rawValue)
            }
            .pickerStyle(.segmented)

            DatePicker(
                String.appLocalized("settings_birthday"),
                selection: Binding(
                    get: { storedBirthday },
                    set: { storedBirthday = $0 }
                ),
                displayedComponents: .date
            )
            .datePickerStyle(.compact)

            Text(
                String.appLocalized("settings_birthday_helper")
            )
            .font(AppFonts.caption)
            .foregroundStyle(AppColors.textSecondary)
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
    }

    private var dataCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(AppLanguageOption.isChinese ? "数据管理" : "Data Management")
                .font(AppFonts.headlineMedium)
                .foregroundStyle(AppColors.textPrimary)

            Button(role: .destructive) {
                showingClearAlert = true
            } label: {
                Text(String.appLocalized("settings_clear_history"))
                    .font(AppFonts.titleMedium)
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(AppColors.error)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
    }

    private var aboutCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(String.appLocalized("settings_about"))
                .font(AppFonts.headlineMedium)
                .foregroundStyle(AppColors.textPrimary)

            Text("Fortune Pocket")
                .font(AppFonts.titleMedium)
                .foregroundStyle(AppColors.accentGold)

            Text("v\(versionText)")
                .font(AppFonts.caption)
                .foregroundStyle(AppColors.textSecondary)

            Text(String.appLocalized("disclaimer_full"))
                .font(AppFonts.bodySmall)
                .foregroundStyle(AppColors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(20)
        .fortuneCard(background: AppColors.backgroundElevated, cornerRadius: 20)
    }

    private func rescheduleReminderIfNeeded() {
        guard notificationEnabled else { return }
        Task {
            await LocalNotificationScheduler.shared.scheduleDailyReminder(
                hour: notificationHour,
                minute: notificationMinute
            )
        }
    }

    private func clearHistory() {
        let descriptor = FetchDescriptor<ReadingRecord>()
        let records = (try? modelContext.fetch(descriptor)) ?? []
        records.forEach { modelContext.delete($0) }
        try? modelContext.save()
    }
}
