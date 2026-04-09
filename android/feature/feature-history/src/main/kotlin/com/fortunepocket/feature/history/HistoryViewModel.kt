package com.fortunepocket.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fortunepocket.core.data.repository.ReadingRepository
import com.fortunepocket.core.model.ReadingRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ReadingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { records ->
                _uiState.update {
                    it.copy(records = records, isLoading = false)
                }
            }
        }
    }

    fun selectRecord(record: ReadingRecord?) {
        _uiState.update { it.copy(selectedRecord = record) }
    }

    fun deleteRecord(record: ReadingRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(record.id)
            _uiState.update { current ->
                current.copy(selectedRecord = current.selectedRecord?.takeUnless { it.id == record.id })
            }
        }
    }
}
