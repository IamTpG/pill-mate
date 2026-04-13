package com.example.pillmate.presentation.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pillmate.domain.model.DrugInfo
import com.example.pillmate.domain.repository.DrugLibraryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DrugLibraryUiState(
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val searchResults: List<DrugInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDrug: DrugInfo? = null,
    val hasSearched: Boolean = false
)

class DrugLibraryViewModel(
    private val drugRepository: DrugLibraryRepository,
    application: Application
) : ViewModel() {

    private val sharedPrefs = application.getSharedPreferences("drug_library_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(DrugLibraryUiState(
        recentSearches = loadRecentSearches()
    ))
    val uiState: StateFlow<DrugLibraryUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    private fun loadRecentSearches(): List<String> {
        val savedString = sharedPrefs.getString("recent_searches_csv", null)
        return if (savedString.isNullOrBlank()) emptyList() else savedString.split("||")
    }

    private fun saveRecentSearches(history: List<String>) {
        sharedPrefs.edit().putString("recent_searches_csv", history.joinToString("||")).apply()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query, error = null) }
    }

    fun submitSearch(query: String) {
        if (query.isBlank()) return
        
        _uiState.update { 
            val updatedHistory = if (!it.recentSearches.contains(query)) {
                (listOf(query) + it.recentSearches).take(5)
            } else {
                it.recentSearches
            }
            
            saveRecentSearches(updatedHistory)
            
            it.copy(
                isLoading = true, 
                hasSearched = true,
                error = null,
                recentSearches = updatedHistory
            ) 
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            drugRepository.searchDrug(query).collect { result ->
                result.onSuccess { drugs ->
                    _uiState.update { it.copy(isLoading = false, searchResults = drugs) }
                }.onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, error = "Failed to fetch data: ${exception.message}", searchResults = emptyList()) }
                }
            }
        }
    }

    fun clearRecentSearches() {
        saveRecentSearches(emptyList())
        _uiState.update { it.copy(recentSearches = emptyList()) }
    }

    fun selectDrug(drug: DrugInfo) {
        _uiState.update { it.copy(selectedDrug = drug) }
    }

    fun clearSelectedDrug() {
        _uiState.update { it.copy(selectedDrug = null) }
    }
}
