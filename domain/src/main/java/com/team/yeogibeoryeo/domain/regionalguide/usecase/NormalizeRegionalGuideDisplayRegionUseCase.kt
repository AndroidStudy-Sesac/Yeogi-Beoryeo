package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.usecase.NormalizeRegionForRegionalGuideUseCase
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLegacyRegionCompatibilityPolicy
import javax.inject.Inject

class NormalizeRegionalGuideDisplayRegionUseCase @Inject constructor(
    private val normalizeRegionForRegionalGuideUseCase: NormalizeRegionForRegionalGuideUseCase
) {
    suspend operator fun invoke(region: Region): Region {
        if (RegionalGuideLegacyRegionCompatibilityPolicy.shouldPreserveDisplayRegion(region)) {
            return region
        }

        return normalizeRegionForRegionalGuideUseCase(region)
    }
}
