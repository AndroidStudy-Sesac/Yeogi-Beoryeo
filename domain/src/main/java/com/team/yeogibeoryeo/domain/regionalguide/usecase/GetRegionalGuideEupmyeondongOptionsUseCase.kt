package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.usecase.GetEupmyeondongOptionsUseCase
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import javax.inject.Inject

class GetRegionalGuideEupmyeondongOptionsUseCase @Inject constructor(
    private val getEupmyeondongOptionsUseCase: GetEupmyeondongOptionsUseCase,
    private val normalizeRegionalGuideQueryUseCase: NormalizeRegionalGuideQueryUseCase,
    private val repository: RegionalDisposalGuideRepository,
) {

    suspend operator fun invoke(
        sido: String,
        sigungu: String,
    ): List<String> {
        val options = getEupmyeondongOptionsUseCase(
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

        val coverage = candidates.toAreaCoverages()
        if (coverage.isEmpty()) return options

        val candidateRegionNames = candidates.toRegionNames()
        val filteredOptions = options.filter { option ->
            candidateRegionNames.contains(option.trim()) ||
                coverage.any { areaCoverage -> areaCoverage.matches(option) }
        }

        return filteredOptions.ifEmpty { options }
    }

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
    }
}
