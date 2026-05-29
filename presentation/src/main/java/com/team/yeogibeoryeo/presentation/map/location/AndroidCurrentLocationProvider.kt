package com.team.yeogibeoryeo.presentation.map.location

import android.annotation.SuppressLint
import com.google.android.gms.location.FusedLocationProviderClient
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidCurrentLocationProvider @Inject constructor(
    private val fusedLocationProviderClient: FusedLocationProviderClient,
    private val locationPermissionChecker: LocationPermissionChecker,
) : CurrentLocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): CurrentLocationResult {
        if (!locationPermissionChecker.hasFineLocationPermission()) {
            return CurrentLocationResult.PermissionDenied
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
}
