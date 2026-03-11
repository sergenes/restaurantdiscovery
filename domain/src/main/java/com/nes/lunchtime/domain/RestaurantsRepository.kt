package com.nes.lunchtime.domain

import com.google.android.gms.maps.model.LatLng

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
