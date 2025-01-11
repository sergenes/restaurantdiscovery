package com.nes.lunchtime.ui.home.map

import com.nes.lunchtime.R
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.ui.components.RestaurantCard
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*

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
    onFavoriteClicked: (Restaurant) -> Unit
) {
    val context = LocalContext.current
    var selectedRestaurant by remember { mutableStateOf<Restaurant?>(null) }

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
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        ) {
            val icon = createCustomMarkerIcon(
                context,
                R.mipmap.pins
            )
            restaurants.forEach { restaurant ->
                val markerPosition = LatLng(restaurant.latitude, restaurant.longitude)

                MarkerInfoWindow(
                    state = MarkerState(
                        position = markerPosition
                    ),
                    title = restaurant.displayName,
                    icon = icon,
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

        selectedRestaurant?.let { restaurant ->
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 8.dp, bottom = 230.dp)
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

        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        android.graphics.Canvas(bitmap).apply {
            vectorDrawable.setBounds(0, 0, width, height)
            vectorDrawable.draw(this)
        }

        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        BitmapDescriptorFactory.defaultMarker()
    }
}