package com.nes.lunchtime.ui.home.nearby

import com.google.android.gms.maps.model.LatLng
import com.nes.lunchtime.MainCoroutineRule
import com.nes.lunchtime.domain.GetRestaurantsUseCase
import com.nes.lunchtime.domain.Restaurant
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NearByViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var getRestaurantsUseCase: GetRestaurantsUseCase
    private lateinit var viewModel: NearByViewModel

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
        viewModel = NearByViewModel(getRestaurantsUseCase)
    }

    @Test
    fun `initial state is Loading`() = runTest {
        assertEquals(NearByViewModel.UiState.Loading, viewModel.uiState.first())
    }

    @Test
    fun `setLocation auto-triggers load and emits Success`() = runTest {
        coEvery { getRestaurantsUseCase.getNearby(testLocation) } returns Result.success(testRestaurants)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setLocation(testLocation)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is NearByViewModel.UiState.Success)
        assertEquals(testRestaurants, state.restaurants)
    }

    @Test
    fun `setLocation auto-triggers load and emits Error on failure`() = runTest {
        coEvery { getRestaurantsUseCase.getNearby(testLocation) } returns Result.failure(Exception("API error"))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setLocation(testLocation)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is NearByViewModel.UiState.Error)
        assertEquals("API error", state.message)
        assertTrue(state.canRetry)
    }

    @Test
    fun `same location set twice does not trigger a second load`() = runTest {
        coEvery { getRestaurantsUseCase.getNearby(testLocation) } returns Result.success(testRestaurants)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setLocation(testLocation)
        advanceUntilIdle()
        viewModel.setLocation(testLocation)  // identical value — StateFlow deduplicates
        advanceUntilIdle()

        coVerify(exactly = 1) { getRestaurantsUseCase.getNearby(testLocation) }
    }

    @Test
    fun `location change cancels in-flight request and reloads for new location`() = runTest {
        val newLocation = LatLng(40.7128, -74.0060)
        coEvery { getRestaurantsUseCase.getNearby(testLocation) } returns Result.success(testRestaurants)
        coEvery { getRestaurantsUseCase.getNearby(newLocation) } returns Result.success(testRestaurants)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setLocation(testLocation)
        advanceUntilIdle()

        viewModel.setLocation(newLocation)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is NearByViewModel.UiState.Success)
        coVerify(exactly = 1) { getRestaurantsUseCase.getNearby(newLocation) }
    }

    @Test
    fun `retry retriggers fetch with current location`() = runTest {
        coEvery { getRestaurantsUseCase.getNearby(testLocation) } returns Result.failure(Exception("Network error"))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setLocation(testLocation)
        advanceUntilIdle()

        coEvery { getRestaurantsUseCase.getNearby(testLocation) } returns Result.success(testRestaurants)

        viewModel.retry()
        advanceUntilIdle()

        coVerify(exactly = 2) { getRestaurantsUseCase.getNearby(testLocation) }
        assertTrue(viewModel.uiState.value is NearByViewModel.UiState.Success)
    }

    @Test
    fun `retry without prior location does nothing`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.retry()
        advanceUntilIdle()

        coVerify(exactly = 0) { getRestaurantsUseCase.getNearby(any()) }
        assertEquals(NearByViewModel.UiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `refresh retriggers fetch and emits Success`() = runTest {
        coEvery { getRestaurantsUseCase.getNearby(testLocation) } returns Result.success(testRestaurants)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setLocation(testLocation)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        coVerify(exactly = 2) { getRestaurantsUseCase.getNearby(testLocation) }
        assertTrue(viewModel.uiState.value is NearByViewModel.UiState.Success)
    }

    @Test
    fun `IOException produces network error message`() = runTest {
        coEvery { getRestaurantsUseCase.getNearby(testLocation) } returns
            Result.failure(java.io.IOException("Connection refused"))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        viewModel.setLocation(testLocation)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is NearByViewModel.UiState.Error)
        assertEquals("Network error — check your connection", state.message)
    }
}
