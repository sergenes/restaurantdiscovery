package com.nes.lunchtime.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.nes.lunchtime.R
import com.nes.lunchtime.ui.theme.LunchtimeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandedAppHeader() {
    TopAppBar(
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_lockup),
                    contentDescription = "Lunchtime restaurant discovery app logo"
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun BrandedAppHeaderPreview() {
    LunchtimeTheme {
        BrandedAppHeader()
    }
}
