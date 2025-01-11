package com.nes.lunchtime.location

import android.location.Location

object LocationUtils {
    fun calculateDistance(
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double,
        toLongitude: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            fromLatitude,
            fromLongitude,
            toLatitude,
            toLongitude,
            results
        )
        return results[0] // Distance in meters
    }
}