package com.fortunepocket.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortunepocket.core.content.ContentLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentLoader: ContentLoader
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val quotesData = contentLoader.loadDailyQuotes()
                // Use day-of-year for stable daily rotation
                val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                val index = (dayOfYear - 1) % quotesData.quotes.size
                _uiState.update {
                    it.copy(dailyQuote = quotesData.quotes[index], isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
