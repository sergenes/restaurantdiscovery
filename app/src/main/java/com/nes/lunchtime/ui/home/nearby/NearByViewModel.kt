package com.nes.lunchtime.ui.home.nearby

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
class NearByViewModel @Inject constructor(
    private val getRestaurantsUseCase: GetRestaurantsUseCase
) : ViewModel() {

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

    private fun loadRestaurants(location: LatLng) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = getRestaurantsUseCase.getNearby(location)
            
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
}
