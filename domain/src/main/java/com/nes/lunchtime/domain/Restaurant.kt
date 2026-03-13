package com.nes.lunchtime.domain

data class Restaurant(
    val id: String = "",
    val displayName: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double = 0.0,
    val userRatingCount: Int = 0,
    val photoUrl: String? = null,
    val distanceInMeters: Float = 0.0f
)

data class Review(val text: String, val languageCode: String)

data class PlaceDetails(
    val id: String,
    val displayName: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val userRatingCount: Int,
    val reviews: List<Review>
)
