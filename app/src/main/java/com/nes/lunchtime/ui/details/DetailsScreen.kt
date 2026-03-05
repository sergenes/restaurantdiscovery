package com.nes.lunchtime.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nes.lunchtime.R
import com.nes.lunchtime.domain.PlaceDetails
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.net.model.ReviewDetails
import com.nes.lunchtime.net.model.ReviewText
import com.nes.lunchtime.ui.components.IndeterminateCircularIndicator
import com.nes.lunchtime.ui.home.ErrorView
import com.nes.lunchtime.ui.theme.Dimens
import com.nes.lunchtime.ui.theme.LunchtimeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    restaurant: Restaurant,
    onNavigateBack: () -> Unit,
    viewModel: DetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(restaurant.id) {
        viewModel.loadRestaurantDetails(restaurant.id)
    }

    DetailsScreenContent(
        restaurant = restaurant,
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetry = viewModel::retry
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsScreenContent(
    restaurant: Restaurant,
    uiState: DetailsViewModel.UiState,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = restaurant.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_content_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            RestaurantBasicInfo(restaurant)

            when (val state = uiState) {
                DetailsViewModel.UiState.Initial,
                DetailsViewModel.UiState.Loading -> {
                    IndeterminateCircularIndicator()
                }
                is DetailsViewModel.UiState.Success -> {
                    DetailsContent(state.details)
                }
                is DetailsViewModel.UiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = onRetry
                    )
                }
            }
        }
    }
}

@Composable
private fun RestaurantBasicInfo(restaurant: Restaurant) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.SpacingMedium)
    ) {
        Text(
            text = restaurant.displayName,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(Dimens.SpacingXSmall))
        Text(
            text = restaurant.formattedAddress,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.SpacingXSmall))
        Text(
            text = stringResource(R.string.distance_label, formatDistance(restaurant.distanceInMeters)),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(Dimens.SpacingXSmall))
        Text(
            text = stringResource(R.string.rating_label, restaurant.rating),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DetailsContent(details: PlaceDetails) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.SpacingMedium)
    ) {
        item {
            Text(
                text = stringResource(R.string.reviews_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = Dimens.SpacingSmall)
            )
        }

        details.reviews.let { reviews ->
            items(reviews) { review ->
                ReviewItem(review)
                Spacer(modifier = Modifier.height(Dimens.SpacingSmall))
            }
        }
    }
}

@Composable
private fun ReviewItem(review: ReviewDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.CardElevationSmall)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.SpacingMedium)
        ) {
            Text(
                text = review.text.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun formatDistance(distanceInMeters: Float) : String {
    return when {
        distanceInMeters < 1000 -> "${distanceInMeters.toInt()}m"
        else -> String.format("%.1fkm", distanceInMeters / 1000)
    }
}

@Preview(showBackground = true)
@Composable
fun DetailsScreenLoadingPreview() {
    LunchtimeTheme {
        DetailsScreenContent(
            restaurant = sampleRestaurant,
            uiState = DetailsViewModel.UiState.Loading,
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DetailsScreenSuccessPreview() {
    LunchtimeTheme {
        DetailsScreenContent(
            restaurant = sampleRestaurant,
            uiState = DetailsViewModel.UiState.Success(samplePlaceDetails),
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DetailsScreenErrorPreview() {
    LunchtimeTheme {
        DetailsScreenContent(
            restaurant = sampleRestaurant,
            uiState = DetailsViewModel.UiState.Error("Could not load reviews. Please try again later."),
            onNavigateBack = {},
            onRetry = {}
        )
    }
}

private val sampleRestaurant = Restaurant(
    id = "1",
    displayName = "Awesome Pizza Place",
    rating = 4.5,
    formattedAddress = "123 Main St, San Francisco, CA",
    photoUrl = "",
    latitude = 37.7749,
    longitude = -122.4194,
    distanceInMeters = 450f
)

private val samplePlaceDetails = PlaceDetails(
    id = "1",
    displayName = "Awesome Pizza Place",
    formattedAddress = "123 Main St, San Francisco, CA",
    latitude = 37.7749,
    longitude = -122.4194,
    rating = 4.5,
    userRatingCount = 120,
    reviews = listOf(
        ReviewDetails(ReviewText("Best pizza in town!", "en")),
        ReviewDetails(ReviewText("Great service and atmosphere.", "en")),
        ReviewDetails(ReviewText("A bit pricey but worth it.", "en"))
    )
)
