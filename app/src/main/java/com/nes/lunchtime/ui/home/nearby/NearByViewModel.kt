package com.nes.lunchtime.ui.home.nearby

import com.google.android.gms.maps.model.LatLng
import com.nes.lunchtime.domain.GetRestaurantsUseCase
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the nearby restaurants screen.
 * Extends BaseViewModel to utilize common loading and error handling patterns.
 */
@HiltViewModel
class NearByViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : BaseViewModel() {

    sealed class UiState {
        data object Loading : UiState()
        data class Success(val restaurants: List<Restaurant>) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Success(emptyList()))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var lastLocation: LatLng? = null

    fun onLoad(location: LatLng) {
        lastLocation = location
        loadRestaurants(location)
    }

    fun retry() {
        lastLocation?.let { loadRestaurants(it) }
    }

    /**
     * Loads nearby restaurants for the given location.
     * Uses the BaseViewModel's executeWithLoading to handle loading states and errors consistently.
     */
    private fun loadRestaurants(location: LatLng) {
        executeWithLoading(
            uiState = _uiState,
            loadingState = UiState.Loading,
            block = { getRestaurantsUseCase.getNearby(location) },
            onSuccess = { restaurants -> UiState.Success(restaurants) },
            onError = { message -> UiState.Error(message) }
        )
    }
}
