package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.model.RegionNameNaturalComparator
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideEupmyeondongNamePolicy
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import javax.inject.Inject

class GetRegionalGuideEupmyeondongOptionsUseCase @Inject constructor(
    private val regionOptionsRepository: RegionOptionsRepository,
    private val normalizeRegionalGuideQueryUseCase: NormalizeRegionalGuideQueryUseCase,
    private val repository: RegionalDisposalGuideRepository,
) {

    suspend operator fun invoke(
        sido: String,
        sigungu: String,
    ): List<String> {
        val options = regionOptionsRepository.getRegionalGuideEupmyeondongOptions(
            sido = sido,
            sigungu = sigungu,
        )
        if (options.isEmpty()) return options

        val query = normalizeRegionalGuideQueryUseCase(
            Region(
                sido = sido,
                sigungu = sigungu,
            )
        ) ?: return options

        val candidates = repository.getRegionalDisposalGuideCandidates(query)
            .getOrElse { return options }

        val candidateRegionNames = candidates.toRegionNames()
        val selectableOptions = options.filterUnsupportedBranchOfficeOptions(candidateRegionNames)
        val coverage = candidates.toAreaCoverages()
        if (coverage.isEmpty()) return selectableOptions.sortedOptionNames()

        val filteredOptions = selectableOptions.filter { option ->
            candidateRegionNames.any { name ->
                RegionalGuideEupmyeondongNamePolicy.containsSameNameOrGuideAreaName(
                    regionName = name,
                    eupmyeondong = option,
                )
            } ||
                coverage.any { areaCoverage -> areaCoverage.matches(option) }
        }

        return filteredOptions.sortedOptionNames()
    }

    private fun List<String>.sortedOptionNames(): List<String> {
        return distinct()
            .sortedWith(RegionNameNaturalComparator)
    }

    private fun List<String>.filterUnsupportedBranchOfficeOptions(
        candidateRegionNames: Set<String>
    ): List<String> {
        return filter { option ->
            !option.isBranchOfficeName() ||
                candidateRegionNames.any { name ->
                    RegionalGuideEupmyeondongNamePolicy.containsSameNameOrGuideAreaName(
                        regionName = name,
                        eupmyeondong = option,
                    )
                }
        }
    }

    private fun String.isBranchOfficeName(): Boolean =
        trim().endsWith(BRANCH_OFFICE_SUFFIX)

    private fun List<RegionalDisposalGuide>.toRegionNames(): Set<String> {
        return flatMap { guide ->
            listOf(guide.managementZoneName, guide.targetRegionName)
        }.mapNotNull { name ->
            name
                ?.trim()
                ?.takeIf { trimmedName -> trimmedName.isNotBlank() }
        }.toSet()
    }

    private fun List<RegionalDisposalGuide>.toAreaCoverages(): Set<RegionalGuideAreaCoverage> {
        return flatMap { guide ->
            listOf(guide.managementZoneName, guide.targetRegionName)
        }.mapNotNull { name ->
            name.toAreaCoverageOrNull()
        }.toSet()
    }

    private fun String?.toAreaCoverageOrNull(): RegionalGuideAreaCoverage? {
        val tokens = this
            ?.trim()
            ?.split(WHITESPACE_REGEX)
            ?.filter { token -> token.isNotBlank() }
            .orEmpty()

        if (tokens.isEmpty()) return null

        return when {
            tokens.matchesAreaExpression(DONG_AREA) -> RegionalGuideAreaCoverage.DONG
            tokens.matchesAreaExpression(EUP_MYEON_AREA) -> RegionalGuideAreaCoverage.EUP_MYEON
            else -> null
        }
    }

    private fun List<String>.matchesAreaExpression(areaExpression: String): Boolean {
        return joinToString(separator = "") == areaExpression ||
            lastOrNull() == areaExpression ||
            takeLast(2).joinToString(separator = "") == areaExpression
    }

    private enum class RegionalGuideAreaCoverage {
        DONG,
        EUP_MYEON;

        fun matches(option: String): Boolean {
            val trimmedOption = option.trim()

            return when (this) {
                DONG -> trimmedOption.endsWith(DONG_SUFFIX)
                EUP_MYEON -> trimmedOption.endsWith(EUP_SUFFIX) ||
                    trimmedOption.endsWith(MYEON_SUFFIX)
            }
        }
    }

    private companion object {
        val WHITESPACE_REGEX = Regex("\\s+")
        const val DONG_AREA = "동지역"
        const val EUP_MYEON_AREA = "읍면지역"
        const val EUP_SUFFIX = "읍"
        const val MYEON_SUFFIX = "면"
        const val DONG_SUFFIX = "동"
        const val BRANCH_OFFICE_SUFFIX = "출장소"
    }
}
