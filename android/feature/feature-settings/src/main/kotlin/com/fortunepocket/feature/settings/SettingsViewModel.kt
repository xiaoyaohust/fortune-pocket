package com.fortunepocket.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortunepocket.core.data.datastore.UserPreferencesDataStore
import com.fortunepocket.core.data.notification.NotificationScheduler
import com.fortunepocket.core.data.repository.ReadingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore,
    private val readingRepository: ReadingRepository,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userPreferencesDataStore.notificationEnabled,
                userPreferencesDataStore.notificationHour,
                userPreferencesDataStore.notificationMinute,
                userPreferencesDataStore.userBirthday,
                userPreferencesDataStore.appLanguage
            ) { enabled, hour, minute, birthday, language ->
                SettingsUiState(
                    notificationEnabled = enabled,
                    notificationHour = hour,
                    notificationMinute = minute,
                    birthdayMillis = birthday,
                    appLanguage = language
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onNotificationEnabledChanged(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesDataStore.setNotificationEnabled(enabled)
            if (enabled) {
                val state = _uiState.value
                notificationScheduler.scheduleDailyReminder(
                    state.notificationHour,
                    state.notificationMinute
                )
            } else {
                notificationScheduler.cancelDailyReminder()
            }
        }
    }

    fun onNotificationTimeChanged(hour: Int, minute: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesDataStore.setNotificationTime(hour, minute)
            if (_uiState.value.notificationEnabled) {
                notificationScheduler.scheduleDailyReminder(hour, minute)
            }
        }
    }

    fun onBirthdayChanged(millis: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesDataStore.setUserBirthday(millis)
        }
    }

    fun onAppLanguageChanged(language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesDataStore.setAppLanguage(language)
            _uiState.update { it.copy(appLanguage = language) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            readingRepository.clearAll()
        }
    }
}
