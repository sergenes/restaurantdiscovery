package com.nes.lunchtime.ui.navigation

import com.nes.lunchtime.domain.Restaurant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for navigation destinations and Restaurant serialization.
 *
 * These tests verify that Restaurant data is correctly converted to/from
 * navigation parameters without data loss. This is critical because:
 * - Data loss would cause missing info on detail screens
 * - Serialization errors would crash the app during navigation
 * - Edge cases (null, special chars) need to be handled
 */
class DestinationsTest {

    @Test
    fun `Home destination exists`() {
        // Verifies Home route is defined
        assertNotNull(Home)
    }

    @Test
    fun `Details converts from Restaurant preserving all fields`() {
        // Given: A restaurant with all fields populated
        val restaurant = Restaurant(
            id = "test_id",
            displayName = "Test Restaurant",
            formattedAddress = "123 Test St",
            latitude = 37.7749,
            longitude = -122.4194,
            rating = 4.5,
            userRatingCount = 100,
            photoUrl = "https://example.com/photo.jpg",
            distanceInMeters = 500f
        )

        // When: Converting to Details destination
        val details = Details.fromRestaurant(restaurant)

        // Then: All fields are preserved
        assertEquals(restaurant.id, details.restaurantId)
        assertEquals(restaurant.displayName, details.displayName)
        assertEquals(restaurant.formattedAddress, details.formattedAddress)
        assertEquals(restaurant.latitude, details.latitude)
        assertEquals(restaurant.longitude, details.longitude)
        assertEquals(restaurant.rating, details.rating)
        assertEquals(restaurant.userRatingCount, details.userRatingCount)
        assertEquals(restaurant.photoUrl, details.photoUrl)
        assertEquals(restaurant.distanceInMeters, details.distanceInMeters)
    }

    @Test
    fun `Details converts to Restaurant preserving all fields`() {
        // Given: A Details destination
        val details = Details(
            restaurantId = "test_id",
            displayName = "Test Restaurant",
            formattedAddress = "123 Test St",
            latitude = 37.7749,
            longitude = -122.4194,
            rating = 4.5,
            userRatingCount = 100,
            photoUrl = "https://example.com/photo.jpg",
            distanceInMeters = 500f
        )

        // When: Converting to Restaurant
        val restaurant = details.toRestaurant()

        // Then: All fields are preserved
        assertEquals(details.restaurantId, restaurant.id)
        assertEquals(details.displayName, restaurant.displayName)
        assertEquals(details.formattedAddress, restaurant.formattedAddress)
        assertEquals(details.latitude, restaurant.latitude)
        assertEquals(details.longitude, restaurant.longitude)
        assertEquals(details.rating, restaurant.rating)
        assertEquals(details.userRatingCount, restaurant.userRatingCount)
        assertEquals(details.photoUrl, restaurant.photoUrl)
        assertEquals(details.distanceInMeters, restaurant.distanceInMeters)
    }

    @Test
    fun `round trip conversion preserves all data`() {
        // Given: A restaurant
        val original = Restaurant(
            id = "test_id",
            displayName = "Test Restaurant",
            formattedAddress = "123 Test St",
            latitude = 37.7749,
            longitude = -122.4194,
            rating = 4.5,
            userRatingCount = 100,
            photoUrl = "https://example.com/photo.jpg",
            distanceInMeters = 500f
        )

        // When: Converting to Details and back
        val details = Details.fromRestaurant(original)
        val converted = details.toRestaurant()

        // Then: All data is preserved
        assertEquals(original.id, converted.id)
        assertEquals(original.displayName, converted.displayName)
        assertEquals(original.formattedAddress, converted.formattedAddress)
        assertEquals(original.latitude, converted.latitude, 0.0001)
        assertEquals(original.longitude, converted.longitude, 0.0001)
        assertEquals(original.rating, converted.rating, 0.001)
        assertEquals(original.userRatingCount, converted.userRatingCount)
        assertEquals(original.photoUrl, converted.photoUrl)
        assertEquals(original.distanceInMeters, converted.distanceInMeters, 0.01f)
    }

    @Test
    fun `handles null photoUrl correctly`() {
        // Given: Restaurant with null photo
        val restaurant = Restaurant(
            id = "test_id",
            displayName = "Test",
            formattedAddress = "Address",
            latitude = 37.7749,
            longitude = -122.4194,
            rating = 4.5,
            userRatingCount = 100,
            photoUrl = null,
            distanceInMeters = 500f
        )

        // When: Converting to Details and back
        val details = Details.fromRestaurant(restaurant)
        val converted = details.toRestaurant()

        // Then: Null is preserved
        assertEquals(null, details.photoUrl)
        assertEquals(null, converted.photoUrl)
    }

