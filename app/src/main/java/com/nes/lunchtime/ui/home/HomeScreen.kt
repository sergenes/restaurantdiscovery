package com.nes.lunchtime.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.ui.components.BrandedAppHeader
import com.nes.lunchtime.ui.components.IndeterminateCircularIndicator
import com.nes.lunchtime.ui.components.ViewSwitcherButton
import com.nes.lunchtime.ui.home.favorites.FavoritesViewModel
import com.nes.lunchtime.ui.home.list.RestaurantsList
import com.nes.lunchtime.ui.home.map.RestaurantMapView
import com.nes.lunchtime.ui.home.nearby.NearByViewModel
import com.nes.lunchtime.ui.home.search.SearchViewModel
import com.nes.lunchtime.ui.theme.LunchtimeTheme
import com.google.android.gms.maps.model.LatLng


sealed class ViewType(
    val title: String,
    val icon: ImageVector? = null
) {
    data object ListView : ViewType("Map", icon = Icons.Filled.Map)
    data object MapView : ViewType("List", icon = Icons.AutoMirrored.Filled.List)
}

@Composable
fun HomeScreen(
    viewModel: NearByViewModel,
    location: LatLng,
    searchViewModel: SearchViewModel,
    favoritesViewModel: FavoritesViewModel,
    onSelected: (Restaurant) -> Unit
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val favorites by favoritesViewModel.favorites.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val nearbyState by viewModel.uiState.collectAsState()
    val searchState by searchViewModel.uiState.collectAsState()

    // Load nearby restaurants when location changes
    LaunchedEffect(location) {
        viewModel.onLoad(location)
    }

    Scaffold(
        topBar = { BrandedAppHeader() }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchBar(
                query = query,
                onQueryChange = { newQuery ->
                    query = newQuery
                    if (newQuery.text.isEmpty()) {
                        searchViewModel.clearSearch()
                    }
                },
                onSearch = {
                    searchViewModel.getRestaurantsByText(query.text, location)
                    keyboardController?.hide()
                }
            )
            // Main content based on search and nearby states
            when {
                // Show search results if there's a query
                query.text.isNotEmpty() -> {
                    when (val state = searchState) {
                        SearchViewModel.UiState.Initial -> {
                            // Show empty state or prompt to search
                            EmptySearchState()
                        }
                        SearchViewModel.UiState.Loading -> {
                            IndeterminateCircularIndicator()
                        }
                        is SearchViewModel.UiState.Success -> {
                            RestaurantContent(
                                restaurants = state.restaurants,
                                favorites = favorites.toList(),
                                location = location,
                                onItemClicked = onSelected,
                                onFavoriteClicked = { restaurant ->
                                    favoritesViewModel.toggleFavorite(restaurant.id)
                                }
                            )
                        }
                        is SearchViewModel.UiState.Error -> {
                            ErrorView(
                                message = state.message,
                                onRetry = searchViewModel::retry
                            )
                        }
                    }
                }
                // Show nearby results if no search query
                else -> {
                    when (val state = nearbyState) {
                        NearByViewModel.UiState.Loading -> {
                            IndeterminateCircularIndicator()
                        }
                        is NearByViewModel.UiState.Success -> {
                            RestaurantContent(
                                restaurants = state.restaurants,
                                favorites = favorites.toList(),
                                location = location,
                                onItemClicked = onSelected,
                                onFavoriteClicked = { restaurant ->
                                    favoritesViewModel.toggleFavorite(restaurant.id)
                                }
                            )
                        }
                        is NearByViewModel.UiState.Error -> {
                            ErrorView(
                                message = state.message,
                                onRetry = viewModel::retry
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: () -> Unit
) {
    Column(modifier = Modifier
        .background(colorScheme.surface)
        .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search restaurants") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search Icon"
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                focusedContainerColor = colorScheme.surfaceContainer,
                unfocusedContainerColor = colorScheme.surfaceContainer,
                disabledContainerColor = colorScheme.surfaceContainer,
            ),
            shape = RoundedCornerShape(25.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }

}

@Composable
private fun RestaurantContent(
    restaurants: List<Restaurant>,
    favorites: List<String>,
    location: LatLng,
    onItemClicked: (Restaurant) -> Unit,
    onFavoriteClicked: (Restaurant) -> Unit
) {
    var currentViewType by remember { mutableStateOf<ViewType>(ViewType.ListView) }

    Box(
        modifier = Modifier
            .background(colorScheme.surfaceContainer)
            .fillMaxSize()
    ) {
        when (currentViewType) {
            ViewType.ListView -> {
                RestaurantsList(
                    restaurants = restaurants,
                    favorites = favorites,
                    onItemClicked = onItemClicked,
                    onFavoriteClicked = onFavoriteClicked
                )
            }
            ViewType.MapView -> {
                RestaurantMapView(
                    restaurants = restaurants,
                    currentLocation = location,
                    favorites = favorites,
                    onItemClicked = onItemClicked,
                    onFavoriteClicked = onFavoriteClicked
                )
            }
        }

        ViewSwitcherButton(
            currentViewType = currentViewType,
            onViewTypeChange = { currentViewType = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun EmptySearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Type to search for restaurants",
            style = typography.bodyLarge,
            color = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit = {}
) {
    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 64.dp)
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = message,
                color = colorScheme.onBackground
            )
            Button(onClick = onRetry) {
                Text(
                    text = "Try Again",
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchBarPreview() {
    LunchtimeTheme {
        SearchBar(
            query = TextFieldValue("Pizza"),
            onQueryChange = {},
            onSearch = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenErrorPreview() {
    LunchtimeTheme {
        ErrorView(
            message = "Unable to load restaurants. Please check your internet connection.",
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenEmptySearchPreview() {
    LunchtimeTheme {
        EmptySearchState()
    }
}

private val sampleRestaurants = listOf(
    Restaurant(
        id = "1",
        displayName = "Awesome Pizza Place",
        rating = 4.5,
        formattedAddress = "123 Main St, San Francisco, CA",
        photoUrl = "https://example.com/pizza.jpg",
        latitude = 37.7749,
        longitude = -122.4194
    ),
    Restaurant(
        id = "2",
        displayName = "Burger Joint",
        rating = 4.0,
        formattedAddress = "456 Market St, San Francisco, CA",
        photoUrl = "https://example.com/burger.jpg",
        latitude = 37.7750,
        longitude = -122.4195
    )
)

private val sampleLocation = LatLng(0.0, 0.0)
private val sampleFavorites = listOf("1")

@Preview(showBackground = true)
@Composable
fun HomeScreenSuccessPreview() {
    LunchtimeTheme {
        RestaurantContent(
            restaurants = sampleRestaurants,
            favorites = sampleFavorites,
            location = sampleLocation,
            onItemClicked = {},
            onFavoriteClicked = {}
        )
    }
}
