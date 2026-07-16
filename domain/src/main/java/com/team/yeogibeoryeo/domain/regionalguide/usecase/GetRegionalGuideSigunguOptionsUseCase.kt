package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import javax.inject.Inject

class GetRegionalGuideSigunguOptionsUseCase @Inject constructor(
    private val regionOptionsRepository: RegionOptionsRepository,
) {

    suspend operator fun invoke(sido: String): List<String> {
        return regionOptionsRepository.getRegionalGuideSigunguOptions(sido)
    }
}
