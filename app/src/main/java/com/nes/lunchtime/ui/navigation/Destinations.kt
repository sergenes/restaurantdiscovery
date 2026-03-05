package com.nes.lunchtime.ui.navigation

import com.nes.lunchtime.domain.Restaurant
import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data class Details(
    val restaurantId: String,
    val displayName: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val userRatingCount: Int,
    val photoUrl: String? = null,
    val distanceInMeters: Float
) {
    companion object {
        fun fromRestaurant(restaurant: Restaurant): Details {
            return Details(
                restaurantId = restaurant.id,
                displayName = restaurant.displayName,
                formattedAddress = restaurant.formattedAddress,
                latitude = restaurant.latitude,
                longitude = restaurant.longitude,
                rating = restaurant.rating,
                userRatingCount = restaurant.userRatingCount,
                photoUrl = restaurant.photoUrl,
                distanceInMeters = restaurant.distanceInMeters
            )
        }
    }
    
    fun toRestaurant(): Restaurant {
        return Restaurant(
            id = restaurantId,
            displayName = displayName,
            formattedAddress = formattedAddress,
            latitude = latitude,
            longitude = longitude,
            rating = rating,
            userRatingCount = userRatingCount,
            photoUrl = photoUrl,
            distanceInMeters = distanceInMeters
        )
    }
}
