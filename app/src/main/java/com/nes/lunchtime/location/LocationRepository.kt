package com.nes.lunchtime.location

import kotlinx.coroutines.CancellableContinuation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class LocationRepository @Inject constructor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {
    sealed class LocationResult {
        data class Success(val location: LatLng) : LocationResult()
        data class Error(val exception: Exception) : LocationResult()
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): LocationResult = suspendCancellableCoroutine { continuation ->
        try {
            if (!hasLocationPermission()) {
                continuation.resume(LocationResult.Error(SecurityException("Location permission not granted")))
                return@suspendCancellableCoroutine
            }

            // First try getting last location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        continuation.resume(LocationResult.Success(location.toLatLng()))
                    } else {
                        // If last location is null, request a fresh location
                        requestFreshLocation(continuation)
                    }
                }
                .addOnFailureListener { exception ->
                    continuation.resume(LocationResult.Error(exception))
                }

            continuation.invokeOnCancellation {
                // Clean up any resources if needed
            }
        } catch (e: Exception) {
            continuation.resume(LocationResult.Error(e))
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestFreshLocation(continuation: CancellableContinuation<LocationResult>) {
        if (!hasLocationPermission()) {
            continuation.resume(LocationResult.Error(SecurityException("Location permission not granted")))
            return
        }
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        continuation.resume(LocationResult.Success(it.toLatLng()))
                    } ?: continuation.resume(
                        LocationResult.Error(Exception("Unable to get current location"))
                    )
                }
                .addOnFailureListener { exception ->
                    continuation.resume(LocationResult.Error(exception))
                }
        } catch (e: Exception) {
            continuation.resume(LocationResult.Error(e))
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun Location.toLatLng(): LatLng {
        return LatLng(latitude, longitude)
    }
}