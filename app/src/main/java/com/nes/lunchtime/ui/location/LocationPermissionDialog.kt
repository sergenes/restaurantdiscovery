package com.nes.lunchtime.ui.location

import android.Manifest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect

@Composable
fun LocationPermissionDialog(
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onGranted()
            } else {
                showRationale = true
            }
        }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (showRationale) {
        RationaleDialog(
            onConfirm = {
                showRationale = false
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = {
                showRationale = false
                onDenied()
            }
        )
    }
}

@Composable
fun LocationPermissionDeniedDialog(
    onGranted: () -> Unit,
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onGranted()
            } else {
                showRationale = true
            }
        }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    if (showRationale) {
        PermissionDeniedDialog(
            onGoToSettings = {
                onGoToSettings()
                showRationale = false},
            onDismiss = {
                showRationale = false
                onDismiss()
            }
        )
    }
}

@Composable
private fun RationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Location Access Needed",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "This app needs access to location to show you nearby restaurants. " +
                        "Without location permission, we can't provide personalized recommendations.",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Text("Grant Access")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Not Now")
            }
        }
    )
}

@Composable
private fun PermissionDeniedDialog(
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Permission Required",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "Location permission is permanently denied. Please enable it in app settings to use this feature.",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onGoToSettings
            ) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}