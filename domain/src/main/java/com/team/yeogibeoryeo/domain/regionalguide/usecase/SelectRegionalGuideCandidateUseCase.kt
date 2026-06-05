package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import javax.inject.Inject

class SelectRegionalGuideCandidateUseCase @Inject constructor() {

    operator fun invoke(
        candidates: List<RegionalDisposalGuide>,
        query: RegionalGuideQuery
    ): RegionalGuideLookupResult {
        if (candidates.isEmpty()) return RegionalGuideLookupResult.NotFound

        val filteredCandidates = candidates
            .filterBySido(query.displayRegion.sido)
            .filterBySigungu(query.sigunguQuery)

        if (filteredCandidates.isEmpty()) {
            return RegionalGuideLookupResult.CandidateNotFound
        }

        if (filteredCandidates.size == 1) {
            return RegionalGuideLookupResult.Success(
                guide = filteredCandidates.first().withDisplayRegion(query.displayRegion)
            )
        }

        val selectedGuide = selectByTargetRegion(
            candidates = filteredCandidates,
            requestedRegion = query.displayRegion,
            sigunguQuery = query.sigunguQuery
        ) ?: return filteredCandidates.toCandidateResultOrNotFound(query.displayRegion)

        return RegionalGuideLookupResult.Success(
            guide = selectedGuide.withDisplayRegion(query.displayRegion)
        )
    }

    private fun List<RegionalDisposalGuide>.filterBySido(
        sido: String?
    ): List<RegionalDisposalGuide> {
        if (sido.isNullOrBlank()) return this

        return filter { guide -> guide.region.sido == sido }
    }

    private fun List<RegionalDisposalGuide>.filterBySigungu(
        sigunguQuery: String
    ): List<RegionalDisposalGuide> {
        if (sigunguQuery == SEJONG_SIGUNGU_QUERY) return this

        return filter { guide -> guide.region.sigungu == sigunguQuery }
    }

    private fun selectByTargetRegion(
        candidates: List<RegionalDisposalGuide>,
        requestedRegion: Region,
        sigunguQuery: String
    ): RegionalDisposalGuide? {
        val eupmyeondong = requestedRegion.eupmyeondong?.trim()

        if (!eupmyeondong.isNullOrBlank()) {
            return candidates.firstOrNull { guide ->
                guide.targetRegionName.matchesEupmyeondong(eupmyeondong)
            } ?: candidates.firstOrNull { guide ->
                guide.targetRegionName.isSejongDongArea() &&
                    eupmyeondong in SEJONG_DONG_AREA_NAMES
            } ?: candidates.firstOrNull { guide ->
                guide.targetRegionName.isOverallTarget(sigunguQuery)
            }
        }

        return null
    }

    private fun List<RegionalDisposalGuide>.toCandidateResultOrNotFound(
        displayRegion: Region
    ): RegionalGuideLookupResult {
        if (!displayRegion.eupmyeondong.isNullOrBlank() || size <= 1) {
            return RegionalGuideLookupResult.CandidateNotFound
        }

        return RegionalGuideLookupResult.Candidates(
            guides = map { guide -> guide.withDisplayRegion(displayRegion) }
        )
    }

    private fun String?.matchesEupmyeondong(
        eupmyeondong: String
    ): Boolean {
        val targetRegionName = this?.trim().orEmpty()
        if (targetRegionName.isBlank()) return false

        if (targetRegionName == eupmyeondong || targetRegionName.contains(eupmyeondong)) {
            return true
        }

        val normalizedEupmyeondong = eupmyeondong.removeAdministrativeSuffix()

        return targetRegionName
            .split(TARGET_REGION_DELIMITER)
            .map { token -> token.trim().removeAdministrativeSuffix() }
            .any { token -> token == normalizedEupmyeondong }
    }

    private fun String.removeAdministrativeSuffix(): String =
        removeSuffix(EUP)
            .removeSuffix(MYEON)
            .removeSuffix(DONG)

    private fun RegionalDisposalGuide.withDisplayRegion(
        displayRegion: Region
    ): RegionalDisposalGuide {
        return copy(
            region = Region(
                sido = displayRegion.sido ?: region.sido,
                sigungu = displayRegion.sigungu ?: region.sigungu,
                eupmyeondong = displayRegion.eupmyeondong
            )
        )
    }

    private fun String?.isOverallTarget(
        sigunguQuery: String
    ): Boolean {
        val targetRegionName = this?.trim().orEmpty()

        if (targetRegionName.isBlank()) return true

        return targetRegionName == OVERALL_NONE ||
            targetRegionName == sigunguQuery ||
            targetRegionName.contains(OVERALL_ALL) ||
            targetRegionName.contains(OVERALL_WHOLE_AREA) ||
            targetRegionName.contains(OVERALL_WITHIN_REGION)
    }

    private fun String?.isSejongDongArea(): Boolean =
        this?.trim() == SEJONG_DONG_AREA

    private companion object {
        const val SEJONG_SIGUNGU_QUERY = "없음"
        const val SEJONG_DONG_AREA = "동지역"

        const val OVERALL_NONE = "없음"
        const val OVERALL_ALL = "전체"
        const val OVERALL_WHOLE_AREA = "전역"
        const val OVERALL_WITHIN_REGION = "관내"
        const val EUP = "읍"
        const val MYEON = "면"
        const val DONG = "동"

        val TARGET_REGION_DELIMITER = Regex("[,+/\\s]+")

        val SEJONG_DONG_AREA_NAMES = setOf(
            "한솔동",
            "새롬동",
            "나성동",
            "도담동",
            "어진동",
            "해밀동",
            "아름동",
            "종촌동",
            "고운동",
            "소담동",
            "반곡동",
            "보람동",
            "대평동",
            "다정동"
        )
    }
}
