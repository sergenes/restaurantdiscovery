package com.nes.lunchtime.domain

import com.google.android.gms.maps.model.LatLng
import com.nes.lunchtime.repo.RestaurantsRepository
import javax.inject.Inject

class GetRestaurantsUseCase @Inject constructor(
    private val repository: RestaurantsRepository
) {
    suspend fun getNearby(location: LatLng): Result<List<Restaurant>> {
        return repository.getNearByRestaurants(location).map { restaurants ->
            restaurants.sortedBy { it.distanceInMeters }
        }
    }

    suspend fun search(query: String, location: LatLng): Result<List<Restaurant>> {
        return repository.getRestaurantsByText(query, location).map { restaurants ->
            restaurants.sortedBy { it.distanceInMeters }
        }
    }
}
