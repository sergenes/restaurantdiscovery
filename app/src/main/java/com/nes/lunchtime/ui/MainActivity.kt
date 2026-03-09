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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nes.lunchtime.ui.details.DetailsScreen
import com.nes.lunchtime.ui.home.HomeScreen
import com.nes.lunchtime.ui.location.LocationPermissionDeniedDialog
import com.nes.lunchtime.ui.location.LocationPermissionDialog
import com.nes.lunchtime.ui.location.LocationPermissionViewModel
import com.nes.lunchtime.ui.navigation.Details
import com.nes.lunchtime.ui.navigation.Home
import com.nes.lunchtime.ui.theme.LunchtimeTheme
import com.nes.lunchtime.ui.theme.Dimens
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val locationPermissionViewModel by viewModels<LocationPermissionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LunchtimeTheme {
                MainContent(locationPermissionViewModel = locationPermissionViewModel)
            }
        }
    }

    @Composable
    private fun MainContent(locationPermissionViewModel: LocationPermissionViewModel) {
        val permissionState by locationPermissionViewModel.state.collectAsState()

        MainContentImpl(
            permissionState = permissionState,
            onPermissionGranted = locationPermissionViewModel::onPermissionGranted,
            onPermissionDenied = locationPermissionViewModel::onPermissionDenied,
            onPermissionDismissed = locationPermissionViewModel::onPermissionDismissed,
            onRetry = locationPermissionViewModel::retry,
            onGoToSettings = { goToAppSettings() }
        )
    }

    private fun goToAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
        this.finish()
    }
}

@Composable
fun MainContentImpl(
    permissionState: LocationPermissionViewModel.PermissionState,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPermissionDismissed: () -> Unit,
    onRetry: () -> Unit,
    onGoToSettings: () -> Unit
) {
    val navController = rememberNavController()

    when (permissionState) {
        is LocationPermissionViewModel.PermissionState.PermissionRequired -> {
            LocationPermissionDialog(
                onGranted = onPermissionGranted,
                onDenied = onPermissionDenied
            )
        }

        is LocationPermissionViewModel.PermissionState.PermissionDenied -> {
            LocationPermissionDeniedDialog(
                onGranted = onPermissionGranted,
                onDismiss = onPermissionDismissed,
                onGoToSettings = onGoToSettings
            )
        }

        is LocationPermissionViewModel.PermissionState.Granted -> {
            NavHost(navController, startDestination = Home) {
                composable<Home> {
                    HomeScreen(
                        onSelected = { restaurant ->
                            navController.navigate(Details.fromRestaurant(restaurant))
                        }
                    )
                }
                composable<Details> { backStackEntry ->
                    val details: Details = backStackEntry.toRoute()
                    DetailsScreen(
                        restaurant = details.toRestaurant(),
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }

        is LocationPermissionViewModel.PermissionState.Error -> {
            ErrorScreen(
                message = permissionState.message,
                onRetry = onRetry
            )
        }

        LocationPermissionViewModel.PermissionState.Loading -> {
            LoadingScreen()
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.SpacingMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.SpacingMedium))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    LunchtimeTheme {
        LoadingScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ErrorScreenPreview() {
    LunchtimeTheme {
        ErrorScreen(
            message = "An error occurred while fetching your location.",
            onRetry = {}
        )
    }
}
