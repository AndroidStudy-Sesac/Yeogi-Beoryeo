package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import javax.inject.Inject

class ResolveRegionFromKeywordUseCase @Inject constructor(
    private val repository: RegionRepository
) {
    suspend operator fun invoke(keyword: String): Region? {
        return repository.resolveRegionFromKeyword(keyword)
    }
}
