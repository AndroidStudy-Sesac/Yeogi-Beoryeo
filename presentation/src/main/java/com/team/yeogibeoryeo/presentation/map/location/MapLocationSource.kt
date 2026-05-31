package com.team.yeogibeoryeo.presentation.map.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.naver.maps.map.LocationSource

@Composable
fun rememberMapLocationSource(): LocationSource {
    val context = LocalContext.current.applicationContext

    return remember(context) {
        PlayServicesMapLocationSource(context)
    }
}

private class PlayServicesMapLocationSource(
    private val context: Context,
) : LocationSource {
    private val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun activate(listener: LocationSource.OnLocationChangedListener) {
        deactivate()

        if (!context.hasFineLocationPermission()) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_UPDATE_INTERVAL_MILLIS,
        ).setMinUpdateIntervalMillis(
            LOCATION_MIN_UPDATE_INTERVAL_MILLIS,
        ).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(listener::onLocationChanged)
            }
        }

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper(),
            )
            locationCallback = callback
        } catch (exception: SecurityException) {
            locationCallback = null
        }
    }

    override fun deactivate() {
        locationCallback?.let(fusedLocationProviderClient::removeLocationUpdates)
        locationCallback = null
    }
}

private fun Context.hasFineLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
}

private const val LOCATION_UPDATE_INTERVAL_MILLIS = 3_000L
private const val LOCATION_MIN_UPDATE_INTERVAL_MILLIS = 1_000L
