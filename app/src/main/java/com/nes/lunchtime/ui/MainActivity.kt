package com.nes.lunchtime.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nes.lunchtime.domain.Restaurant

import com.nes.lunchtime.ui.home.HomeScreen
import com.nes.lunchtime.ui.home.nearby.NearByViewModel
import com.nes.lunchtime.ui.details.DetailsScreen
import com.nes.lunchtime.ui.details.DetailsViewModel
import com.nes.lunchtime.ui.home.favorites.FavoritesViewModel
import com.nes.lunchtime.ui.home.search.SearchViewModel
import com.nes.lunchtime.ui.location.LocationPermissionDeniedDialog
import com.nes.lunchtime.ui.location.LocationPermissionDialog
import com.nes.lunchtime.ui.location.LocationViewModel
import com.nes.lunchtime.ui.theme.LunchtimeTheme
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val locationViewModel by viewModels<LocationViewModel>()
    private val nearByViewModel by viewModels<NearByViewModel>()
    private val searchViewModel by viewModels<SearchViewModel>()
    private val detailsViewModel by viewModels<DetailsViewModel>()
    private val favoritesViewModel by viewModels<FavoritesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LunchtimeTheme {
                MainContent(
                    locationViewModel = locationViewModel,
                    nearByViewModel = nearByViewModel,
                    searchViewModel = searchViewModel,
                    detailsViewModel = detailsViewModel,
                    favoritesViewModel = favoritesViewModel
                )
            }
        }
    }

    @Composable
    private fun MainContent(
        locationViewModel: LocationViewModel,
        nearByViewModel: NearByViewModel,
        searchViewModel: SearchViewModel,
        detailsViewModel: DetailsViewModel,
        favoritesViewModel: FavoritesViewModel
    ) {
        val locationState by locationViewModel.locationState.collectAsState()
        val navController = rememberNavController()

        // Handle location state changes
        when (val state = locationState) {
            is LocationViewModel.LocationState.PermissionRequired -> {
                LocationPermissionDialog(
                    onGranted = locationViewModel::onPermissionGranted,
                    onDenied = locationViewModel::onPermissionDenied
                )
            }

            is LocationViewModel.LocationState.PermissionDenied -> {
                LocationPermissionDeniedDialog(
                    onGranted = locationViewModel::onPermissionGranted,
                    onDismiss = locationViewModel::onPermissionDismissed,
                    onGoToSettings = { goToAppSettings() }
                )
            }

            is LocationViewModel.LocationState.LocationAvailable -> {
                var selectedRestaurant : Restaurant? = null
                NavHost(navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            viewModel = nearByViewModel,
                            location = state.location,
                            searchViewModel = searchViewModel,
                            favoritesViewModel = favoritesViewModel,
                            onSelected = { restaurant ->
                                selectedRestaurant = restaurant
                                navController.navigate("details")
                            }
                        )
                    }
                    composable("details") {
                        selectedRestaurant?.let {
                            DetailsScreen(
                                restaurant = it,
                                viewModel = detailsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }

            is LocationViewModel.LocationState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = locationViewModel::retry
                )
            }

            LocationViewModel.LocationState.Loading -> {
                LoadingScreen()
            }
        }
    }

    @Composable
    private fun LoadingScreen() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    @Composable
    private fun ErrorScreen(
        message: String,
        onRetry: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }

    private fun goToAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
        this.finish()
    }
}

