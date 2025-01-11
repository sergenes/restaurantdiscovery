package com.nes.lunchtime.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationPermissionManager(private val context: Context) {
    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.Unknown)
    val permissionState = _permissionState.asStateFlow()

    sealed class PermissionState {
        data object Unknown : PermissionState()
        data object Granted : PermissionState()
        data object Denied : PermissionState()
        data object ShowRationale : PermissionState()
    }

    fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                _permissionState.value = PermissionState.Granted
            }
            else -> {
                _permissionState.value = PermissionState.Denied
            }
        }
    }
}