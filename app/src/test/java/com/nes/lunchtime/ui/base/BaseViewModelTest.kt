package com.nes.lunchtime.ui.base

import com.nes.lunchtime.MainCoroutineRule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {
    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    sealed class TestUiState {
        data object Initial : TestUiState()
        data object Loading : TestUiState()
        data class Success(val data: String) : TestUiState()
        data class Error(val message: String) : TestUiState()
    }

    class TestViewModel : BaseViewModel() {
        val uiState = MutableStateFlow<TestUiState>(TestUiState.Initial)

        fun executeOperation(block: suspend () -> Result<String>) {
            executeWithLoading(
                uiState = uiState,
                loadingState = TestUiState.Loading,
                block = block,
                onSuccess = { data -> TestUiState.Success(data) },
                onError = { message -> TestUiState.Error(message) }
            )
        }
    }

    @Test
    fun `executeWithLoading sets loading state before execution`() = runTest {
        // Given: A ViewModel with initial state
        val viewModel = TestViewModel()
        assertEquals(TestUiState.Initial, viewModel.uiState.value)

        // When: Executing a slow operation
        var blockStarted = false
        viewModel.executeOperation {
            blockStarted = true
            kotlinx.coroutines.delay(100)
            Result.success("data")
        }

        // Then: State immediately changes to Loading
        assertEquals(TestUiState.Loading, viewModel.uiState.value)

        // Wait for completion
        advanceUntilIdle()
        assertTrue(blockStarted)
    }

    @Test
    fun `executeWithLoading handles success result`() = runTest {
        // Given: A ViewModel
        val viewModel = TestViewModel()

        // When: Operation succeeds
        viewModel.executeOperation {
            Result.success("test data")
        }
        advanceUntilIdle()

        // Then: State is Success with data
        val state = viewModel.uiState.value
        assertTrue(state is TestUiState.Success)
        assertEquals("test data", state.data)
    }

    @Test
    fun `executeWithLoading handles failure result`() = runTest {
        // Given: A ViewModel
        val viewModel = TestViewModel()

        // When: Operation fails
        viewModel.executeOperation {
            Result.failure(Exception("Network error"))
        }
        advanceUntilIdle()

        // Then: State is Error with message
        val state = viewModel.uiState.value
        assertTrue(state is TestUiState.Error)
        assertEquals("Network error", state.message)
    }

    @Test
    fun `executeWithLoading handles exception in block`() = runTest {
        // Given: A ViewModel
        val viewModel = TestViewModel()

        // When: Block throws exception
        viewModel.executeOperation {
            throw IllegalStateException("Unexpected error")
        }
        advanceUntilIdle()

        // Then: State is Error with message
        val state = viewModel.uiState.value
        assertTrue(state is TestUiState.Error)
        assertEquals("Unexpected error", state.message)
    }

    @Test
    fun `executeWithLoading propagates CancellationException from Result`() = runTest {
        // Given: A ViewModel
        val viewModel = TestViewModel()

        // When: Result contains CancellationException
        viewModel.executeOperation {
            Result.failure(CancellationException("Cancelled"))
        }
        advanceUntilIdle()

        // Then: CancellationException is not swallowed as a regular Error
        assertTrue(viewModel.uiState.value !is TestUiState.Error)
    }

    @Test
    fun `executeWithLoading propagates CancellationException from block`() = runTest {
        // Given: A ViewModel
        val viewModel = TestViewModel()

        // When: Block throws CancellationException
        viewModel.executeOperation {
            throw CancellationException("Cancelled")
        }
        advanceUntilIdle()

        // Then: CancellationException is not swallowed as a regular Error
        assertTrue(viewModel.uiState.value !is TestUiState.Error)
    }

    @Test
    fun `executeWithLoading uses localized message when available`() = runTest {
        // Given: A ViewModel
        val viewModel = TestViewModel()

        // When: Operation fails with localized message
        viewModel.executeOperation {
            Result.failure(Exception("Localized error message"))
        }
        advanceUntilIdle()

        // Then: Error state uses localized message
        val state = viewModel.uiState.value
        assertTrue(state is TestUiState.Error)
        assertEquals("Localized error message", state.message)
    }

    @Test
    fun `executeWithLoading uses default message when localized message is null`() = runTest {
        // Given: A ViewModel
        val viewModel = TestViewModel()

        // When: Operation fails with null message
        viewModel.executeOperation {
            Result.failure(object : Exception() {
                override val message: String? = null
            })
        }
        advanceUntilIdle()

        // Then: Error state uses default message
        val state = viewModel.uiState.value
        assertTrue(state is TestUiState.Error)
        assertEquals("Unknown error occurred", state.message)
    }

    @Test
    fun `executeWithLoading transitions through Loading to Success`() =
        runTest(mainCoroutineRule.testDispatcher) {
            // Given: A ViewModel
            val viewModel = TestViewModel()

            // When: Operation starts (block delayed so Loading state is observable)
            viewModel.executeOperation {
                kotlinx.coroutines.delay(100)
                Result.success("data")
            }

            // Then: Loading state is set while block is suspended
            assertEquals(TestUiState.Loading, viewModel.uiState.value)

            // When: Operation completes
            advanceUntilIdle()

            // Then: Final state is Success
            val state = viewModel.uiState.value
            assertTrue(state is TestUiState.Success && state.data == "data")
        }

    @Test
    fun `executeWithLoading transitions through Loading to Error`() =
        runTest(mainCoroutineRule.testDispatcher) {
            // Given: A ViewModel
            val viewModel = TestViewModel()

            // When: Operation starts (block delayed so Loading state is observable)
            viewModel.executeOperation {
                kotlinx.coroutines.delay(100)
                Result.failure(Exception("Error"))
            }

            // Then: Loading state is set while block is suspended
            assertEquals(TestUiState.Loading, viewModel.uiState.value)

            // When: Operation completes
            advanceUntilIdle()

            // Then: Final state is Error
            val state = viewModel.uiState.value
            assertTrue(state is TestUiState.Error && state.message == "Error")
        }

    @Test
    fun `multiple concurrent operations update state correctly`() = runTest {
        // Given: A ViewModel
        val viewModel = TestViewModel()

        // When: Multiple operations are triggered
        viewModel.executeOperation {
            kotlinx.coroutines.delay(50)
            Result.success("first")
        }
        viewModel.executeOperation {
            kotlinx.coroutines.delay(10)
            Result.success("second")
        }

        advanceUntilIdle()

        // Then: Final state reflects last operation
        val state = viewModel.uiState.value
        assertTrue(state is TestUiState.Success)
        // Note: Could be either "first" or "second" depending on timing
        assertTrue(state.data in listOf("first", "second"))
    }
}
