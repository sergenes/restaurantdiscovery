package com.nes.lunchtime.domain

import com.google.android.gms.maps.model.LatLng
import com.nes.lunchtime.repo.RestaurantsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetRestaurantsUseCaseTest {
    private lateinit var repository: RestaurantsRepository
    private lateinit var useCase: GetRestaurantsUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = GetRestaurantsUseCase(repository)
    }

    @Test
    fun `getNearby sorts restaurants by distance ascending`() = runTest {
        // Given: Unsorted restaurants with different distances
        val location = LatLng(37.7749, -122.4194)
        val restaurants = listOf(
            createRestaurant(id = "1", distance = 500f),
            createRestaurant(id = "2", distance = 100f),
            createRestaurant(id = "3", distance = 300f)
        )
        coEvery { repository.getNearByRestaurants(location) } returns Result.success(restaurants)

        // When: Getting nearby restaurants
        val result = useCase.getNearby(location)

        // Then: Restaurants are sorted by distance
        assertTrue(result.isSuccess)
        val sorted = result.getOrNull()!!
        assertEquals("2", sorted[0].id) // 100m
        assertEquals("3", sorted[1].id) // 300m
        assertEquals("1", sorted[2].id) // 500m
    }

    @Test
    fun `getNearby handles empty list`() = runTest {
        // Given: Empty restaurant list
        val location = LatLng(37.7749, -122.4194)
        coEvery { repository.getNearByRestaurants(location) } returns Result.success(emptyList())

        // When: Getting nearby restaurants
        val result = useCase.getNearby(location)

        // Then: Returns empty list successfully
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `getNearby propagates repository errors`() = runTest {
        // Given: Repository returns error
        val location = LatLng(37.7749, -122.4194)
        val exception = Exception("Network error")
        coEvery { repository.getNearByRestaurants(location) } returns Result.failure(exception)

        // When: Getting nearby restaurants
        val result = useCase.getNearby(location)

        // Then: Error is propagated
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `search sorts restaurants by distance ascending`() = runTest {
        // Given: Unsorted search results
        val location = LatLng(37.7749, -122.4194)
        val restaurants = listOf(
            createRestaurant(id = "1", distance = 800f),
            createRestaurant(id = "2", distance = 200f),
            createRestaurant(id = "3", distance = 500f)
        )
        coEvery {
            repository.getRestaurantsByText("pizza", location)
        } returns Result.success(restaurants)

        // When: Searching restaurants
        val result = useCase.search("pizza", location)

        // Then: Results are sorted by distance
        assertTrue(result.isSuccess)
        val sorted = result.getOrNull()!!
        assertEquals("2", sorted[0].id) // 200m
        assertEquals("3", sorted[1].id) // 500m
        assertEquals("1", sorted[2].id) // 800m
    }

    @Test
    fun `search handles empty results`() = runTest {
        // Given: No search results
        val location = LatLng(37.7749, -122.4194)
        coEvery {
            repository.getRestaurantsByText("nonexistent", location)
        } returns Result.success(emptyList())

        // When: Searching
        val result = useCase.search("nonexistent", location)

        // Then: Returns empty list successfully
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `search propagates repository errors`() = runTest {
        // Given: Repository returns error
        val location = LatLng(37.7749, -122.4194)
        val exception = Exception("API error")
        coEvery {
            repository.getRestaurantsByText("pizza", location)
        } returns Result.failure(exception)

        // When: Searching
        val result = useCase.search("pizza", location)

        // Then: Error is propagated
        assertTrue(result.isFailure)
        assertEquals("API error", result.exceptionOrNull()?.message)
    }

    private fun createRestaurant(
        id: String,
        distance: Float
    ): Restaurant {
        return Restaurant(
            id = id,
            displayName = "Restaurant $id",
            formattedAddress = "Address $id",
            latitude = 0.0,
            longitude = 0.0,
            rating = 4.0,
            userRatingCount = 100,
            photoUrl = "",
            distanceInMeters = distance
        )
    }
}
