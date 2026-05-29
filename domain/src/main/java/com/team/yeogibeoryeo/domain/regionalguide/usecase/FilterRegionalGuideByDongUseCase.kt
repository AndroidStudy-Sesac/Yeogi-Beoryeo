package com.team.yeogibeoryeo.domain.regionalguide.usecase

import kotlin.collections.filter

import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import javax.inject.Inject

class FilterRegionalGuideByDongUseCase @Inject constructor() {

    operator fun invoke(
        guides: List<RegionalDisposalGuide>,
        dongName: String?
    ): List<RegionalDisposalGuide> {

        if (dongName.isNullOrBlank()) {
            return guides
        }

        return guides.filter { guide ->
            val eupmyeondong = guide.region.eupmyeondong

            eupmyeondong?.contains(
                other = dongName,
                ignoreCase = true
            ) == true
        }
    }
}
