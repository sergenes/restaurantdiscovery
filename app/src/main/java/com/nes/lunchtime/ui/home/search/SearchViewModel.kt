package com.nes.lunchtime.ui.home.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.nes.lunchtime.domain.GetRestaurantsUseCase
import com.nes.lunchtime.domain.Restaurant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : ViewModel() {

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

    fun getRestaurantsByText(searchText: String, location: LatLng) {
        if (searchText.isBlank()) {
            _uiState.value = UiState.Success(emptyList())
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

    private fun searchRestaurants(searchText: String, location: LatLng) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = getRestaurantsUseCase.search(searchText, location)
            
            _uiState.value = result.fold(
                onSuccess = { restaurants ->
                    UiState.Success(restaurants)
                },
                onFailure = { exception ->
                    UiState.Error(exception.localizedMessage ?: "Unknown error occurred")
                }
            )
        }
    }

    fun clearSearch() {
        _uiState.value = UiState.Initial
        lastSearchParams = null
    }
}