    @Test
    fun `handles empty photoUrl correctly`() {
        // Given: Restaurant with empty photo URL
        val restaurant = Restaurant(
            id = "test_id",
            displayName = "Test",
            formattedAddress = "Address",
            latitude = 37.7749,
            longitude = -122.4194,
            rating = 4.5,
            userRatingCount = 100,
            photoUrl = "",
            distanceInMeters = 500f
        )

        // When: Converting to Details and back
        val details = Details.fromRestaurant(restaurant)
        val converted = details.toRestaurant()

        // Then: Empty string is preserved
        assertEquals("", details.photoUrl)
        assertEquals("", converted.photoUrl)
    }

    @Test
    fun `handles special characters in text fields`() {
        // Given: Restaurant with special characters
        val restaurant = Restaurant(
            id = "test_id",
            displayName = "Café & Restaurant's \"Best\" Place",
            formattedAddress = "123 Main St, Apt #5, San Francisco, CA 94102",
            latitude = 37.7749,
            longitude = -122.4194,
            rating = 4.5,
            userRatingCount = 100,
            photoUrl = "https://example.com/photo?id=123&size=large",
            distanceInMeters = 500f
        )

        // When: Converting to Details and back
        val details = Details.fromRestaurant(restaurant)
        val converted = details.toRestaurant()

        // Then: Special characters are preserved
        assertEquals(restaurant.displayName, converted.displayName)
        assertEquals(restaurant.formattedAddress, converted.formattedAddress)
        assertEquals(restaurant.photoUrl, converted.photoUrl)
    }

    @Test
    fun `handles extreme coordinate values`() {
        // Given: Restaurant at extreme coordinates
        val restaurant = Restaurant(
            id = "test_id",
            displayName = "Test",
            formattedAddress = "Address",
            latitude = 90.0, // Max latitude
            longitude = 180.0, // Max longitude
            rating = 5.0,
            userRatingCount = 100,
            photoUrl = "",
            distanceInMeters = 500f
        )

        // When: Converting to Details and back
        val details = Details.fromRestaurant(restaurant)
        val converted = details.toRestaurant()

        // Then: Extreme values are preserved
        assertEquals(90.0, converted.latitude, 0.0001)
        assertEquals(180.0, converted.longitude, 0.0001)
    }

    @Test
    fun `handles zero values`() {
        // Given: Restaurant with zero values
        val restaurant = Restaurant(
            id = "test_id",
            displayName = "Test",
            formattedAddress = "Address",
            latitude = 0.0,
            longitude = 0.0,
            rating = 0.0,
            userRatingCount = 0,
            photoUrl = "",
            distanceInMeters = 0f
        )

        // When: Converting to Details and back
        val details = Details.fromRestaurant(restaurant)
        val converted = details.toRestaurant()

        // Then: Zero values are preserved
        assertEquals(0.0, converted.latitude, 0.0001)
        assertEquals(0.0, converted.longitude, 0.0001)
        assertEquals(0.0, converted.rating, 0.001)
        assertEquals(0, converted.userRatingCount)
        assertEquals(0f, converted.distanceInMeters, 0.01f)
    }

    @Test
    fun `handles negative coordinates`() {
        // Given: Restaurant with negative coordinates (valid for southern/western hemispheres)
        val restaurant = Restaurant(
            id = "test_id",
            displayName = "Test",
            formattedAddress = "Address",
            latitude = -33.8688,
            longitude = -151.2093,
            rating = 4.5,
            userRatingCount = 100,
            photoUrl = "",
            distanceInMeters = 500f
        )

        // When: Converting to Details and back
        val details = Details.fromRestaurant(restaurant)
        val converted = details.toRestaurant()

        // Then: Negative coordinates are preserved
        assertEquals(-33.8688, converted.latitude, 0.0001)
        assertEquals(-151.2093, converted.longitude, 0.0001)
    }

    @Test
    fun `handles large userRatingCount`() {
        // Given: Restaurant with large rating count
        val restaurant = Restaurant(
            id = "test_id",
            displayName = "Test",
            formattedAddress = "Address",
            latitude = 37.7749,
            longitude = -122.4194,
            rating = 4.5,
            userRatingCount = Int.MAX_VALUE,
            photoUrl = "",
            distanceInMeters = 500f
        )

        // When: Converting to Details and back
        val details = Details.fromRestaurant(restaurant)
        val converted = details.toRestaurant()

        // Then: Large value is preserved
        assertEquals(Int.MAX_VALUE, converted.userRatingCount)
    }

    @Test
    fun `handles large distance`() {
        // Given: Restaurant with large distance
        val restaurant = Restaurant(
            id = "test_id",
            displayName = "Test",
            formattedAddress = "Address",
            latitude = 37.7749,
            longitude = -122.4194,
            rating = 4.5,
            userRatingCount = 100,
            photoUrl = "",
            distanceInMeters = 999999.99f
        )

        // When: Converting to Details and back
        val details = Details.fromRestaurant(restaurant)
        val converted = details.toRestaurant()

        // Then: Large distance is preserved
        assertEquals(999999.99f, converted.distanceInMeters, 0.01f)
    }
}
