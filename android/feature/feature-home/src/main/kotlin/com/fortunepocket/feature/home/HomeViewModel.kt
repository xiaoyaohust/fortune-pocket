package com.fortunepocket.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortunepocket.core.content.ContentLoader
import com.fortunepocket.core.data.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentLoader: ContentLoader,
    private val userPreferencesDataStore: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val birthdayMillis = runCatching {
                    userPreferencesDataStore.getUserBirthdayValue()
                }.getOrDefault(-1L)
                val ritual = DailyRitualBuilder.build(
                    contentLoader = contentLoader,
                    savedBirthdayMillis = birthdayMillis
                )
                _uiState.update {
                    it.copy(dailyRitual = ritual, isLoading = false, error = null)
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        error = "daily_ritual_unavailable",
                        isLoading = false
                    )
                }
            }
        }
    }
}
