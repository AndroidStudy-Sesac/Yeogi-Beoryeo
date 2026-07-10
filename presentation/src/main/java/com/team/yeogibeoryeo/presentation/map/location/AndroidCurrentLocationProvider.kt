package com.team.yeogibeoryeo.presentation.map.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidCurrentLocationProvider @Inject constructor(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val locationPermissionChecker: LocationPermissionChecker,
    @param:ApplicationContext private val context: Context,
) : CurrentLocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): CurrentLocationResult {
        if (!locationPermissionChecker.hasFineLocationPermission()) {
            return CurrentLocationResult.PermissionDenied
        }

        if (!context.isLocationServiceEnabled()) {
            return CurrentLocationResult.LocationServiceDisabled
        }

        return try {
            suspendCancellableCoroutine { continuation ->
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location ->
                        val result = location?.let {
                            CurrentLocationResult.Found(
                                coordinate = Coordinate(
                                    latitude = it.latitude,
                                    longitude = it.longitude,
                                ),
                            )
                        } ?: CurrentLocationResult.NotFound

                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(CurrentLocationResult.NotFound)
                        }
                    }
                    .addOnCanceledListener {
                        if (continuation.isActive) {
                            continuation.resume(CurrentLocationResult.NotFound)
                        }
                    }
            }
        } catch (exception: SecurityException) {
            CurrentLocationResult.PermissionDenied
        }
    }

    private fun Context.isLocationServiceEnabled(): Boolean {
        return runCatching {
            val locationManager = getSystemService(LocationManager::class.java)

            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }.getOrDefault(true)
    }
}
