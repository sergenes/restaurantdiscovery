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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nes.lunchtime.R
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.ui.ErrorScreen
import com.nes.lunchtime.ui.LoadingScreen
import com.nes.lunchtime.ui.components.BrandedAppHeader
import com.nes.lunchtime.ui.components.IndeterminateCircularIndicator
import com.nes.lunchtime.ui.components.ViewSwitcherButton
import com.nes.lunchtime.ui.home.favorites.FavoritesViewModel
import com.nes.lunchtime.ui.home.list.RestaurantsList
import com.nes.lunchtime.ui.home.map.RestaurantMapView
import com.nes.lunchtime.ui.home.nearby.NearByViewModel
import com.nes.lunchtime.ui.home.search.SearchViewModel
import com.nes.lunchtime.ui.location.LocationViewModel
import com.nes.lunchtime.ui.theme.LunchtimeTheme
import com.nes.lunchtime.ui.theme.Dimens
import com.nes.lunchtime.ui.home.search.SearchViewModel.UiState.Initial
import com.nes.lunchtime.ui.home.search.SearchViewModel.UiState.Success
import com.nes.lunchtime.ui.home.search.SearchViewModel.UiState.Error
import com.nes.lunchtime.ui.home.search.SearchViewModel.UiState.Loading

sealed class ViewType(
    val title: String,
    val icon: ImageVector? = null
) {
    data object ListView : ViewType("Map", icon = Icons.Filled.Map)
    data object MapView : ViewType("List", icon = Icons.AutoMirrored.Filled.List)
}

@Composable
fun HomeScreen(
    onSelected: (Restaurant) -> Unit,
    locationViewModel: LocationViewModel = hiltViewModel(),
    viewModel: NearByViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    favoritesViewModel: FavoritesViewModel = hiltViewModel()
) {
    val locationState by locationViewModel.locationState.collectAsState()

    when (val state = locationState) {
        LocationViewModel.LocationState.Loading -> LoadingScreen()
        is LocationViewModel.LocationState.Error -> ErrorScreen(
            message = state.message,
            onRetry = locationViewModel::refreshLocation
        )

        is LocationViewModel.LocationState.LocationAvailable -> {
            // SideEffect lives here — it only runs when the LocationAvailable state changes
            // (i.e., when location actually updates), not on inner HomeScreenContent recompositions.
            // StateFlow in NearByViewModel/SearchViewModel deduplicates equal values.
            SideEffect {
                viewModel.setLocation(state.location)
                searchViewModel.setLocation(state.location)
            }
            HomeScreenContent(
                onSelected = onSelected,
                locationViewModel = locationViewModel,
                viewModel = viewModel,
                searchViewModel = searchViewModel,
                favoritesViewModel = favoritesViewModel
            )
        }
    }
}

@Composable
private fun HomeScreenContent(
    onSelected: (Restaurant) -> Unit,
    locationViewModel: LocationViewModel,
    viewModel: NearByViewModel,
    searchViewModel: SearchViewModel,
    favoritesViewModel: FavoritesViewModel
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val favorites by favoritesViewModel.favorites.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val nearbyState by viewModel.uiState.collectAsState()
    val searchState by searchViewModel.uiState.collectAsState()

    // Hoisted with rememberSaveable so the view choice survives both NearBy loading
    // cycles (list → loading → list) and any future composition restarts.
    var isMapView by rememberSaveable { mutableStateOf(false) }
    val currentViewType = if (isMapView) ViewType.MapView else ViewType.ListView

    HomeScreenLayout(
        query = query,
        favorites = favorites.toList(),
        nearbyState = nearbyState,
        searchState = searchState,
        onQueryChange = { newQuery ->
            query = newQuery
            searchViewModel.onSearchQueryChanged(query.text)
        },
        onSearch = {
            // Read location imperatively — avoids collectAsState() here which would
            // cause HomeScreenContent to recompose on every location tick.
            val location = (locationViewModel.locationState.value
                    as? LocationViewModel.LocationState.LocationAvailable)?.location
            if (location != null) searchViewModel.getRestaurantsByText(query.text, location)
            keyboardController?.hide()
        },
        onSelected = onSelected,
        onFavoriteClicked = { restaurant -> favoritesViewModel.toggleFavorite(restaurant.id) },
        onRefresh = {
            locationViewModel.refreshLocation()
            viewModel.refresh()
        },
        onRetrySearch = searchViewModel::retry,
        onRetryNearby = viewModel::retry,
        // Slot — HomeScreenLayout has no location dependency; only RestaurantContent does.
        restaurantContent = { restaurants ->
            RestaurantContent(
                restaurants = restaurants,
                favorites = favorites.toList(),
                locationViewModel = locationViewModel,
                currentViewType = currentViewType,
                onViewTypeChange = { isMapView = it is ViewType.MapView },
                onItemClicked = onSelected,
                onFavoriteClicked = { restaurant -> favoritesViewModel.toggleFavorite(restaurant.id) }
            )
        }
    )
}

/**
 * Pure layout composable — no location or ViewModel dependency.
 * Receives restaurant content as a slot so it never recomposes due to location ticks.
 */
