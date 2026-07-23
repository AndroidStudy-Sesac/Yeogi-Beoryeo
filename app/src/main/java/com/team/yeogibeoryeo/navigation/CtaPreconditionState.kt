package com.team.yeogibeoryeo.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType

@Stable
internal class CtaPreconditionState(
    private val isInternetAvailable: () -> Boolean,
    private val hasFineLocationPermission: () -> Boolean,
    private val hasCoarseLocationPermission: () -> Boolean,
    private val isLocationServiceEnabled: () -> Boolean,
    private val onOpenMap: (CollectionSpotType) -> Unit,
    private val onOpenExternalUrl: (String) -> Boolean,
) {
    var dialog: CtaPreconditionDialog? by mutableStateOf(null)
        private set

    private var pendingRequest: PendingCtaRequest? = null
    private var waitingForSettings: SettingsDestination? = null
    private var requiresAppSettings = false
    private var preciseUpgradeRequested = false

    fun requestMap(type: CollectionSpotType) {
        pendingRequest = PendingCtaRequest.Map(type)
        proceed()
    }

    fun requestExternalUrl(url: String): Boolean {
        pendingRequest = PendingCtaRequest.ExternalUrl(url)
        return proceed()
    }

    fun confirmDialog(): CtaPreconditionEffect? = when (dialog) {
        CtaPreconditionDialog.MapInternetRequired,
        CtaPreconditionDialog.ExternalUrlInternetRequired,
        -> {
            dialog = null
            if (!proceed()) {
                dialog = CtaPreconditionDialog.ExternalUrlOpenFailed
            }
            null
        }

        CtaPreconditionDialog.LocationPermissionRationale,
        CtaPreconditionDialog.LocationPermissionDenied,
        CtaPreconditionDialog.PreciseLocationDenied,
        -> {
            dialog = null
            CtaPreconditionEffect.RequestLocationPermission
        }

        CtaPreconditionDialog.PreciseLocationRationale -> {
            dialog = null
            preciseUpgradeRequested = true
            CtaPreconditionEffect.RequestLocationPermission
        }

        CtaPreconditionDialog.LocationPermissionSettings,
        CtaPreconditionDialog.PreciseLocationSettings,
        -> {
            dialog = null
            waitingForSettings = SettingsDestination.AppPermission
            CtaPreconditionEffect.OpenAppSettings
        }

        CtaPreconditionDialog.LocationServiceDisabled -> {
            dialog = null
            waitingForSettings = SettingsDestination.LocationService
            CtaPreconditionEffect.OpenLocationSettings
        }

        CtaPreconditionDialog.ExternalUrlOpenFailed -> {
            cancelPendingRequest()
            null
        }

        null -> null
    }

    fun onLocationPermissionResult(
        isFineLocationGranted: Boolean,
        isCoarseLocationGranted: Boolean,
        canRequestAgain: Boolean,
    ) {
        when {
            isFineLocationGranted -> {
                requiresAppSettings = false
                preciseUpgradeRequested = false
                proceed()
            }

            isCoarseLocationGranted -> {
                dialog = when {
                    !preciseUpgradeRequested -> CtaPreconditionDialog.PreciseLocationRationale
                    canRequestAgain -> CtaPreconditionDialog.PreciseLocationDenied
                    else -> CtaPreconditionDialog.PreciseLocationSettings
                }
            }

            canRequestAgain -> {
                dialog = CtaPreconditionDialog.LocationPermissionDenied
            }

            else -> {
                requiresAppSettings = true
                dialog = CtaPreconditionDialog.LocationPermissionSettings
            }
        }
    }

    fun onResumeFromSettings() {
        when (waitingForSettings) {
            SettingsDestination.AppPermission -> {
                waitingForSettings = null
                if (hasFineLocationPermission()) {
                    requiresAppSettings = false
                    proceed()
                } else {
                    cancelPendingRequest()
                }
            }

            SettingsDestination.LocationService -> {
                waitingForSettings = null
                if (isLocationServiceEnabled()) {
                    proceed()
                } else {
                    cancelPendingRequest()
                }
            }

            null -> Unit
        }
    }

    fun onSettingsOpenFailed() {
        cancelPendingRequest()
    }

    fun cancelPendingRequest() {
        pendingRequest = null
        waitingForSettings = null
        dialog = null
        preciseUpgradeRequested = false
    }

    private fun proceed(): Boolean {
        val request = pendingRequest ?: return true

        if (!isInternetAvailable()) {
            dialog = when (request) {
                is PendingCtaRequest.Map -> CtaPreconditionDialog.MapInternetRequired
                is PendingCtaRequest.ExternalUrl ->
                    CtaPreconditionDialog.ExternalUrlInternetRequired
            }
            return true
        }

        return when (request) {
            is PendingCtaRequest.Map -> proceedToMap(request)
            is PendingCtaRequest.ExternalUrl -> proceedToExternalUrl(request)
        }
    }

    private fun proceedToMap(request: PendingCtaRequest.Map): Boolean {
        if (!hasFineLocationPermission()) {
            dialog = when {
                hasCoarseLocationPermission() -> CtaPreconditionDialog.PreciseLocationRationale
                requiresAppSettings -> CtaPreconditionDialog.LocationPermissionSettings
                else -> CtaPreconditionDialog.LocationPermissionRationale
            }
            return true
        }

        if (!isLocationServiceEnabled()) {
            dialog = CtaPreconditionDialog.LocationServiceDisabled
            return true
        }

        pendingRequest = null
        dialog = null
        onOpenMap(request.type)
        return true
    }

    private fun proceedToExternalUrl(request: PendingCtaRequest.ExternalUrl): Boolean {
        val opened = onOpenExternalUrl(request.url)
        pendingRequest = null
        dialog = null
        return opened
    }
}

internal enum class CtaPreconditionDialog {
    MapInternetRequired,
    ExternalUrlInternetRequired,
    LocationPermissionRationale,
    LocationPermissionDenied,
    LocationPermissionSettings,
    PreciseLocationRationale,
    PreciseLocationDenied,
    PreciseLocationSettings,
    LocationServiceDisabled,
    ExternalUrlOpenFailed,
}

internal sealed interface CtaPreconditionEffect {
    data object RequestLocationPermission : CtaPreconditionEffect
    data object OpenAppSettings : CtaPreconditionEffect
    data object OpenLocationSettings : CtaPreconditionEffect
}

private sealed interface PendingCtaRequest {
    data class Map(val type: CollectionSpotType) : PendingCtaRequest
    data class ExternalUrl(val url: String) : PendingCtaRequest
}

private enum class SettingsDestination {
    AppPermission,
    LocationService,
}
