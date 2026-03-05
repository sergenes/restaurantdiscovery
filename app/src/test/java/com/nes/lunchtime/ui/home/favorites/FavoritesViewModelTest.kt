package com.nes.lunchtime.ui.home.favorites


import com.nes.lunchtime.MainCoroutineRule
import com.nes.lunchtime.data.favorites.FavoritesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var repository: FavoritesRepository
    private lateinit var viewModel: FavoritesViewModel
    private val favoritesFlow = MutableStateFlow<Set<String>>(emptySet())

    @Before
    fun setup() {
        repository = mockk()
        coEvery { repository.getFavorites() } returns favoritesFlow
        coEvery { repository.toggleFavorite(any(), any()) } returns Unit
        viewModel = FavoritesViewModel(repository)
    }

    @Test
    fun `toggleFavorite adds restaurant when not in favorites`() = runTest {
        // Start collecting the StateFlow so it updates its value
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.favorites.collect()
        }

        val restaurantId = "test_id"
        favoritesFlow.value = emptySet()

        advanceUntilIdle()

        val currentState = viewModel.favorites.value
        assert(!currentState.contains(restaurantId))

        viewModel.toggleFavorite(restaurantId)
        advanceUntilIdle()

        coVerify {
            repository.toggleFavorite(
                restaurantId = restaurantId,
                isFavorite = true
            )
        }
        collectJob.cancel()
    }

    @Test
    fun `toggleFavorite removes restaurant when already in favorites`() = runTest {
        // Start collecting the StateFlow so it updates its value
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.favorites.collect()
        }

        val restaurantId = "test_id"
        favoritesFlow.value = setOf(restaurantId)

        advanceUntilIdle()

        val currentState = viewModel.favorites.value
        assert(currentState.contains(restaurantId))

        viewModel.toggleFavorite(restaurantId)
        advanceUntilIdle()

        coVerify {
            repository.toggleFavorite(
                restaurantId = restaurantId,
                isFavorite = false
            )
        }
        collectJob.cancel()
    }
}
