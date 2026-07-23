package com.team.yeogibeoryeo.navigation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.team.yeogibeoryeo.R
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType

@Composable
internal fun rememberCtaPreconditionState(
    onOpenMap: (CollectionSpotType) -> Unit,
    onOpenExternalUrl: (String) -> Boolean,
): CtaPreconditionState {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val currentOnOpenMap by rememberUpdatedState(onOpenMap)
    val currentOnOpenExternalUrl by rememberUpdatedState(onOpenExternalUrl)

    return remember(applicationContext) {
        CtaPreconditionState(
            isInternetAvailable = applicationContext::hasValidatedInternetConnection,
            hasFineLocationPermission = applicationContext::hasFineLocationPermission,
            isLocationServiceEnabled = applicationContext::isLocationServiceEnabled,
            onOpenMap = { type -> currentOnOpenMap(type) },
            onOpenExternalUrl = { url -> currentOnOpenExternalUrl(url) },
        )
    }
}

@Composable
internal fun CtaPreconditionDialogHost(state: CtaPreconditionState) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        state.onLocationPermissionResult(
            isFineLocationGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true,
            isCoarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
            canRequestAgain = activity?.let { currentActivity ->
                ActivityCompat.shouldShowRequestPermissionRationale(
                    currentActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                )
            } == true,
        )
    }

    DisposableEffect(lifecycle, state) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.onResumeFromSettings()
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    val dialog = state.dialog ?: return
    val spec = dialog.toSpec()

    AlertDialog(
        onDismissRequest = state::cancelPendingRequest,
        title = {
            Text(text = stringResource(spec.titleResId))
        },
        text = {
            Text(text = stringResource(spec.messageResId))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    state.confirmDialog()?.let { effect ->
                        when (effect) {
                            CtaPreconditionEffect.RequestLocationPermission -> {
                                permissionLauncher.launch(LOCATION_PERMISSIONS)
                            }

                            CtaPreconditionEffect.OpenAppSettings -> {
                                if (!context.openAppSettings()) {
                                    state.onSettingsOpenFailed()
                                }
                            }

                            CtaPreconditionEffect.OpenLocationSettings -> {
                                if (!context.openLocationSettings()) {
                                    state.onSettingsOpenFailed()
                                }
                            }
                        }
                    }
                },
            ) {
                Text(text = stringResource(spec.confirmResId))
            }
        },
        dismissButton = spec.dismissResId?.let { dismissResId ->
            {
                TextButton(onClick = state::cancelPendingRequest) {
                    Text(text = stringResource(dismissResId))
                }
            }
        },
    )
}

private fun CtaPreconditionDialog.toSpec(): CtaPreconditionDialogSpec = when (this) {
    CtaPreconditionDialog.MapInternetRequired -> CtaPreconditionDialogSpec(
        titleResId = R.string.cta_precondition_map_internet_title,
        messageResId = R.string.cta_precondition_map_internet_message,
        confirmResId = R.string.cta_precondition_retry,
        dismissResId = R.string.cta_precondition_cancel,
    )

    CtaPreconditionDialog.ExternalUrlInternetRequired -> CtaPreconditionDialogSpec(
        titleResId = R.string.cta_precondition_url_internet_title,
        messageResId = R.string.cta_precondition_url_internet_message,
        confirmResId = R.string.cta_precondition_retry,
        dismissResId = R.string.cta_precondition_cancel,
    )

    CtaPreconditionDialog.LocationPermissionRationale -> CtaPreconditionDialogSpec(
        titleResId = R.string.cta_precondition_location_permission_title,
        messageResId = R.string.cta_precondition_location_permission_message,
        confirmResId = R.string.cta_precondition_continue,
        dismissResId = R.string.cta_precondition_cancel,
    )

    CtaPreconditionDialog.LocationPermissionDenied -> CtaPreconditionDialogSpec(
        titleResId = R.string.cta_precondition_location_permission_title,
        messageResId = R.string.cta_precondition_location_permission_denied_message,
        confirmResId = R.string.cta_precondition_request_again,
        dismissResId = R.string.cta_precondition_cancel,
    )

    CtaPreconditionDialog.LocationPermissionSettings -> CtaPreconditionDialogSpec(
        titleResId = R.string.cta_precondition_location_permission_settings_title,
        messageResId = R.string.cta_precondition_location_permission_settings_message,
        confirmResId = R.string.cta_precondition_open_app_settings,
        dismissResId = R.string.cta_precondition_cancel,
    )

    CtaPreconditionDialog.PreciseLocationSettings -> CtaPreconditionDialogSpec(
        titleResId = R.string.cta_precondition_precise_location_title,
        messageResId = R.string.cta_precondition_precise_location_message,
        confirmResId = R.string.cta_precondition_open_app_settings,
        dismissResId = R.string.cta_precondition_cancel,
    )

    CtaPreconditionDialog.LocationServiceDisabled -> CtaPreconditionDialogSpec(
        titleResId = R.string.cta_precondition_location_service_title,
        messageResId = R.string.cta_precondition_location_service_message,
        confirmResId = R.string.cta_precondition_open_location_settings,
        dismissResId = R.string.cta_precondition_cancel,
    )

    CtaPreconditionDialog.ExternalUrlOpenFailed -> CtaPreconditionDialogSpec(
        titleResId = R.string.cta_precondition_url_open_failed_title,
        messageResId = R.string.cta_precondition_url_open_failed_message,
        confirmResId = R.string.cta_precondition_confirm,
        dismissResId = null,
    )
}

private data class CtaPreconditionDialogSpec(
    @param:StringRes val titleResId: Int,
    @param:StringRes val messageResId: Int,
    @param:StringRes val confirmResId: Int,
    @param:StringRes val dismissResId: Int?,
)

private fun Context.hasValidatedInternetConnection(): Boolean {
    val connectivityManager = getSystemService(ConnectivityManager::class.java)
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private fun Context.hasFineLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED

private fun Context.isLocationServiceEnabled(): Boolean =
    getSystemService(LocationManager::class.java).isLocationEnabled

private fun Context.openAppSettings(): Boolean {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )
    return runCatching { startActivity(intent) }.isSuccess
}

private fun Context.openLocationSettings(): Boolean =
    runCatching {
        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }.isSuccess

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)
