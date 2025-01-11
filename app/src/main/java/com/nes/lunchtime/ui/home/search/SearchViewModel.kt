package com.nes.lunchtime.ui.home.search

import com.nes.lunchtime.repo.RestaurantsRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nes.lunchtime.domain.Restaurant
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: RestaurantsRepository
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
            try {
                _uiState.value = UiState.Loading

                val result = repo.getRestaurantsByText(searchText, location)
                _uiState.value = when {
                    result.isSuccess -> {
                        val restaurants = result.getOrNull()
                        if (restaurants.isNullOrEmpty()) {
                            UiState.Error("No restaurants found matching '$searchText'")
                        } else {
                            UiState.Success(restaurants.sortedBy { it.distanceInMeters })
                        }
                    }

                    result.isFailure -> {
                        when (val exception = result.exceptionOrNull()) {
                            is CancellationException -> throw exception // Don't catch cancellation
                            null -> UiState.Error("Unknown error occurred")
                            else -> UiState.Error(
                                exception.localizedMessage ?: "Unknown error occurred"
                            )
                        }
                    }

                    else -> UiState.Error("Unknown error occurred")
                }
            } catch (e: CancellationException) {
                throw e // Don't catch cancellation
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }

    fun clearSearch() {
        _uiState.value = UiState.Initial
        lastSearchParams = null
    }
}