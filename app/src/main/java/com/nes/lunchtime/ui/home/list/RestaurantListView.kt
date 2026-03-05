package com.nes.lunchtime.ui.home.list

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.ui.components.RestaurantCard
import com.nes.lunchtime.ui.theme.LunchtimeTheme

@Composable
fun RestaurantsList(
    restaurants: List<Restaurant>,
    favorites: List<String>,
    onItemClicked: (Restaurant) -> Unit,
    onFavoriteClicked: (Restaurant) -> Unit
) {
    // Convert favorites list to set for O(1) lookup performance
    val favoritesSet = remember(favorites) { favorites.toSet() }

    LazyColumn {
        items(
            items = restaurants,
            key = { it.id },
            contentType = { "restaurant" }
        ) { item ->
            RestaurantCard(
                restaurant = item,
                isFavorite = favoritesSet.contains(item.id),
                onItemClicked = onItemClicked,
                onFavoriteClicked = onFavoriteClicked
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RestaurantsListPreview() {
    val items = listOf(
        Restaurant(
            id = "id1", 
            displayName = "Pizza Palace",
            formattedAddress = "123 Pizza St",
            latitude = 0.0,
            longitude = 0.0,
            rating = 3.5,
            userRatingCount = 100,
            photoUrl = ""
        ), 
        Restaurant(
            id = "id2", 
            displayName = "Burger King",
            formattedAddress = "456 Burger Ave",
            latitude = 0.0,
            longitude = 0.0,
            rating = 4.5,
            userRatingCount = 150,
            photoUrl = ""
        )
    )
    val favorites = remember { mutableStateListOf<String>("id1") }
    
    LunchtimeTheme {
        RestaurantsList(
            restaurants = items, 
            favorites = favorites, 
            onItemClicked = {}, 
            onFavoriteClicked = {
                if (favorites.contains(it.id)) {
                    favorites.remove(it.id)
                } else {
                    favorites.add(it.id)
                }
            }
        )
    }
}
