package com.nes.lunchtime.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nes.lunchtime.location.LocationPermissionManager
import com.nes.lunchtime.location.LocationRepository
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val locationPermissionManager: LocationPermissionManager
) : ViewModel() {

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Loading)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    init {
        checkLocationPermission()
    }

    sealed class LocationState {
        data object Loading : LocationState()
        data object PermissionRequired : LocationState()
        data object PermissionDenied : LocationState()
        data class LocationAvailable(val location: LatLng) : LocationState()
        data class Error(val message: String) : LocationState()
    }

    private fun checkLocationPermission() {
        viewModelScope.launch {
            locationPermissionManager.permissionState.collect { permissionState ->
                when (permissionState) {
                    is LocationPermissionManager.PermissionState.Granted -> {
                        fetchLocation()
                    }
                    is LocationPermissionManager.PermissionState.Denied -> {
                        _locationState.emit(LocationState.PermissionDenied)
                    }
                    is LocationPermissionManager.PermissionState.ShowRationale -> {
                        _locationState.emit(LocationState.PermissionRequired)
                    }
                    LocationPermissionManager.PermissionState.Unknown -> {
                        locationPermissionManager.checkPermission()
                    }
                }
            }
        }
    }

    private fun fetchLocation() {
        viewModelScope.launch {
            _locationState.emit(LocationState.Loading)
            when (val result = locationRepository.getLastLocation()) {
                is LocationRepository.LocationResult.Success -> {
                    _locationState.emit(LocationState.LocationAvailable(result.location))
                }
                is LocationRepository.LocationResult.Error -> {
                    _locationState.emit(
                        LocationState.Error(
                            result.exception.localizedMessage
                                ?: "Unable to get location"
                        )
                    )
                }
            }
        }
    }

    fun onPermissionGranted() {
        viewModelScope.launch {
            locationPermissionManager.checkPermission()
        }
    }

    fun onPermissionDenied() {
        viewModelScope.launch {
            _locationState.emit(LocationState.Error("Location permission is required to show nearby restaurants"))
        }
    }

    fun onPermissionDismissed() {
        viewModelScope.launch {
            _locationState.emit(LocationState.Error("Location permission is required"))
        }
    }

    fun retry() {
        checkLocationPermission()
    }
}