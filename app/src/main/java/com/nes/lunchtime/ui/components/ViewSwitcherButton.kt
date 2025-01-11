package com.nes.lunchtime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nes.lunchtime.ui.home.ViewType
import com.nes.lunchtime.ui.theme.LunchtimeTheme

@Composable
fun ViewSwitcherButton(
    currentViewType: ViewType,
    onViewTypeChange: (ViewType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colorScheme.primary)
            .clickable {
                onViewTypeChange(
                    when (currentViewType) {
                        ViewType.ListView -> ViewType.MapView
                        ViewType.MapView -> ViewType.ListView
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        currentViewType.icon?.let {
            Icon(
                imageVector = it,
                contentDescription = currentViewType.title,
                tint = colorScheme.onPrimary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = currentViewType.title,
            color = colorScheme.onPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ViewSwitcherPreview() {
    var currentViewType by remember { mutableStateOf<ViewType>(ViewType.ListView) }
    LunchtimeTheme {
        ViewSwitcherButton(
            currentViewType = currentViewType,
            onViewTypeChange = { currentViewType = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}