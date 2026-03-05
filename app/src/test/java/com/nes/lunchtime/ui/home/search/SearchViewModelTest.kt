package com.nes.lunchtime.ui.home.search

import com.google.android.gms.maps.model.LatLng
import com.nes.lunchtime.MainCoroutineRule
import com.nes.lunchtime.domain.GetRestaurantsUseCase
import com.nes.lunchtime.domain.Restaurant
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var getRestaurantsUseCase: GetRestaurantsUseCase
    private lateinit var viewModel: SearchViewModel

    private val testLocation = LatLng(37.7749, -122.4194)
    private val testRestaurants = listOf(
        Restaurant(
            id = "1",
            displayName = "Pizza Place",
            formattedAddress = "123 Main St",
            latitude = 37.7749,
            longitude = -122.4194,
            rating = 4.5,
            userRatingCount = 100,
            photoUrl = ""
        )
    )

    @Before
    fun setup() {
        getRestaurantsUseCase = mockk()
        viewModel = SearchViewModel(getRestaurantsUseCase)
        viewModel.setLocation(testLocation)
    }

    @Test
    fun `initial state is Initial`() = runTest {
        assertEquals(SearchViewModel.UiState.Initial, viewModel.uiState.first())
    }

    @Test
    fun `debounce waits 500ms before searching`() = runTest {
        // Given: UseCase returns success
        coEvery {
            getRestaurantsUseCase.search("pizza", testLocation)
        } returns Result.success(testRestaurants)

        // When: User types query
        viewModel.onSearchQueryChanged("pizza")

        // Then: Search is not triggered immediately
        advanceTimeBy(300)
        coVerify(exactly = 0) { getRestaurantsUseCase.search(any(), any()) }

        // When: 500ms have passed
        advanceTimeBy(300) // Total 600ms
        advanceUntilIdle()

        // Then: Search is triggered
        coVerify(exactly = 1) { getRestaurantsUseCase.search("pizza", testLocation) }
    }

    @Test
    fun `debounce cancels previous search when user keeps typing`() = runTest {
        // Given: UseCase returns success
        coEvery {
            getRestaurantsUseCase.search(any(), any())
        } returns Result.success(testRestaurants)

        // When: User types multiple characters quickly
        viewModel.onSearchQueryChanged("p")
        advanceTimeBy(200)
        viewModel.onSearchQueryChanged("pi")
        advanceTimeBy(200)
        viewModel.onSearchQueryChanged("piz")
        advanceTimeBy(200)
        viewModel.onSearchQueryChanged("pizz")
        advanceTimeBy(200)
        viewModel.onSearchQueryChanged("pizza")

        // Then: No search yet (debounce resets each time)
        coVerify(exactly = 0) { getRestaurantsUseCase.search(any(), any()) }

        // When: Wait for debounce to complete
        advanceTimeBy(500)
        advanceUntilIdle()

        // Then: Only the final search is triggered
        coVerify(exactly = 1) { getRestaurantsUseCase.search("pizza", testLocation) }
    }

    @Test
    fun `blank query resets to Initial state`() = runTest {
        // Given: ViewModel in Success state
        coEvery {
            getRestaurantsUseCase.search("pizza", testLocation)
        } returns Result.success(testRestaurants)

        viewModel.onSearchQueryChanged("pizza")
        advanceTimeBy(500)
        advanceUntilIdle()

        // When: User clears search query
        viewModel.onSearchQueryChanged("")

        // Then: State is reset to Initial
        assertEquals(SearchViewModel.UiState.Initial, viewModel.uiState.value)
    }

    @Test
    fun `does not search for blank queries`() = runTest {
        // When: User enters blank query
        viewModel.onSearchQueryChanged("   ")
        advanceTimeBy(500)
        advanceUntilIdle()

        // Then: No search is triggered
        coVerify(exactly = 0) { getRestaurantsUseCase.search(any(), any()) }
    }

    @Test
    fun `manual search triggers immediately without debounce`() = runTest {
        // Given: UseCase returns success
        coEvery {
            getRestaurantsUseCase.search("burger", testLocation)
        } returns Result.success(testRestaurants)

        // When: User manually triggers search
        viewModel.getRestaurantsByText("burger", testLocation)
        advanceUntilIdle()

        // Then: Search is triggered immediately
        coVerify(exactly = 1) { getRestaurantsUseCase.search("burger", testLocation) }
    }

    @Test
    fun `retry uses last search parameters`() = runTest {
        // Given: Initial search fails
        coEvery {
            getRestaurantsUseCase.search("pizza", testLocation)
        } returns Result.failure(Exception("Network error"))

        viewModel.getRestaurantsByText("pizza", testLocation)
        advanceUntilIdle()

        // When: UseCase now succeeds and user retries
        coEvery {
            getRestaurantsUseCase.search("pizza", testLocation)
        } returns Result.success(testRestaurants)

        viewModel.retry()
        advanceUntilIdle()

        // Then: Search is triggered again with same parameters
        coVerify(exactly = 2) { getRestaurantsUseCase.search("pizza", testLocation) }
    }

    @Test
    fun `success state contains restaurant results`() = runTest {
        // Given: UseCase returns restaurants
        coEvery {
            getRestaurantsUseCase.search("pizza", testLocation)
        } returns Result.success(testRestaurants)

        // When: Search is triggered
        viewModel.getRestaurantsByText("pizza", testLocation)
        advanceUntilIdle()

        // Then: State is Success with results
        val state = viewModel.uiState.value
        assertTrue(state is SearchViewModel.UiState.Success)
        assertEquals(testRestaurants, state.restaurants)
    }

    @Test
    fun `error state contains error message`() = runTest {
        // Given: UseCase returns error
        coEvery {
            getRestaurantsUseCase.search("pizza", testLocation)
        } returns Result.failure(Exception("API error"))

        // When: Search is triggered
        viewModel.getRestaurantsByText("pizza", testLocation)
        advanceUntilIdle()

        // Then: State is Error with message
        val state = viewModel.uiState.value
        assertTrue(state is SearchViewModel.UiState.Error)
        assertEquals("API error", state.message)
    }

    @Test
    fun `distinctUntilChanged prevents duplicate searches`() = runTest {
        // Given: UseCase returns success
        coEvery {
            getRestaurantsUseCase.search("pizza", testLocation)
        } returns Result.success(testRestaurants)

        // When: User types same query twice
        viewModel.onSearchQueryChanged("pizza")
        advanceTimeBy(500)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("pizza")
        advanceTimeBy(500)
        advanceUntilIdle()

        // Then: Search is only triggered once
        coVerify(exactly = 1) { getRestaurantsUseCase.search("pizza", testLocation) }
    }
}
