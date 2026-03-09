package com.nes.lunchtime.location


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class LocationRepository @Inject constructor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {
    sealed class LocationResult {
        data class Success(val location: LatLng) : LocationResult()
        data class Error(val exception: Exception) : LocationResult()
    }

    /**
     * Emits the current location immediately (from last known fix), then continues
     * emitting whenever the device moves at least [minDistanceM] metres or after
     * [intervalMs] milliseconds, whichever comes first.
     *
     * The flow completes (and unregisters the callback) when the collector is cancelled.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(
        intervalMs: Long = 30_000L,
        minDistanceM: Float = 100f
    ): Flow<LocationResult> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(LocationResult.Error(SecurityException("Location permission not granted")))
            close()
            return@callbackFlow
        }

        // Request a fresh GPS fix first so the initial emission reflects the current position
        // rather than the stale OS-level lastLocation cache (which can survive app restarts
        // and persist a previous simulated/real position indefinitely).
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                location?.let { trySend(LocationResult.Success(it.toLatLng())) }
            }

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(minDistanceM)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(gmsResult: com.google.android.gms.location.LocationResult) {
                gmsResult.lastLocation?.let { trySend(LocationResult.Success(it.toLatLng())) }
            }
        }

        fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener { e ->
                trySend(LocationResult.Error(e))
                close(e)
            }

        awaitClose { fusedLocationClient.removeLocationUpdates(callback) }
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
