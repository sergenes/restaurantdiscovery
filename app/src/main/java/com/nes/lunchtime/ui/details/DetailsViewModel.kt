package com.nes.lunchtime.ui.details

import com.nes.lunchtime.domain.PlaceDetails
import com.nes.lunchtime.repo.RestaurantsRepository
import com.nes.lunchtime.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the restaurant details screen.
 * Extends BaseViewModel to utilize common loading and error handling patterns.
 */
@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val repo: RestaurantsRepository
) : BaseViewModel() {

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

    /**
     * Fetches restaurant details for the given ID.
     * Uses the BaseViewModel's executeWithLoading to handle loading states and errors consistently.
     */
    private fun fetchDetails(id: String) {
        executeWithLoading(
            uiState = _uiState,
            loadingState = UiState.Loading,
            block = { repo.getPlaceDetails(id) },
            onSuccess = { details -> UiState.Success(details) },
            onError = { message -> UiState.Error(message) }
        )
    }
}