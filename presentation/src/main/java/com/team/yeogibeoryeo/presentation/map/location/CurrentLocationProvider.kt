package com.team.yeogibeoryeo.presentation.map.location

import com.team.yeogibeoryeo.domain.spot.model.Coordinate

interface CurrentLocationProvider {
    suspend fun getCurrentLocation(): CurrentLocationResult
}

sealed interface CurrentLocationResult {
    data class Found(
        val coordinate: Coordinate,
    ) : CurrentLocationResult

    data object PermissionDenied : CurrentLocationResult

    data object LocationServiceDisabled : CurrentLocationResult

    data object NotFound : CurrentLocationResult
}
