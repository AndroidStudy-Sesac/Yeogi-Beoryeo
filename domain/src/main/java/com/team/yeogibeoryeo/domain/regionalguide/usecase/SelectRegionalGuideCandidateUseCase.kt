package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import javax.inject.Inject

class SelectRegionalGuideCandidateUseCase @Inject constructor() {

    operator fun invoke(
        candidates: List<RegionalDisposalGuide>,
        query: RegionalGuideQuery,
        preferredTargetRegionName: String? = null,
        preferredManagementZoneName: String? = null,
        mappedAdminDongCandidates: List<Region> = emptyList(),
    ): RegionalGuideLookupResult {
        if (candidates.isEmpty()) return RegionalGuideLookupResult.NotFound

        val filteredCandidates = candidates
            .filterBySido(query.displayRegion.sido)
            .filterBySigungu(query.sigunguQuery)
            .mergeDuplicateCandidateRows()

        if (filteredCandidates.isEmpty()) {
            return RegionalGuideLookupResult.CandidateNotFound
        }

        if (!preferredTargetRegionName.isNullOrBlank() || !preferredManagementZoneName.isNullOrBlank()) {
            return filteredCandidates
                .filterByPreferredCandidate(
                    preferredTargetRegionName = preferredTargetRegionName,
                    preferredManagementZoneName = preferredManagementZoneName
                )
                .toSingleSuccessOrCandidates(query.displayRegion)
                ?: RegionalGuideLookupResult.CandidateNotFound
        }

        if (
            filteredCandidates.size == 1 &&
            query.displayRegion.eupmyeondong.isNullOrBlank()
        ) {
            return filteredCandidates.toSingleSuccessOrCandidates(query.displayRegion)
                ?: RegionalGuideLookupResult.CandidateNotFound
        }

        filteredCandidates.selectOverallCandidates(query.sigunguQuery)
            .toSingleSuccessOrCandidates(query.displayRegion)
            ?.let { result -> return result }

        selectByTargetRegion(
            candidates = filteredCandidates,
            requestedRegion = query.displayRegion,
            sigunguQuery = query.sigunguQuery,
            mappedAdminDongCandidates = mappedAdminDongCandidates
        )?.let { result -> return result }

        return filteredCandidates.toCandidateResultOrNotFound(query.displayRegion)
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
        sigunguQuery: String,
        mappedAdminDongCandidates: List<Region>
    ): RegionalGuideLookupResult? {
        val eupmyeondong = requestedRegion.eupmyeondong?.trim()

        if (!eupmyeondong.isNullOrBlank()) {
            candidates
                .filterByRequestedEupmyeondong(eupmyeondong)
                .toSingleSuccessOrCandidates(requestedRegion)
                ?.let { result -> return result }

            candidates
                .filter { guide ->
                    guide.targetRegionName.isSejongDongArea() &&
                        eupmyeondong in SEJONG_DONG_AREA_NAMES
                }
                .toSingleSuccessOrCandidates(requestedRegion)
                ?.let { result -> return result }

            candidates
                .filterByMappedAdminDongs(mappedAdminDongCandidates)
                .toSingleSuccessOrCandidates(requestedRegion)
                ?.let { result -> return result }

            return candidates
                .selectOverallCandidates(sigunguQuery)
                .toSingleSuccessOrCandidates(requestedRegion)
        }

        return null
    }

    private fun List<RegionalDisposalGuide>.filterByRequestedEupmyeondong(
        eupmyeondong: String
    ): List<RegionalDisposalGuide> {
        val targetRegionMatches = filter { guide ->
            guide.targetRegionName.matchesEupmyeondong(eupmyeondong)
        }
        val managementZoneMatches = filter { guide ->
            guide.managementZoneName.isExactRegionName(eupmyeondong)
        }

        return (targetRegionMatches + managementZoneMatches)
            .mergeDuplicateCandidateRows()
    }

