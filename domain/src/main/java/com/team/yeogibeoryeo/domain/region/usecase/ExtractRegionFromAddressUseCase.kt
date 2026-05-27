package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import javax.inject.Inject

class ExtractRegionFromAddressUseCase @Inject constructor(
    private val repository: RegionRepository
) {
    operator fun invoke(address: String): Region? {
        return repository.extractRegionFromAddress(address)
    }
}
