package com.nes.lunchtime.data.favorites

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavoritesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    fun getFavorites(): Flow<Set<String>> {
        return dataStore.data.map { preferences ->
            preferences[FAVORITES_KEY] ?: emptySet()
        }
    }

    suspend fun addFavorite(restaurantId: String) {
        dataStore.edit { preferences ->
            val currentFavorites = preferences[FAVORITES_KEY] ?: emptySet()
            preferences[FAVORITES_KEY] = currentFavorites + restaurantId
        }
    }

    suspend fun removeFavorite(restaurantId: String) {
        dataStore.edit { preferences ->
            val currentFavorites = preferences[FAVORITES_KEY] ?: emptySet()
            preferences[FAVORITES_KEY] = currentFavorites - restaurantId
        }
    }

    companion object {
        private val FAVORITES_KEY = stringSetPreferencesKey("favorites")
    }
}