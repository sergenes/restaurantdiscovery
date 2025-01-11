package com.nes.lunchtime.net.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbySearchResponse(
    @SerialName("places") val places: List<Place>
)

@Serializable
data class Place(
    @SerialName("id") val id: String,
    @SerialName("displayName") val displayName: DisplayName,
    @SerialName("formattedAddress") val formattedAddress: String,
    @SerialName("location") val location: Location,
    @SerialName("rating") val rating: Double? = null,
    @SerialName("userRatingCount") val userRatingCount: Int? = null,
    @SerialName("photos") val photos: List<Photo>? = null
)

@Serializable
data class DisplayName(
    @SerialName("text") val text: String
)

@Serializable
data class Location(
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double
)

@Serializable
data class ReviewDetails(
    @SerialName("text") val text: ReviewText
)

@Serializable
data class ReviewText(
    @SerialName("text") val text: String,
    @SerialName("languageCode") val languageCode: String
)

@Serializable
data class PlaceDetailsResponse(
    @SerialName("id") val id: String,
    @SerialName("displayName") val displayName: DisplayName,
    @SerialName("formattedAddress") val formattedAddress: String,
    @SerialName("location") val location: Location,
    @SerialName("rating") val rating: Double? = null,
    @SerialName("userRatingCount") val userRatingCount: Int? = null,
    @SerialName("reviews") val reviews: List<ReviewDetails>? = null,
    @SerialName("photos") val photos: List<Photo>? = null
)

@Serializable
data class Photo(
    @SerialName("name") val name: String
)
