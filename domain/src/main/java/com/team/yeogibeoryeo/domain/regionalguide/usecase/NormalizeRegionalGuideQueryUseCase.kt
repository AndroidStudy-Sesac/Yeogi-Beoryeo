package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideRegionKeyNormalizer
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import javax.inject.Inject

class NormalizeRegionalGuideQueryUseCase @Inject constructor() {

    operator fun invoke(region: Region): RegionalGuideQuery? {
        if (region.sido == SEJONG_SIDO) {
            return RegionalGuideQuery(
                displayRegion = region.copy(sigungu = null),
                sigunguQuery = SEJONG_SIGUNGU_QUERY
            )
        }

        val sigungu = region.sigungu
            ?.trim()
            ?.takeIf { sigungu -> sigungu.isNotBlank() }
            ?: return null

        return RegionalGuideQuery(
            displayRegion = region,
            sigunguQuery = RegionalGuideRegionKeyNormalizer.normalizeSigungu(sigungu)
        )
    }

    private companion object {
        const val SEJONG_SIDO = "세종특별자치시"
        const val SEJONG_SIGUNGU_QUERY = "없음"
    }
}
