package com.nes.lunchtime.ui.components

import com.nes.lunchtime.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.nes.lunchtime.domain.Restaurant
import com.nes.lunchtime.ui.theme.Dimens
import com.nes.lunchtime.ui.theme.LunchtimeTheme

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
            .padding(horizontal = Dimens.CardPaddingHorizontal, vertical = Dimens.CardPaddingVertical)
            .shadow(Dimens.CardElevation, RoundedCornerShape(Dimens.CardCornerRadius))
            .clip(RoundedCornerShape(Dimens.CardCornerRadius))
            .clickable { onItemClicked(restaurant) },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .padding(Dimens.SpacingMedium)
                .fillMaxWidth()
                .height(Dimens.CardHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RestaurantImage(
                photoUrl = restaurant.photoUrl,
                contentDescription = "Photo of ${restaurant.displayName}",
                modifier = Modifier
                    .fillMaxHeight()
                    .width(Dimens.CardImageWidth)
            )

            Spacer(modifier = Modifier.width(Dimens.SpacingMedium))
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = restaurant.displayName,
                    style = typography.titleMedium.copy(fontSize = Dimens.TextSizeMedium),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(android.R.drawable.btn_star_big_on),
                        contentDescription = "Rating Star",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.IconSizeSmall)
                    )
                    Text(
                        text = "${restaurant.rating} • ${restaurant.userRatingCount} reviews",
                        style = typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = Dimens.SpacingXSmall)
                    )
                }
                Spacer(modifier = Modifier.height(Dimens.SpacingXSmall))
                Text(
                    text = restaurant.formattedAddress,
                    style = typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.fillMaxHeight()
            ) {
                IconButton(
                    modifier = Modifier
                        .height(Dimens.IconButtonHeight)
                        .width(Dimens.IconSizeButton),
                    onClick = { onFavoriteClicked(restaurant) }
                ) {
                    Icon(
                        painter = painterResource(
                            if (isFavorite) R.mipmap.saved
                            else R.mipmap.bookmark
                        ),
                        contentDescription = "Favorite Icon",
                        tint = MaterialTheme.colorScheme.primary
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
        id = "id",
        displayName = "Name Very Long Too Long to fit and more",
        formattedAddress = "123 Address St, City",
        latitude = 0.0,
        longitude = 0.0,
        rating = 3.5,
        userRatingCount = 100,
        photoUrl = ""
    )
    var isFavorite by remember { mutableStateOf(false) }

    LunchtimeTheme {
        RestaurantCard(
            restaurant = item,
            isFavorite = isFavorite,
            onItemClicked = {},
            onFavoriteClicked = { isFavorite = !isFavorite }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRestaurantCardFavorite() {
    val item = Restaurant(
        id = "id",
        displayName = "Italian Bistro",
        formattedAddress = "456 Pasta Ave, City",
        latitude = 0.0,
        longitude = 0.0,
        rating = 4.8,
        userRatingCount = 250,
        photoUrl = ""
    )

    LunchtimeTheme {
        RestaurantCard(
            restaurant = item,
            isFavorite = true,
            onItemClicked = {},
            onFavoriteClicked = {}
        )
    }
}
