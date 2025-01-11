package com.nes.lunchtime.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nes.lunchtime.domain.PlaceDetails
import com.nes.lunchtime.repo.RestaurantsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.CancellationException


@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repo: RestaurantsRepository
) : ViewModel() {

    sealed class UiState {
        data object Initial : UiState()
        data object Loading : UiState()
        data class Success(val details: PlaceDetails) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentRestaurantId: String? = null

    fun loadRestaurantDetails(id: String) {
        currentRestaurantId = id
        fetchDetails(id)
    }

    fun retry() {
        currentRestaurantId?.let { fetchDetails(it) }
    }

    private fun fetchDetails(id: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                val result = repo.getPlaceDetails(id)
                _uiState.value = when {
                    result.isSuccess -> {
                        val details = result.getOrNull()
                        if (details != null) {
                            UiState.Success(details)
                        } else {
                            UiState.Error("Restaurant details not found")
                        }
                    }
                    result.isFailure -> {
                        when (val exception = result.exceptionOrNull()) {
                            is CancellationException -> throw exception
                            null -> UiState.Error("Unknown error occurred")
                            else -> UiState.Error(
                                exception.localizedMessage ?: "Failed to load details"
                            )
                        }
                    }
                    else -> UiState.Error("Unknown error occurred")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "Unknown error occurred")
            }
        }
    }
}