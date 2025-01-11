package com.nes.lunchtime.net


import com.nes.lunchtime.domain.PlaceDetails
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.location.LocationUtils
import com.nes.lunchtime.net.model.Center
import com.nes.lunchtime.net.model.Circle
import com.nes.lunchtime.net.model.LocationRestriction
import com.nes.lunchtime.net.model.NearbySearchRequest
import com.nes.lunchtime.net.model.NearbySearchResponse
import com.nes.lunchtime.net.model.Photo
import com.nes.lunchtime.net.model.PlaceDetailsResponse
import com.nes.lunchtime.net.model.TextSearchRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.path
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Named


@Singleton
class GooglePlacesClient @Inject constructor(
    private val client: HttpClient,
    @Named("google_places_api_key") private val apiKey: String
) {
    companion object {
        private const val BASE_URL = "https://places.googleapis.com"
    }

    /**
     * Retrieves nearby restaurants based on the provided location.
     * https://developers.google.com/maps/documentation/places/web-service/nearby-search
     * https://developers.google.com/maps/documentation/places/web-service/place-photos
     *
     * @param latitude The latitude of the location.
     * @param longitude The longitude of the location.
     * @param maxResultCount The maximum number of results to return.
     * @param radius The radius in meters.
     * @return A list of [Restaurant] objects.
     */
    suspend fun getNearbyRestaurants(
        latitude: Double,
        longitude: Double,
        maxResultCount: Int = 10,
        radius: Int = 5000
    ): List<Restaurant> {
        val url = URLBuilder(BASE_URL).apply {
            protocol = URLProtocol.HTTPS
            path("/v1/places:searchNearby")
        }.build()
        val response: NearbySearchResponse = client.post(url) {
            header("X-Goog-Api-Key", apiKey)
            header(
                "X-Goog-FieldMask",
                "places.id,places.displayName,places.formattedAddress," +
                        "places.location,places.rating,places.userRatingCount,places.photos"
            )
            setBody(
                NearbySearchRequest(
                    maxResultCount = maxResultCount,
                    includedTypes = listOf("restaurant"),
                    locationRestriction = LocationRestriction(
                        circle = Circle(
                            center = Center(latitude, longitude),
                            radius = radius
                        )
                    )
                )
            )
        }.body()
        return response.places.map {
            Restaurant(
                id = it.id,
                displayName = it.displayName.text,
                formattedAddress = it.formattedAddress,
                latitude = it.location.latitude,
                longitude = it.location.longitude,
                rating = it.rating ?: 0.0,
                userRatingCount = it.userRatingCount ?: 0,
                photoUrl = buildPhotoUrl(it.photos),
                distanceInMeters = LocationUtils.calculateDistance(
                    latitude,
                    longitude,
                    it.location.latitude,
                    it.location.longitude
                )
            )
        }
    }

    private fun buildPhotoUrl(photos: List<Photo>?): String? {
        photos?.first()?.let {
            return "$BASE_URL/v1/${it.name}/media?maxHeightPx=400&maxWidthPx=400&key=$apiKey"
        }
        return null
    }

    /**
     * Retrieves details for a specific place.
     * https://developers.google.com/maps/documentation/places/web-service/place-details
     *
     * @param placeId The ID of the place.
     * @return A [PlaceDetails] object.
     */
    suspend fun getPlaceDetails(placeId: String): PlaceDetails {
        val url = URLBuilder(BASE_URL).apply {
            protocol = URLProtocol.HTTPS
            path("/v1/places/$placeId")
        }.build()

        val response: PlaceDetailsResponse = client.get(url) {
            header("X-Goog-Api-Key", apiKey)
            header(
                "X-Goog-FieldMask", "id,displayName,formattedAddress,location,rating," +
                        "userRatingCount,reviews"
            )
        }.body()

        return PlaceDetails(
            id = response.id,
            displayName = response.displayName.text,
            formattedAddress = response.formattedAddress,
            latitude = response.location.latitude,
            longitude = response.location.longitude,
            rating = response.rating ?: 0.0,
            userRatingCount = response.userRatingCount ?: 0,
            reviews = response.reviews ?: emptyList()
        )
    }

    /**
     * Retrieves restaurants based on the text.
     * https://developers.google.com/maps/documentation/places/web-service/text-search
     *
     * @param searchText The ID of the place.
     * @param latitude The latitude of the location.
     * @param longitude The longitude of the location.
     * @param maxResultCount The maximum number of results to return.
     * @param radius The radius in meters.
     * @return A list of [Restaurant] objects.
     */
    suspend fun getRestaurantsByText(
        searchText: String,
        latitude: Double,
        longitude: Double,
        maxResultCount: Int = 10,
        radius: Int = 5000
    ): List<Restaurant> {
        val url = URLBuilder(BASE_URL).apply {
            protocol = URLProtocol.HTTPS
            path("/v1/places:searchText")
        }.build()
        val response: NearbySearchResponse = client.post(url) {
            header("X-Goog-Api-Key", apiKey)
            header(
                "X-Goog-FieldMask",
                "places.id,places.displayName,places.formattedAddress," +
                        "places.location,places.rating,places.userRatingCount,places.photos"
            )
            setBody(
                TextSearchRequest(
                    textQuery = searchText,
                    pageSize = maxResultCount,
                    locationBias = LocationRestriction(
                        circle = Circle(
                            center = Center(latitude, longitude),
                            radius = radius
                        )
                    )
                )
            )
        }.body()
        return response.places.map {
            Restaurant(
                id = it.id,
                displayName = it.displayName.text,
                formattedAddress = it.formattedAddress,
                latitude = it.location.latitude,
                longitude = it.location.longitude,
                rating = it.rating ?: 0.0,
                userRatingCount = it.userRatingCount ?: 0,
                photoUrl = buildPhotoUrl(it.photos),
                distanceInMeters = LocationUtils.calculateDistance(
                    latitude,
                    longitude,
                    it.location.latitude,
                    it.location.longitude
                )
            )
        }
    }
}