package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import javax.inject.Inject

class NormalizeRegionForRegionalGuideUseCase @Inject constructor(
    private val repository: RegionOptionsRepository
) {
    suspend operator fun invoke(region: Region): Region {
        return repository.normalizeRegionForRegionalGuide(region)
    }
}
