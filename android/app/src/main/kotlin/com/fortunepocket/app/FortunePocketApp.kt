package com.fortunepocket.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.fortunepocket.core.data.datastore.UserPreferencesDataStore
import com.fortunepocket.R
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class FortunePocketApp : Application() {
    @Inject lateinit var userPreferencesDataStore: UserPreferencesDataStore

    override fun onCreate() {
        super.onCreate()
        applySavedLanguage()
        createNotificationChannels()
    }

    private fun applySavedLanguage() {
        val language = runBlocking {
            userPreferencesDataStore.getAppLanguageValue()
        }
        val locales = when (language) {
            "zh" -> LocaleListCompat.forLanguageTags("zh")
            "en" -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                CHANNEL_DAILY_REMINDER,
                getString(R.string.notification_channel_reminder_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_reminder_desc)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    companion object {
        const val CHANNEL_DAILY_REMINDER = "fortune_pocket_daily_reminder"
    }
}
