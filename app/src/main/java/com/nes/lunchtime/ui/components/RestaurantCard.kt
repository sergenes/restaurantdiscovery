package com.nes.lunchtime.ui.components

import com.nes.lunchtime.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.decode.DataSource
import com.nes.lunchtime.domain.Restaurant

@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    isFavorite: Boolean,
    onItemClicked: (Restaurant) -> Unit,
    onFavoriteClicked: (Restaurant) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onItemClicked(restaurant) },
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .height(88.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(restaurant.photoUrl)
                    .allowHardware(false)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build(),
                onSuccess = { result ->
                    val successResult = result.result
                    when (successResult.dataSource) {
                        DataSource.MEMORY -> println("Image loaded from memory cache")
                        DataSource.DISK -> println("Image loaded from disk cache")
                        DataSource.NETWORK -> println("Image loaded from network")
                        else -> println("Image loaded from unknown source")
                    }
                },
                onError = { result ->
                    val errorResult = result.result
                    println("Error loading image: ${errorResult.throwable}")
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(64.dp),
                placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                error = painterResource(android.R.drawable.stat_notify_error)
            )

            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = restaurant.displayName,
                    style = typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(android.R.drawable.btn_star_big_on),
                        contentDescription = "Rating Star",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${restaurant.rating} • ${restaurant.userRatingCount} reviews",
                        style = typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = restaurant.formattedAddress,
                    style = typography.bodyMedium,
                    color = Color.Gray
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.fillMaxHeight()

            ) {
                IconButton(
                    modifier = Modifier
                        .height(30.dp)
                        .width(24.dp),
                    onClick = { onFavoriteClicked(restaurant) }) {
                    Icon(
                        painter = painterResource(
                            if (isFavorite) R.mipmap.saved
                            else R.mipmap.bookmark
                        ),
                        contentDescription = "Favorite Icon",
                        tint = Color(0xFF2C5601)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRestaurantCard() {
    val item = Restaurant(
        "id", "Name Very Long Too Long to fit and more",
        "Address",
        0.0,
        0.0,
        3.5,
        100,
        ""
    )
    var isFavorite by remember { mutableStateOf(false) }

    RestaurantCard(
        restaurant = item,
        isFavorite = isFavorite,
        onItemClicked = {},
        onFavoriteClicked = { isFavorite = !isFavorite }
    )
}
