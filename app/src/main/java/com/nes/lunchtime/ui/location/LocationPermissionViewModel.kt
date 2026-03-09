package com.nes.lunchtime.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nes.lunchtime.location.LocationPermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationPermissionViewModel @Inject constructor(
    private val locationPermissionManager: LocationPermissionManager
) : ViewModel() {

    sealed class PermissionState {
        data object Loading : PermissionState()
        data object PermissionRequired : PermissionState()
        data object PermissionDenied : PermissionState()
        data object Granted : PermissionState()
        data class Error(val message: String) : PermissionState()
    }

    private val _state = MutableStateFlow<PermissionState>(PermissionState.Loading)
    val state: StateFlow<PermissionState> = _state.asStateFlow()

    init {
        observePermission()
    }

    private fun observePermission() {
        viewModelScope.launch {
            locationPermissionManager.permissionState.collect { permissionState ->
                when (permissionState) {
                    LocationPermissionManager.PermissionState.Granted ->
                        _state.emit(PermissionState.Granted)
                    LocationPermissionManager.PermissionState.Denied ->
                        _state.emit(PermissionState.PermissionDenied)
                    LocationPermissionManager.PermissionState.ShowRationale ->
                        _state.emit(PermissionState.PermissionRequired)
                    LocationPermissionManager.PermissionState.Unknown ->
                        locationPermissionManager.checkPermission()
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
            _state.emit(PermissionState.Error("Location permission is required to show nearby restaurants"))
        }
    }

    fun onPermissionDismissed() {
        viewModelScope.launch {
            _state.emit(PermissionState.Error("Location permission is required"))
        }
    }

    fun retry() {
        viewModelScope.launch {
            locationPermissionManager.checkPermission()
        }
    }
}
