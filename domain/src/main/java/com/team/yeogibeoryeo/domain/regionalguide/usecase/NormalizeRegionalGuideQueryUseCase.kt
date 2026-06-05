package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
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
            sigunguQuery = sigungu.toInfoSigunguQuery()
        )
    }

    private fun String.toInfoSigunguQuery(): String {
        val firstToken = split(Regex("\\s+"))
            .firstOrNull()
            ?.takeIf { token -> token.endsWith("시") }

        if (firstToken != null) return firstToken

        val cityIndex = indexOf("시")
        val guIndex = indexOf("구")

        return if (cityIndex > 0 && guIndex > cityIndex) {
            substring(startIndex = 0, endIndex = cityIndex + 1)
        } else {
            this
        }
    }

    private companion object {
        const val SEJONG_SIDO = "세종특별자치시"
        const val SEJONG_SIGUNGU_QUERY = "없음"
    }
}
