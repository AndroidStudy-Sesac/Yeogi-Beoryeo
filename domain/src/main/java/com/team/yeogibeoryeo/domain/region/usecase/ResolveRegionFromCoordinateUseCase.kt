package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import javax.inject.Inject

class ResolveRegionFromCoordinateUseCase @Inject constructor(
    private val repository: RegionRepository
) {
    suspend operator fun invoke(latitude: Double, longitude: Double): Region? {
        return repository.resolveRegionFromCoordinate(latitude, longitude)
    }
}
