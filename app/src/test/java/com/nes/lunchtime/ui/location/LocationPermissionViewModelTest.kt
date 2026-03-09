package com.nes.lunchtime.ui.location

import com.nes.lunchtime.MainCoroutineRule
import com.nes.lunchtime.location.LocationPermissionManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
class LocationPermissionViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var locationPermissionManager: LocationPermissionManager
    private lateinit var viewModel: LocationPermissionViewModel

    // Backing flow we control in tests — simulates permission state changes.
    private val permissionStateFlow =
        MutableStateFlow<LocationPermissionManager.PermissionState>(
            LocationPermissionManager.PermissionState.Unknown
        )

    @Before
    fun setup() {
        locationPermissionManager = mockk {
            every { permissionState } returns permissionStateFlow.asStateFlow()
            // Called when permission state is Unknown — does nothing in tests;
            // we drive state changes directly via permissionStateFlow.
            every { checkPermission() } just Runs
        }
        viewModel = LocationPermissionViewModel(locationPermissionManager)
    }

    @Test
    fun `initial state is Loading`() = runTest {
        assertEquals(LocationPermissionViewModel.PermissionState.Loading, viewModel.state.value)
    }

    @Test
    fun `permission Unknown triggers checkPermission`() = runTest {
        // init already triggered checkPermission() once via Unknown state
        verify(exactly = 1) { locationPermissionManager.checkPermission() }
    }

    @Test
    fun `permission Granted emits Granted state`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }

        permissionStateFlow.value = LocationPermissionManager.PermissionState.Granted
        advanceUntilIdle()

        assertEquals(LocationPermissionViewModel.PermissionState.Granted, viewModel.state.value)
    }

    @Test
    fun `permission Denied emits PermissionDenied`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }

        permissionStateFlow.value = LocationPermissionManager.PermissionState.Denied
        advanceUntilIdle()

        assertEquals(LocationPermissionViewModel.PermissionState.PermissionDenied, viewModel.state.value)
    }

    @Test
    fun `permission ShowRationale emits PermissionRequired`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }

        permissionStateFlow.value = LocationPermissionManager.PermissionState.ShowRationale
        advanceUntilIdle()

        assertEquals(LocationPermissionViewModel.PermissionState.PermissionRequired, viewModel.state.value)
    }

    @Test
    fun `onPermissionGranted calls checkPermission`() = runTest {
        viewModel.onPermissionGranted()
        advanceUntilIdle()

        // Once from init (Unknown state) + once from onPermissionGranted
        verify(exactly = 2) { locationPermissionManager.checkPermission() }
    }

    @Test
    fun `onPermissionDenied emits Error`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }

        viewModel.onPermissionDenied()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is LocationPermissionViewModel.PermissionState.Error)
    }

    @Test
    fun `onPermissionDismissed emits Error`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }

        viewModel.onPermissionDismissed()
        advanceUntilIdle()

        assertTrue(viewModel.state.value is LocationPermissionViewModel.PermissionState.Error)
    }

    @Test
    fun `retry calls checkPermission`() = runTest {
        viewModel.retry()
        advanceUntilIdle()

        // Once from init (Unknown state) + once from retry
        verify(exactly = 2) { locationPermissionManager.checkPermission() }
    }
}
