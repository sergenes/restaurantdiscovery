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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nes.lunchtime.R
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
import com.nes.lunchtime.ui.theme.Dimens
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
    location: LatLng,
    onSelected: (Restaurant) -> Unit,
    viewModel: NearByViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel()
) {
    /**
     * Search query state managed in Composable (not ViewModel) for better text field performance.
     *
     * This is an acceptable pattern because:
     * 1. TextField needs immediate, lag-free updates - storing in StateFlow would add latency
     * 2. The actual business logic (debouncing, API calls) is in SearchViewModel
     * 3. Search results (the important data) ARE in ViewModel and survive config changes
     * 4. Losing the typed query on rotation is acceptable for search fields
     *
     * The query text is passed to SearchViewModel for debouncing (line 96), so business
     * logic remains in the ViewModel layer where it belongs.
     */
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val favorites by favoritesViewModel.favorites.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val nearbyState by viewModel.uiState.collectAsState()
    val searchState by searchViewModel.uiState.collectAsState()

    // Load nearby restaurants when location changes
    LaunchedEffect(location) {
        viewModel.onLoad(location)
        searchViewModel.setLocation(location)
    }

    HomeScreenContent(
        location = location,
        query = query,
        favorites = favorites.toList(),
        nearbyState = nearbyState,
        searchState = searchState,
        onQueryChange = { newQuery ->
            query = newQuery
            searchViewModel.onSearchQueryChanged(newQuery.text)
        },
        onSearch = {
            searchViewModel.getRestaurantsByText(query.text, location)
            keyboardController?.hide()
        },
        onSelected = onSelected,
        onFavoriteClicked = { restaurant ->
            favoritesViewModel.toggleFavorite(restaurant.id)
        },
        onRetrySearch = searchViewModel::retry,
        onRetryNearby = viewModel::retry
    )
}

@Composable
private fun HomeScreenContent(
    location: LatLng,
    query: TextFieldValue,
    favorites: List<String>,
    nearbyState: NearByViewModel.UiState,
    searchState: SearchViewModel.UiState,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    onSelected: (Restaurant) -> Unit,
    onFavoriteClicked: (Restaurant) -> Unit,
    onRetrySearch: () -> Unit,
    onRetryNearby: () -> Unit
) {
    Scaffold(
        topBar = { BrandedAppHeader() }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch
            )
            
            when {
                query.text.isNotEmpty() -> {
                    when (val state = searchState) {
                        SearchViewModel.UiState.Initial -> EmptySearchState()
                        SearchViewModel.UiState.Loading -> IndeterminateCircularIndicator()
                        is SearchViewModel.UiState.Success -> {
                            if (state.restaurants.isEmpty()) {
                                EmptyResultsState(stringResource(R.string.no_restaurants_found_search))
                            } else {
                                RestaurantContent(
                                    restaurants = state.restaurants,
                                    favorites = favorites,
                                    location = location,
                                    onItemClicked = onSelected,
                                    onFavoriteClicked = onFavoriteClicked
                                )
                            }
                        }
                        is SearchViewModel.UiState.Error -> {
                            ErrorView(
                                message = state.message,
                                onRetry = onRetrySearch
                            )
                        }
                    }
                }
                else -> {
                    when (val state = nearbyState) {
                        NearByViewModel.UiState.Loading -> IndeterminateCircularIndicator()
                        is NearByViewModel.UiState.Success -> {
                            if (state.restaurants.isEmpty()) {
                                EmptyResultsState(stringResource(R.string.no_restaurants_found_nearby))
                            } else {
                                RestaurantContent(
                                    restaurants = state.restaurants,
                                    favorites = favorites,
                                    location = location,
                                    onItemClicked = onSelected,
                                    onFavoriteClicked = onFavoriteClicked
                                )
                            }
                        }
                        is NearByViewModel.UiState.Error -> {
                            ErrorView(
                                message = state.message,
                                onRetry = onRetryNearby
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
        .padding(bottom = Dimens.SpacingMedium, start = Dimens.SpacingSmall, end = Dimens.SpacingSmall)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.search_icon_content_description)
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
            shape = RoundedCornerShape(Dimens.SearchBarCornerRadius),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.SearchBarPaddingHorizontal)
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
                .padding(bottom = Dimens.SpacingMedium)
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
            text = stringResource(R.string.empty_search_prompt),
            style = typography.bodyLarge,
            color = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyResultsState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
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
            modifier = Modifier.padding(top = Dimens.SpacingXLarge)
        ) {
            Text(
                modifier = Modifier.padding(horizontal = Dimens.SpacingMedium, vertical = Dimens.SpacingSmall),
                text = message,
                color = colorScheme.onBackground
            )
            Button(onClick = onRetry) {
                Text(
                    text = stringResource(R.string.try_again),
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenLoadingPreview() {
    LunchtimeTheme {
        HomeScreenContent(
            location = sampleLocation,
            query = TextFieldValue(""),
            favorites = emptyList(),
            nearbyState = NearByViewModel.UiState.Loading,
            searchState = SearchViewModel.UiState.Initial,
            onQueryChange = {},
            onSearch = {},
            onSelected = {},
            onFavoriteClicked = {},
            onRetrySearch = {},
            onRetryNearby = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenNearbySuccessPreview() {
    LunchtimeTheme {
        HomeScreenContent(
            location = sampleLocation,
            query = TextFieldValue(""),
            favorites = sampleFavorites,
            nearbyState = NearByViewModel.UiState.Success(sampleRestaurants),
            searchState = SearchViewModel.UiState.Initial,
            onQueryChange = {},
            onSearch = {},
            onSelected = {},
            onFavoriteClicked = {},
            onRetrySearch = {},
            onRetryNearby = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenSearchSuccessPreview() {
    LunchtimeTheme {
        HomeScreenContent(
            location = sampleLocation,
            query = TextFieldValue("Pizza"),
            favorites = sampleFavorites,
            nearbyState = NearByViewModel.UiState.Success(sampleRestaurants),
            searchState = SearchViewModel.UiState.Success(sampleRestaurants),
            onQueryChange = {},
            onSearch = {},
            onSelected = {},
            onFavoriteClicked = {},
            onRetrySearch = {},
            onRetryNearby = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenErrorPreview() {
    LunchtimeTheme {
        HomeScreenContent(
            location = sampleLocation,
            query = TextFieldValue(""),
            favorites = emptyList(),
            nearbyState = NearByViewModel.UiState.Error("Failed to load nearby restaurants"),
            searchState = SearchViewModel.UiState.Initial,
            onQueryChange = {},
            onSearch = {},
            onSelected = {},
            onFavoriteClicked = {},
            onRetrySearch = {},
            onRetryNearby = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyResultsStatePreview() {
    LunchtimeTheme {
        EmptyResultsState(stringResource(R.string.no_restaurants_found_search))
    }
}

private val sampleRestaurants = listOf(
    Restaurant(
        id = "1",
        displayName = "Awesome Pizza Place",
        rating = 4.5,
        formattedAddress = "123 Main St, San Francisco, CA",
        photoUrl = "",
        latitude = 37.7749,
        longitude = -122.4194
    ),
    Restaurant(
        id = "2",
        displayName = "Burger Joint",
        rating = 4.0,
        formattedAddress = "456 Market St, San Francisco, CA",
        photoUrl = "",
        latitude = 37.7750,
        longitude = -122.4195
    )
)

private val sampleLocation = LatLng(37.7749, -122.4194)
private val sampleFavorites = listOf("1")