@Composable
private fun HomeScreenLayout(
    query: TextFieldValue,
    favorites: List<String>,
    nearbyState: NearByViewModel.UiState,
    searchState: SearchViewModel.UiState,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    onSelected: (Restaurant) -> Unit,
    onFavoriteClicked: (Restaurant) -> Unit,
    onRefresh: () -> Unit,
    onRetrySearch: () -> Unit,
    onRetryNearby: () -> Unit,
    restaurantContent: @Composable (restaurants: List<Restaurant>) -> Unit
) {
    Scaffold(
        topBar = { BrandedAppHeader(onRefresh = onRefresh) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            SearchBar(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch
            )

            if (query.text.isNotEmpty()) {
                SearchContent(searchState, restaurantContent, onRetrySearch)
            } else {
                NearbyContent(nearbyState, restaurantContent, onRetryNearby)
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
    Column(
        modifier = Modifier
            .background(colorScheme.surface)
            .padding(
                bottom = Dimens.SpacingMedium,
                start = Dimens.SpacingSmall,
                end = Dimens.SpacingSmall
            )
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

/**
 * Collects location directly — only this composable recomposes on GPS ticks.
 * The scaffold, search bar, and loading/error states above are unaffected.
 */
@Composable
private fun RestaurantContent(
    restaurants: List<Restaurant>,
    favorites: List<String>,
    locationViewModel: LocationViewModel,
    currentViewType: ViewType,
    onViewTypeChange: (ViewType) -> Unit,
    onItemClicked: (Restaurant) -> Unit,
    onFavoriteClicked: (Restaurant) -> Unit
) {
    val locationState by locationViewModel.locationState.collectAsState()
    val location = (locationState as? LocationViewModel.LocationState.LocationAvailable)?.location
        ?: return  // guard: HomeScreen only shows content when LocationAvailable

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
            onViewTypeChange = onViewTypeChange,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Dimens.SpacingMedium)
        )
    }
}

@Composable
private fun SearchContent(
    state: SearchViewModel.UiState,
    restaurantContent: @Composable (List<Restaurant>) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        Initial -> EmptySearchState()
        Loading -> IndeterminateCircularIndicator()
        is Success if state.restaurants.isEmpty() ->
            EmptyResultsState(stringResource(R.string.no_restaurants_found_search))
        is Success -> restaurantContent(state.restaurants)
        is Error -> ErrorView(message = state.message, onRetry = onRetry)
    }
}

@Composable
private fun NearbyContent(
    state: NearByViewModel.UiState,
    restaurantContent: @Composable (List<Restaurant>) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        NearByViewModel.UiState.Loading,
        NearByViewModel.UiState.Refreshing -> IndeterminateCircularIndicator()
        is NearByViewModel.UiState.Success if state.restaurants.isEmpty() ->
            EmptyResultsState(stringResource(R.string.no_restaurants_found_nearby))
        is NearByViewModel.UiState.Success -> restaurantContent(state.restaurants)
        is NearByViewModel.UiState.Error -> ErrorView(message = state.message, onRetry = onRetry)
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
                modifier = Modifier.padding(
                    horizontal = Dimens.SpacingMedium,
                    vertical = Dimens.SpacingSmall
                ),
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

// ── Previews ─────────────────────────────────────────────────────────────────
// HomeScreenLayout previews use a simple list slot — no ViewModel or location needed.

@Preview(showBackground = true)
@Composable
fun HomeScreenLoadingPreview() {
    LunchtimeTheme {
        HomeScreenLayout(
            query = TextFieldValue(""),
            favorites = emptyList(),
            nearbyState = NearByViewModel.UiState.Loading,
            searchState = SearchViewModel.UiState.Initial,
            onQueryChange = {}, onSearch = {}, onSelected = {},
            onFavoriteClicked = {}, onRefresh = {}, onRetrySearch = {}, onRetryNearby = {},
            restaurantContent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenNearbySuccessPreview() {
    LunchtimeTheme {
        HomeScreenLayout(
            query = TextFieldValue(""),
            favorites = sampleFavorites,
            nearbyState = NearByViewModel.UiState.Success(sampleRestaurants),
            searchState = SearchViewModel.UiState.Initial,
            onQueryChange = {}, onSearch = {}, onSelected = {},
            onFavoriteClicked = {}, onRefresh = {}, onRetrySearch = {}, onRetryNearby = {},
            restaurantContent = { restaurants ->
                RestaurantsList(
                    restaurants = restaurants,
                    favorites = sampleFavorites,
                    onItemClicked = {},
                    onFavoriteClicked = {}
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenSearchSuccessPreview() {
    LunchtimeTheme {
        HomeScreenLayout(
            query = TextFieldValue("Pizza"),
            favorites = sampleFavorites,
            nearbyState = NearByViewModel.UiState.Success(sampleRestaurants),
            searchState = SearchViewModel.UiState.Success(sampleRestaurants),
            onQueryChange = {}, onSearch = {}, onSelected = {},
            onFavoriteClicked = {}, onRefresh = {}, onRetrySearch = {}, onRetryNearby = {},
            restaurantContent = { restaurants ->
                RestaurantsList(
                    restaurants = restaurants,
                    favorites = sampleFavorites,
                    onItemClicked = {},
                    onFavoriteClicked = {}
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenErrorPreview() {
    LunchtimeTheme {
        HomeScreenLayout(
            query = TextFieldValue(""),
            favorites = emptyList(),
            nearbyState = NearByViewModel.UiState.Error("Failed to load nearby restaurants"),
            searchState = SearchViewModel.UiState.Initial,
            onQueryChange = {}, onSearch = {}, onSelected = {},
            onFavoriteClicked = {}, onRefresh = {}, onRetrySearch = {}, onRetryNearby = {},
            restaurantContent = {}
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

private val sampleFavorites = listOf("1")
