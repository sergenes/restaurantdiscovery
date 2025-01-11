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

    fun toggleFavorite(restaurantId: String) {
        viewModelScope.launch {
            val isFavorite = !favorites.value.contains(restaurantId)
            favoritesRepository.toggleFavorite(restaurantId, isFavorite)
        }
    }
}