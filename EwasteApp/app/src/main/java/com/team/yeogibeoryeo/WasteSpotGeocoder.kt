package com.team.yeogibeoryeo

import android.content.Context
import android.location.Geocoder
import android.util.Log
import java.util.Locale

fun geocodeWasteSpotsWithAndroid(
    context: Context,
    spots: List<WasteSpot>
): List<WasteSpot> {
    val geocoder = Geocoder(context, Locale.KOREA)

    return spots.map { spot ->
        try {
            val results = geocoder.getFromLocationName(spot.address, 1)
            val first = results?.firstOrNull()

            if (first != null) {
                spot.copy(
                    latitude = first.latitude,
                    longitude = first.longitude
                )
            } else {
                Log.w("WasteSpotGeocoder", "좌표 변환 실패: ${spot.address}")
                spot
            }
        } catch (e: Exception) {
            Log.e("WasteSpotGeocoder", "Geocoder 오류: ${spot.address}", e)
            spot
        }
    }
}