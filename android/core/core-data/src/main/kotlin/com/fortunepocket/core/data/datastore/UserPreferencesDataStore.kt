package com.fortunepocket.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "fortune_pocket_prefs"
)

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val NOTIFICATION_HOUR = intPreferencesKey("notification_hour")
        val NOTIFICATION_MINUTE = intPreferencesKey("notification_minute")
        val USER_BIRTHDAY = longPreferencesKey("user_birthday")       // epoch millis, -1 = not set
        val USER_ZODIAC = stringPreferencesKey("user_zodiac")          // zodiac id, empty = not set
        val APP_LANGUAGE = stringPreferencesKey("app_language")        // "system" | "zh" | "en"
    }

    val notificationEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.NOTIFICATION_ENABLED] ?: false
    }

    val notificationHour: Flow<Int> = context.dataStore.data.map {
        it[Keys.NOTIFICATION_HOUR] ?: 9
    }

    val notificationMinute: Flow<Int> = context.dataStore.data.map {
        it[Keys.NOTIFICATION_MINUTE] ?: 0
    }

    val userBirthday: Flow<Long> = context.dataStore.data.map {
        it[Keys.USER_BIRTHDAY] ?: -1L
    }

    val userZodiac: Flow<String> = context.dataStore.data.map {
        it[Keys.USER_ZODIAC] ?: ""
    }

    val appLanguage: Flow<String> = context.dataStore.data.map {
        it[Keys.APP_LANGUAGE] ?: "system"
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.NOTIFICATION_HOUR] = hour
            it[Keys.NOTIFICATION_MINUTE] = minute
        }
    }

    suspend fun setUserBirthday(epochMillis: Long) {
        context.dataStore.edit { it[Keys.USER_BIRTHDAY] = epochMillis }
    }

    suspend fun setUserZodiac(zodiacId: String) {
        context.dataStore.edit { it[Keys.USER_ZODIAC] = zodiacId }
    }

    suspend fun setAppLanguage(lang: String) {
        context.dataStore.edit { it[Keys.APP_LANGUAGE] = lang }
    }

    suspend fun getNotificationEnabledValue(): Boolean = notificationEnabled.first()

    suspend fun getNotificationHourValue(): Int = notificationHour.first()

    suspend fun getNotificationMinuteValue(): Int = notificationMinute.first()

    suspend fun getUserBirthdayValue(): Long = userBirthday.first()

    suspend fun getAppLanguageValue(): String = appLanguage.first()
}
