package com.nes.lunchtime.ui.home.search

import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.nes.lunchtime.domain.GetRestaurantsUseCase
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for the search screen with debouncing support.
 * Extends BaseViewModel to utilize common loading and error handling patterns.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : BaseViewModel() {

    sealed class UiState {
        data object Initial : UiState()
        data object Loading : UiState()
        data class Success(val restaurants: List<Restaurant>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private data class SearchParams(val query: String, val location: LatLng)
    private var lastSearchParams: SearchParams? = null

    private val _searchQuery = MutableStateFlow("")
    private var currentLocation: LatLng? = null

    init {
        // Debounce search queries to avoid excessive API calls
        _searchQuery
            .debounce(500) // Wait 500ms after user stops typing
            .distinctUntilChanged()
            .filter { it.isNotBlank() }
            .onEach { query ->
                currentLocation?.let { location ->
                    searchRestaurants(query, location)
                }
            }
            .launchIn(viewModelScope)
    }

    fun setLocation(location: LatLng) {
        currentLocation = location
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _uiState.value = UiState.Initial
            lastSearchParams = null
        }
    }

    fun getRestaurantsByText(searchText: String, location: LatLng) {
        if (searchText.isBlank()) {
            _uiState.value = UiState.Initial
            return
        }

        lastSearchParams = SearchParams(searchText, location)
        searchRestaurants(searchText, location)
    }

    fun retry() {
        lastSearchParams?.let { params ->
            searchRestaurants(params.query, params.location)
        }
    }

    /**
     * Searches for restaurants by text and location.
     * Uses the BaseViewModel's executeWithLoading to handle loading states and errors consistently.
     */
    private fun searchRestaurants(searchText: String, location: LatLng) {
        executeWithLoading(
            uiState = _uiState,
            loadingState = UiState.Loading,
            block = { getRestaurantsUseCase.search(searchText, location) },
            onSuccess = { restaurants -> UiState.Success(restaurants) },
            onError = { message -> UiState.Error(message) }
        )
    }

    fun clearSearch() {
        _uiState.value = UiState.Initial
        lastSearchParams = null
    }
}
