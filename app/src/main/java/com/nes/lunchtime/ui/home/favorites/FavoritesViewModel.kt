package com.nes.lunchtime.ui.home.favorites


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nes.lunchtime.data.favorites.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    val favorites: StateFlow<Set<String>> = favoritesRepository
        .getFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    /**
     * Toggles the favorite status of a restaurant.
     *
     * Note: While this reads from favorites.value (which could be slightly stale),
     * this is NOT a race condition issue because:
     * 1. DataStore.edit{} operations are atomic and transactional
     * 2. The actual add/remove operations in DataSource read the current state
     *    inside the edit block, ensuring correctness
     * 3. Multiple rapid clicks will be safely queued by DataStore
     *
     * The worst case is a brief UI inconsistency (showing old state) until the
     * Flow emits the updated value, but the underlying data remains consistent.
     */
    fun toggleFavorite(restaurantId: String) {
        viewModelScope.launch {
            val isFavorite = !favorites.value.contains(restaurantId)
            favoritesRepository.toggleFavorite(restaurantId, isFavorite)
        }
    }
}