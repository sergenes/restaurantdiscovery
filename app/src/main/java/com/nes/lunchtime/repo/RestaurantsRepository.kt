package com.nes.lunchtime.repo

import android.util.Log
import com.nes.lunchtime.domain.PlaceDetails
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.net.GooglePlacesClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

interface RestaurantsRepository {
    suspend fun getNearByRestaurants(
        location: LatLng,
        radius: Int = 5000 //5Km
    ): Result<List<Restaurant>>

    suspend fun getPlaceDetails(
        id: String
    ): Result<PlaceDetails>

    suspend fun getRestaurantsByText(
        searchText: String,
        location: LatLng,
        radius: Int = 5000 //5Km
    ): Result<List<Restaurant>>
}

class RestaurantsRepositoryImpl @Inject constructor(
    private val client: GooglePlacesClient
) : RestaurantsRepository {
    override suspend fun getNearByRestaurants(
        location: LatLng,
        radius: Int
    ): Result<List<Restaurant>> = safeApiCall("Error fetching nearby restaurants") {
        client.getNearbyRestaurants(location.latitude, location.longitude, radius = radius)
    }

    override suspend fun getPlaceDetails(id: String): Result<PlaceDetails> =
        safeApiCall("Error fetching place details") {
            client.getPlaceDetails(id)
        }

    override suspend fun getRestaurantsByText(
        searchText: String,
        location: LatLng,
        radius: Int
    ): Result<List<Restaurant>> = safeApiCall("Error fetching restaurants by text") {
        client.getRestaurantsByText(
            searchText,
            location.latitude,
            location.longitude,
            radius = radius
        )
    }

    private suspend fun <T> safeApiCall(errorMessage: String, call: suspend () -> T): Result<T> {
        return try {
            Result.success(call())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("RestaurantsRepository", errorMessage, e)
            Result.failure(e)
        }
    }
}
