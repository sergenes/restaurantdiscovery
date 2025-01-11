package com.nes.lunchtime.repo

import android.util.Log
import com.nes.lunchtime.domain.PlaceDetails
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.net.GooglePlacesClient
import com.google.android.gms.maps.model.LatLng
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
    ): Result<List<Restaurant>> {
        return runCatching {
            client.getNearbyRestaurants(location.latitude, location.longitude, radius = radius)
        }.onFailure { error ->
            Log.e("RestaurantsRepository", "Error fetching nearby restaurants", error)
        }
    }

    override suspend fun getPlaceDetails(id: String): Result<PlaceDetails> {
        return runCatching {
            client.getPlaceDetails(id)
        }.onFailure { error ->
            Log.e("RestaurantsRepository", "Error fetching place details", error)
        }
    }

    override suspend fun getRestaurantsByText(
        searchText: String,
        location: LatLng,
        radius: Int
    ): Result<List<Restaurant>> {
        return runCatching {
            client.getRestaurantsByText(
                searchText,
                location.latitude,
                location.longitude,
                radius = radius
            )
        }.onFailure { error ->
            Log.e("RestaurantsRepository", "Error fetching restaurants by text", error)
        }
    }
}