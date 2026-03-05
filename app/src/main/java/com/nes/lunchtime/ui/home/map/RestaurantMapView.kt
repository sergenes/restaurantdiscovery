package com.nes.lunchtime.ui.home.map

import com.nes.lunchtime.R
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.ui.components.RestaurantCard
import com.nes.lunchtime.ui.theme.Dimens
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.nes.lunchtime.ui.theme.LunchtimeTheme
import androidx.core.graphics.createBitmap

private const val DEFAULT_ZOOM = 12f
private const val MAP_PADDING = 100
private const val CAMERA_ANIMATION_DURATION = 1000
private const val MIN_ZOOM = 8f
private const val MAX_ZOOM = 18f

//https://github.com/googlemaps/android-maps-compose
@Composable
fun RestaurantMapView(
    restaurants: List<Restaurant>,
    currentLocation: LatLng,
    favorites: List<String>,
    onItemClicked: (Restaurant) -> Unit,
    onFavoriteClicked: (Restaurant) -> Unit,
    initialSelectedRestaurant: Restaurant? = null // Added for preview support
) {
    val context = LocalContext.current
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(initialSelectedRestaurant) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation, DEFAULT_ZOOM)
    }

    val mapProperties by remember {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = true,
                mapType = MapType.NORMAL,
                minZoomPreference = MIN_ZOOM,
                maxZoomPreference = MAX_ZOOM
            )
        )
    }

    val mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = true,
                mapToolbarEnabled = false
            )
        )
    }

    // Handle camera updates when restaurants list changes
    LaunchedEffect(restaurants) {
        if (restaurants.isNotEmpty()) {
            updateCameraToShowAllMarkers(
                restaurants = restaurants,
                currentLocation = currentLocation,
                cameraPositionState = cameraPositionState
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (LocalInspectionMode.current) {
            // Preview Placeholder
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Map View Placeholder\n(${restaurants.size} markers)",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = mapUiSettings
            ) {
                // Marker icons should be created inside the GoogleMap scope or after initialization
                // to avoid "IBitmapDescriptorFactory is not initialized" crash.
                val markerIcon = remember(context) {
                    createCustomMarkerIcon(context, R.mipmap.pins)
                }

                restaurants.forEach { restaurant ->
                    val markerPosition = LatLng(restaurant.latitude, restaurant.longitude)

                    MarkerInfoWindow(
                        state = MarkerState(
                            position = markerPosition
                        ),
                        title = restaurant.displayName,
                        icon = markerIcon,
                        draggable = false,
                        onClick = {
                            selectedRestaurant = if (selectedRestaurant?.id == restaurant.id) {
                                null
                            } else {
                                restaurant
                            }
                            false
                        }
                    )
                }
            }
        }

        selectedRestaurant?.let { restaurant ->
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = Dimens.SpacingSmall,
                        end = Dimens.SpacingSmall,
                        bottom = Dimens.MapCardBottomPadding
                    )
            ) {
                RestaurantCard(
                    restaurant = restaurant,
                    isFavorite = favorites.contains(restaurant.id),
                    onItemClicked = {
                        onItemClicked(it)
                    },
                    onFavoriteClicked = {
                        onFavoriteClicked(it)
                    }
                )
            }
        }
    }
}

private suspend fun updateCameraToShowAllMarkers(
    restaurants: List<Restaurant>,
    currentLocation: LatLng,
    cameraPositionState: CameraPositionState
) {
    val bounds = LatLngBounds.builder().apply {
        include(currentLocation) // Include current location
        restaurants.forEach {
            include(LatLng(it.latitude, it.longitude))
        }
    }.build()

    try {
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngBounds(
                bounds,
                MAP_PADDING
            ),
            durationMs = CAMERA_ANIMATION_DURATION
        )
    } catch (e: Exception) {
        // Handle camera update failure
        cameraPositionState.move(
            CameraUpdateFactory.newLatLngZoom(
                currentLocation,
                DEFAULT_ZOOM
            )
        )
    }
}

private fun createCustomMarkerIcon(
    context: Context,
    vectorResId: Int
): BitmapDescriptor {
    return try {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
            ?: return BitmapDescriptorFactory.defaultMarker()

        val bitmap = createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)

        android.graphics.Canvas(bitmap).apply {
            vectorDrawable.setBounds(0, 0, width, height)
            vectorDrawable.draw(this)
        }

        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        BitmapDescriptorFactory.defaultMarker()
    }
}

@Preview(showBackground = true)
@Composable
fun RestaurantMapViewPreview() {
    LunchtimeTheme {
        RestaurantMapView(
            restaurants = sampleRestaurants,
            currentLocation = LatLng(37.7749, -122.4194),
            favorites = listOf("1"),
            onItemClicked = {},
            onFavoriteClicked = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RestaurantMapViewWithSelectedPreview() {
    LunchtimeTheme {
        RestaurantMapView(
            restaurants = sampleRestaurants,
            currentLocation = LatLng(37.7749, -122.4194),
            favorites = listOf("1"),
            onItemClicked = {},
            onFavoriteClicked = {},
            initialSelectedRestaurant = sampleRestaurants[0]
        )
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
