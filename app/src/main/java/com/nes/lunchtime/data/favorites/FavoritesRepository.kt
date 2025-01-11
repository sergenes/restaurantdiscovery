package com.nes.lunchtime.data.favorites

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FavoritesRepository @Inject constructor(
    private val favoritesDataSource: FavoritesDataSource
) {
    fun getFavorites(): Flow<Set<String>> {
        return favoritesDataSource.getFavorites()
    }

    suspend fun toggleFavorite(restaurantId: String, isFavorite: Boolean) {
        if (isFavorite) {
            favoritesDataSource.addFavorite(restaurantId)
        } else {
            favoritesDataSource.removeFavorite(restaurantId)
        }
    }
}