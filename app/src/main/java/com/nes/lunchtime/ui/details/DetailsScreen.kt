package com.nes.lunchtime.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nes.lunchtime.domain.PlaceDetails
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.net.model.ReviewDetails
import com.nes.lunchtime.ui.components.IndeterminateCircularIndicator
import com.nes.lunchtime.ui.home.ErrorView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    restaurant: Restaurant,
    viewModel: DetailsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(restaurant.id) {
        viewModel.loadRestaurantDetails(restaurant.id)
    }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        onRetry = viewModel::retry
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
            .padding(16.dp)
    ) {
        Text(
            text = restaurant.displayName,
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = restaurant.formattedAddress,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Distance: ${formatDistance(restaurant.distanceInMeters)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Rating: ${restaurant.rating}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DetailsContent(details: PlaceDetails) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Reviews",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        details.reviews.let { reviews ->
            items(reviews) { review ->
                ReviewItem(review)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReviewItem(review: ReviewDetails) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
