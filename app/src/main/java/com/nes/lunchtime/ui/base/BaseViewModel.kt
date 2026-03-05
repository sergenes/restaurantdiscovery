package com.nes.lunchtime.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel class that provides common functionality for handling loading states
 * and error handling across all ViewModels in the app.
 *
 * This eliminates code duplication and ensures consistent error handling patterns.
 */
abstract class BaseViewModel : ViewModel() {

    /**
     * Executes a suspend block with automatic loading state management and error handling.
     *
     * This helper function:
     * 1. Sets the UI state to Loading before execution
     * 2. Executes the provided block
     * 3. Handles success by calling the onSuccess callback
     * 4. Handles failures by converting them to Error state
     * 5. Properly propagates CancellationException (critical for coroutine cancellation)
     *
     * @param T The type of data returned by the operation
     * @param uiState The MutableStateFlow to update with loading/success/error states
     * @param block The suspend function that returns a Result<T>
     * @param onSuccess Callback to convert the success data to a UiState
     *
     * Example usage:
     * ```
     * executeWithLoading(
     *     uiState = _uiState,
     *     block = { repository.fetchData() },
     *     onSuccess = { data -> UiState.Success(data) }
     * )
     * ```
     */
    protected fun <T, S> executeWithLoading(
        uiState: MutableStateFlow<S>,
        loadingState: S,
        block: suspend () -> Result<T>,
        onSuccess: (T) -> S,
        onError: (String) -> S
    ) {
        viewModelScope.launch {
            try {
                // Set loading state before executing the block
                uiState.value = loadingState

                // Execute the operation and handle the result
                val result = block()
                uiState.value = result.fold(
                    onSuccess = { data -> onSuccess(data) },
                    onFailure = { exception ->
                        // Don't catch CancellationException - it's used for coroutine cancellation
                        if (exception is CancellationException) throw exception

                        // Convert exception to error message
                        val errorMessage = exception.localizedMessage ?: "Unknown error occurred"
                        onError(errorMessage)
                    }
                )
            } catch (e: CancellationException) {
                // Always re-throw CancellationException to allow proper coroutine cancellation
                throw e
            } catch (e: Exception) {
                // Catch any other unexpected exceptions
                val errorMessage = e.localizedMessage ?: "Unknown error occurred"
                uiState.value = onError(errorMessage)
            }
        }
    }
}
