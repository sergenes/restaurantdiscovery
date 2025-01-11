package com.nes.lunchtime.ui.home.list

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.ui.components.RestaurantCard

@Composable
fun RestaurantsList(
    restaurants: List<Restaurant>,
    favorites: List<String>,
    onItemClicked: (Restaurant) -> Unit,
    onFavoriteClicked: (Restaurant) -> Unit
) {
    LazyColumn {
        items(restaurants) { item ->
            RestaurantCard(
                restaurant = item,
                isFavorite = favorites.contains(item.id),
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
            "id1", "Name 1",
            "Address 1",
            0.0,
            0.0,
            3.5,
            100,
            ""
        ), Restaurant(
            "id2", "Name 2",
            "Address 2",
            0.0,
            0.0,
            4.5,
            150,
            ""
        )
    )
    val favorites = remember { mutableStateListOf<String>("id1") }
    RestaurantsList(items, favorites, onItemClicked = {}, onFavoriteClicked = {
        if (favorites.contains(it.id)) {
            favorites.remove(it.id)
        } else {
            favorites.add(it.id)
        }
    })
}