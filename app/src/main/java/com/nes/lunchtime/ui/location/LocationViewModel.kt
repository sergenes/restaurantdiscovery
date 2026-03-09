package com.nes.lunchtime.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nes.lunchtime.location.LocationRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    sealed class LocationState {
        data object Loading : LocationState()
        data class LocationAvailable(val location: LatLng) : LocationState()
        data class Error(val message: String) : LocationState()
    }

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Loading)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private var locationJob: Job? = null

    init {
        startLocationUpdates()
    }

    private fun startLocationUpdates(showLoading: Boolean = true) {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            if (showLoading) _locationState.emit(LocationState.Loading)
            locationRepository.getLocationUpdates().collect { result ->
                when (result) {
                    is LocationRepository.LocationResult.Success ->
                        _locationState.emit(LocationState.LocationAvailable(result.location))
                    is LocationRepository.LocationResult.Error ->
                        _locationState.emit(
                            LocationState.Error(
                                result.exception.localizedMessage ?: "Unable to get location"
                            )
                        )
                }
            }
        }
    }

    // Keep showing the current location while a fresh fix is being acquired so
    // HomeScreen is never unmounted mid-session (preserves map/list view choice).
    fun refreshLocation() {
        startLocationUpdates(showLoading = false)
    }
}
