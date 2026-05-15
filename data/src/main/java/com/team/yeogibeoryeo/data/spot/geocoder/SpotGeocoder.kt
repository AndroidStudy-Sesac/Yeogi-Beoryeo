package com.team.yeogibeoryeo.data.spot.geocoder

import androidx.annotation.RequiresApi
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

interface SpotGeocoder {
    suspend fun geocode(address: String): Coordinate?
}

class AndroidSpotGeocoder @Inject constructor(
    @ApplicationContext private val context: Context,
) : SpotGeocoder {

    override suspend fun geocode(address: String): Coordinate? {
        if (address.isBlank()) return null

        val geocoder = Geocoder(context, Locale.KOREA)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocodeForTiramisuAndAbove(
                geocoder = geocoder,
                address = address,
            )
        } else {
            geocodeForBelowTiramisu(
                geocoder = geocoder,
                address = address,
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun geocodeForTiramisuAndAbove(
        geocoder: Geocoder,
        address: String,
    ): Coordinate? {
        return suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocationName(
                address,
                MAX_RESULT_COUNT,
            ) { addresses ->
                val firstAddress = addresses.firstOrNull()

                continuation.resume(
                    firstAddress?.let {
                        Coordinate(
                            latitude = it.latitude,
                            longitude = it.longitude,
                        )
                    },
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun geocodeForBelowTiramisu(
        geocoder: Geocoder,
        address: String,
    ): Coordinate? {
        return runCatching {
            geocoder.getFromLocationName(
                address,
                MAX_RESULT_COUNT,
            )?.firstOrNull()?.let {
                Coordinate(
                    latitude = it.latitude,
                    longitude = it.longitude,
                )
            }
        }.getOrNull()
    }

    private companion object {
        const val MAX_RESULT_COUNT = 1
    }
}