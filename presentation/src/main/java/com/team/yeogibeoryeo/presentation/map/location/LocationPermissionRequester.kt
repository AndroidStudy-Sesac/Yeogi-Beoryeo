package com.team.yeogibeoryeo.presentation.map.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun rememberCurrentLocationSearchRequester(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val currentOnGranted by rememberUpdatedState(onGranted)
    val currentOnDenied by rememberUpdatedState(onDenied)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            currentOnGranted()
        } else {
            currentOnDenied()
        }
    }

    return remember(launcher) {
        {
            if (context.hasFineLocationPermission()) {
                currentOnGranted()
            } else {
                launcher.launch(LOCATION_PERMISSIONS)
            }
        }
    }
}

@Composable
fun rememberFineLocationPermissionGranted(): Boolean {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var isGranted by remember {
        mutableStateOf(context.hasFineLocationPermission())
    }

    DisposableEffect(context, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = context.hasFineLocationPermission()
            }
        }

        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    return isGranted
}

private fun Context.hasFineLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
}

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)
