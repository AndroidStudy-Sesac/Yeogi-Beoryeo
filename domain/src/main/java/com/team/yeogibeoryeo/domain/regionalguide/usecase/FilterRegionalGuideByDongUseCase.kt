package com.team.yeogibeoryeo.domain.regionalguide.usecase

import kotlin.collections.filter

import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import javax.inject.Inject

/**
 * 행정동/관리구역명을 기준으로 지역별 배출 가이드 목록을 필터링하는 UseCase
 */
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