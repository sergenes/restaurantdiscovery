package com.nes.lunchtime.ui.location

import com.google.android.gms.maps.model.LatLng
import com.nes.lunchtime.MainCoroutineRule
import com.nes.lunchtime.data.location.LocationRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
class LocationViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var locationRepository: LocationRepository
    private lateinit var viewModel: LocationViewModel

    private val testLocation = LatLng(37.7749, -122.4194)

    @Before
    fun setup() {
        locationRepository = mockk()
        every { locationRepository.getLocationUpdates() } returns flowOf(
            LocationRepository.LocationResult.Success(testLocation)
        )
        viewModel = LocationViewModel(locationRepository)
    }

    @Test
    fun `initial state is Loading`() = runTest {
        every { locationRepository.getLocationUpdates() } returns flowOf()
        val freshViewModel = LocationViewModel(locationRepository)
        assertEquals(LocationViewModel.LocationState.Loading, freshViewModel.locationState.value)
    }

    @Test
    fun `emits LocationAvailable on successful location update`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.locationState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.locationState.value
        assertTrue(state is LocationViewModel.LocationState.LocationAvailable)
        assertEquals(testLocation, state.location)
    }

    @Test
    fun `each location update emits a new LocationAvailable`() = runTest {
        val secondLocation = LatLng(40.7128, -74.0060)
        every { locationRepository.getLocationUpdates() } returns flow {
            emit(LocationRepository.LocationResult.Success(testLocation))
            emit(LocationRepository.LocationResult.Success(secondLocation))
        }
        val freshViewModel = LocationViewModel(locationRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            freshViewModel.locationState.collect {}
        }
        advanceUntilIdle()

        val state = freshViewModel.locationState.value
        assertTrue(state is LocationViewModel.LocationState.LocationAvailable)
        assertEquals(secondLocation, state.location)
    }

    @Test
    fun `location error emits Error state`() = runTest {
        every { locationRepository.getLocationUpdates() } returns flowOf(
            LocationRepository.LocationResult.Error(Exception("GPS unavailable"))
        )
        val freshViewModel = LocationViewModel(locationRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            freshViewModel.locationState.collect {}
        }
        advanceUntilIdle()

        assertTrue(freshViewModel.locationState.value is LocationViewModel.LocationState.Error)
    }

    @Test
    fun `refreshLocation restarts flow and emits latest location`() = runTest {
        val refreshedLocation = LatLng(40.7128, -74.0060)
        every { locationRepository.getLocationUpdates() } returnsMany listOf(
            flowOf(LocationRepository.LocationResult.Success(testLocation)),
            flowOf(LocationRepository.LocationResult.Success(refreshedLocation))
        )
        val freshViewModel = LocationViewModel(locationRepository)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            freshViewModel.locationState.collect {}
        }
        advanceUntilIdle()

        freshViewModel.refreshLocation()
        advanceUntilIdle()

        val state = freshViewModel.locationState.value
        assertTrue(state is LocationViewModel.LocationState.LocationAvailable)
        assertEquals(refreshedLocation, state.location)
    }
}