    private fun List<RegionalDisposalGuide>.filterByMappedAdminDongs(
        mappedAdminDongCandidates: List<Region>
    ): List<RegionalDisposalGuide> {
        val adminDongNames = mappedAdminDongCandidates
            .mapNotNull { region -> region.eupmyeondong.normalizeRegionName() }
            .toSet()

        if (adminDongNames.isEmpty()) return emptyList()

        return filter { guide ->
            guide.managementZoneName.matchesExactMappedAdminDong(adminDongNames) ||
                guide.targetRegionName.matchesExactMappedAdminDong(adminDongNames)
        }
            .mergeDuplicateCandidateRows()
    }

    private fun List<RegionalDisposalGuide>.selectOverallCandidates(
        sigunguQuery: String
    ): List<RegionalDisposalGuide> {
        val targetRegionNames = map { guide ->
            guide.targetRegionName
                ?.trim()
                .orEmpty()
        }.distinct()

        return if (
            targetRegionNames.size == 1 &&
            targetRegionNames.single().isOverallTarget(sigunguQuery)
        ) {
            this
        } else {
            emptyList()
        }
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

    private fun List<RegionalDisposalGuide>.toSingleSuccessOrCandidates(
        displayRegion: Region
    ): RegionalGuideLookupResult? {
        return when (size) {
            0 -> null
            1 -> RegionalGuideLookupResult.Success(
                guide = first().withDisplayRegion(displayRegion)
            )
            else -> RegionalGuideLookupResult.Candidates(
                guides = map { guide -> guide.withDisplayRegion(displayRegion) }
            )
        }
    }

    private fun List<RegionalDisposalGuide>.filterByPreferredCandidate(
        preferredTargetRegionName: String?,
        preferredManagementZoneName: String?
    ): List<RegionalDisposalGuide> {
        val targetRegionName = preferredTargetRegionName.normalizeRegionName()
        val managementZoneName = preferredManagementZoneName.normalizeRegionName()

        return filter { guide ->
            val targetMatches = targetRegionName == null ||
                guide.targetRegionName.normalizeRegionName() == targetRegionName
            val managementMatches = managementZoneName == null ||
                guide.managementZoneName.normalizeRegionName() == managementZoneName

            targetMatches && managementMatches
        }
    }

    private fun List<RegionalDisposalGuide>.mergeDuplicateCandidateRows(): List<RegionalDisposalGuide> =
        groupBy { guide -> guide.toCandidateKey() }
            .values
            .map { guides ->
                val firstGuide = guides.first()
                firstGuide.copy(
                    schedules = guides
                        .flatMap { guide -> guide.schedules }
                        .distinct()
                )
            }

    private fun RegionalDisposalGuide.toCandidateKey(): CandidateKey =
        CandidateKey(
            sido = region.sido.normalizeRegionName(),
            sigungu = region.sigungu.normalizeRegionName(),
            managementZoneName = managementZoneName.normalizeRegionName(),
            targetRegionName = targetRegionName.normalizeRegionName(),
            disposalPlaceType = disposalPlaceType.normalizeRegionName(),
            disposalPlaceDescription = disposalPlaceDescription.normalizeRegionName(),
            uncollectedDays = uncollectedDays.normalizeRegionName(),
            departmentName = departmentName.normalizeRegionName(),
            departmentPhoneNumber = departmentPhoneNumber.normalizeRegionName(),
        )

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

    private fun String?.isExactRegionName(
        regionName: String
    ): Boolean =
        normalizeRegionName() == regionName.normalizeRegionName()

    private fun String?.matchesExactMappedAdminDong(
        adminDongNames: Set<String>
    ): Boolean {
        val value = normalizeRegionName() ?: return false

        return value in adminDongNames ||
            value
                .split(TARGET_REGION_DELIMITER)
                .mapNotNull { token -> token.normalizeRegionName() }
                .any { token -> token in adminDongNames }
    }

    private fun String?.normalizeRegionName(): String? =
        this
            ?.trim()
            ?.takeIf { value -> value.isNotBlank() }

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

    private data class CandidateKey(
        val sido: String?,
        val sigungu: String?,
        val managementZoneName: String?,
        val targetRegionName: String?,
        val disposalPlaceType: String?,
        val disposalPlaceDescription: String?,
        val uncollectedDays: String?,
        val departmentName: String?,
        val departmentPhoneNumber: String?,
    )

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
