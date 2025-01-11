package com.nes.lunchtime.net.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NearbySearchRequest(
    @SerialName("maxResultCount") val maxResultCount: Int,
    @SerialName("includedTypes") val includedTypes: List<String>,
    @SerialName("locationRestriction") val locationRestriction: LocationRestriction
)

@Serializable
data class TextSearchRequest(
    @SerialName("textQuery") val textQuery: String,
    @SerialName("pageSize") val pageSize: Int,
    @SerialName("locationBias") val locationBias: LocationRestriction
)

@Serializable
data class LocationRestriction(
    @SerialName("circle") val circle: Circle
)

@Serializable
data class Circle(
    @SerialName("center") val center: Center,
    @SerialName("radius") val radius: Int
)

@Serializable
data class Center(
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double
)