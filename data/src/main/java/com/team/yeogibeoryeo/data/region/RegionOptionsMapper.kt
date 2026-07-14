package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideRegionDto
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideRegionKeyNormalizer

internal object RegionOptionsMapper {

    fun getSidoOptions(
        regionalGuideRegions: List<RegionalGuideRegionDto>
    ): List<String> {
        return regionalGuideRegions
            .map { region -> region.sidoName }
            .filter { sido -> sido.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun getSigunguOptions(
        regionalGuideRegions: List<RegionalGuideRegionDto>,
        sido: String
    ): List<String> {
        return regionalGuideRegions
            .asSequence()
            .filter { region -> region.sidoName == sido }
            .map { region -> region.toDisplaySigunguName() }
            .filter { sigungu -> sigungu.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }

    fun getEupmyeondongOptions(
        administrativeRegions: List<AdministrativeRegionDto>,
        sido: String,
        sigungu: String
    ): List<String> {
        return administrativeRegions
            .asSequence()
            .filter { region ->
                region.sidoName == sido &&
                    region.toInfoSigunguOptionName().isSameGuideSigunguName(sigungu)
            }
            .map { region -> region.eupmyeondongName }
            .filter { eupmyeondong -> eupmyeondong.isNotBlank() }
            .distinct()
            .sortedWith(REGION_OPTION_NAME_COMPARATOR)
            .toList()
    }

    fun findSigunguRegions(
        administrativeRegions: List<AdministrativeRegionDto>,
        regionalGuideRegions: List<RegionalGuideRegionDto>,
        keyword: String
    ): List<Region> {
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val regionalGuideMatches = regionalGuideRegions
            .findByGuideRegionName(targetKeyword)

        if (regionalGuideMatches.isNotEmpty()) {
            return regionalGuideMatches
                .map { region -> region.toRegion() }
                .distinctByRegion()
                .sortedWith(REGION_NAME_COMPARATOR)
        }

        return administrativeRegions
            .findByRegionName(targetKeyword) { region -> region.sigunguName }
            .map { region -> region.toSigunguRegion() }
            .distinctByRegion()
            .sortedWith(REGION_NAME_COMPARATOR)
    }

    fun findEupmyeondongRegions(
        administrativeRegions: List<AdministrativeRegionDto>,
        legalAdminDongMappings: List<LegalAdminDongMappingDto>,
        keyword: String
    ): List<Region> {
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val administrativeMatches = administrativeRegions
            .mapNotNull { region ->
                val eupmyeondongName = region.eupmyeondongName
                    .matchedRegionalGuideEupmyeondongNameForKeyword(targetKeyword)
                    ?: return@mapNotNull null

                RegionNormalizer.normalize(
                    Region(
                        sido = region.sidoName,
                        sigungu = region.sigunguName.ifBlank { null },
                        eupmyeondong = eupmyeondongName
                    )
                )
            }

        val legalMatches = legalAdminDongMappings
            .filter { mapping -> mapping.hasSameSigunguCode() }
            .mapNotNull { mapping ->
                val legalDongName = mapping.legalDongName
                    .trim()
                    .matchedLegalDongNameForKeyword(
                        targetKeyword = targetKeyword,
                        allowSuffixlessDongMatch = administrativeMatches.isEmpty()
                    )
                    ?: return@mapNotNull null

                RegionNormalizer.normalize(
                    Region(
                        sido = mapping.sidoName.trim(),
                        sigungu = mapping.sigunguName.trimToNull(),
                        eupmyeondong = legalDongName
                    )
                )
            }

        return (administrativeMatches + legalMatches)
            .distinctByRegionWithEupmyeondong()
            .sortedWith(REGION_NAME_COMPARATOR)
    }

    fun findLegalDongKeywordsByRegion(
        mappings: List<LegalAdminDongMappingDto>,
        region: Region,
        keyword: String
    ): List<String> {
        val targetKeyword = keyword.trim()
        if (targetKeyword.isBlank()) return emptyList()

        val sido = region.sido.trimToNull()
        val sigungu = region.sigungu.trimToNull()

        return mappings
            .asSequence()
            .filter { mapping ->
                (sido == null || mapping.sidoName.trim() == sido) &&
                    (sigungu == null || mapping.sigunguName.trim() == sigungu) &&
                    mapping.legalDongName.trim().matchedLegalDongNameForKeyword(targetKeyword) != null
            }
            .map { mapping -> mapping.legalDongName.trim() }
            .filter { legalDongName -> legalDongName.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }

    fun normalizeRegionForRegionalGuide(
        region: Region,
        regionalGuideRegions: List<RegionalGuideRegionDto>
    ): Region {
        val sido = region.sido?.trim()?.takeIf { sido -> sido.isNotBlank() }
            ?: return region

        if (sido == SEJONG_SIDO) {
            return region.copy(
                sido = sido,
                sigungu = SEJONG_SIDO
            )
        }

        val sigungu = region.sigungu
            ?.trim()
            ?.takeIf { sigungu -> sigungu.isNotBlank() }
            ?: return region.copy(sido = sido)

        val normalizedSigungu = RegionalGuideRegionKeyNormalizer.normalizeSigungu(sigungu)
        val regionalGuideRegion = regionalGuideRegions.firstOrNull { regionalGuideRegion ->
            regionalGuideRegion.sidoName == sido &&
                regionalGuideRegion.sigunguName.isSameGuideSigunguName(normalizedSigungu)
        } ?: return region.copy(sido = sido)

        return region.copy(
            sido = sido,
            sigungu = regionalGuideRegion.toDisplaySigunguName()
        )
    }

    fun findAdminDongCandidatesForLegalDong(
        mappings: List<LegalAdminDongMappingDto>,
        region: Region
    ): List<Region> {
        val sido = region.sido.trimToNull() ?: return emptyList()
        val sigungu = region.sigungu.trimToNull().orEmpty()
        val legalDongName = region.eupmyeondong.trimToNull() ?: return emptyList()

        return mappings
            .filter { mapping ->
                mapping.sidoName.trim() == sido &&
                    mapping.sigunguName.trim() == sigungu &&
                    mapping.legalDongName.trim() == legalDongName
            }
            .mapNotNull { mapping ->
                val adminDongName = mapping.adminDongName.trimToNull() ?: return@mapNotNull null

                RegionNormalizer.normalize(
                    Region(
                        sido = mapping.sidoName.trim(),
                        sigungu = mapping.sigunguName.trimToNull(),
                        eupmyeondong = adminDongName
                    )
                )
            }
            .distinct()
            .sortedWith(REGION_NAME_COMPARATOR)
    }

    private fun RegionalGuideRegionDto.toDisplaySigunguName(): String {
        return if (sidoName == SEJONG_SIDO && sigunguName == NO_SIGUNGU_NAME) {
            sidoName
        } else {
            sigunguName
        }
    }

    private fun AdministrativeRegionDto.toInfoSigunguOptionName(): String {
        val sigungu = sigunguName.ifBlank {
            return sidoName
        }

        return RegionalGuideRegionKeyNormalizer.normalizeSigungu(sigungu)
    }

    private fun RegionalGuideRegionDto.toRegion(): Region {
        return RegionNormalizer.normalize(
            Region(
                sido = sidoName,
                sigungu = sigunguName.ifBlank { null }
            )
        )
    }

    private fun AdministrativeRegionDto.toSigunguRegion(): Region {
        return RegionNormalizer.normalize(
            Region(
                sido = sidoName,
                sigungu = sigunguName.ifBlank { null },
                eupmyeondong = null
            )
        )
    }

    private fun <T> List<T>.findByRegionName(
        targetKeyword: String,
        regionNameSelector: (T) -> String
    ): List<T> {
        val exactMatches = filter { region ->
            regionNameSelector(region) == targetKeyword
        }

        val prefixMatches = exactMatches.ifEmpty {
            filter { region ->
                regionNameSelector(region).startsWith(targetKeyword)
            }
        }

        return prefixMatches.ifEmpty {
            filter { region ->
                regionNameSelector(region).contains(targetKeyword)
            }
        }
    }

    private fun List<RegionalGuideRegionDto>.findByGuideRegionName(
        targetKeyword: String
    ): List<RegionalGuideRegionDto> {
        val exactMatches = filter { region ->
            region.toDisplaySigunguName().isSameGuideSigunguName(targetKeyword)
        }

        val normalizedKeyword = targetKeyword.toGuideSigunguCompareKey()
        val prefixMatches = exactMatches.ifEmpty {
            filter { region ->
                region.toDisplaySigunguName()
                    .toGuideSigunguCompareKey()
                    .startsWith(normalizedKeyword)
            }
        }

        return prefixMatches.ifEmpty {
            filter { region ->
                region.toDisplaySigunguName()
                    .toGuideSigunguCompareKey()
                    .contains(normalizedKeyword)
            }
        }
    }

    private fun String.isSameGuideSigunguName(
        other: String
    ): Boolean {
        return this == other ||
            toGuideSigunguCompareKey() == other.toGuideSigunguCompareKey()
    }

    private fun String.toGuideSigunguCompareKey(): String {
        return trim().removeSuffix(CITY_SUFFIX)
    }

    private fun List<Region>.distinctByRegion(): List<Region> {
        return distinctBy { region ->
            listOf(
                region.sido.orEmpty(),
                region.sigungu.orEmpty()
            )
        }
    }

    private fun List<Region>.distinctByRegionWithEupmyeondong(): List<Region> {
        return distinctBy { region ->
            listOf(
                region.sido.orEmpty(),
                region.sigungu.orEmpty(),
                region.eupmyeondong.orEmpty()
            )
        }
    }

    private fun String.matchedLegalDongNameForKeyword(
        targetKeyword: String,
        allowSuffixlessDongMatch: Boolean = true
    ): String? =
        when {
            this == targetKeyword -> this
            allowSuffixlessDongMatch && matchesEupmyeondongKeyword(targetKeyword) -> this
            startsWith(targetKeyword) && LEGAL_DONG_GA_REGEX.matches(this) -> targetKeyword
            else -> null
        }

    private fun String.matchesEupmyeondongKeyword(targetKeyword: String): Boolean {
        if (this == targetKeyword) return true

        return startsWith(targetKeyword) &&
            length > targetKeyword.length &&
            last() in EUPMYEONDONG_SUFFIXES
    }

    private fun String.matchedRegionalGuideEupmyeondongNameForKeyword(
        targetKeyword: String
    ): String? {
        val joinedCompositeName = toJoinedNonNumericCompositeDongNameOrNull()
        val comparableKeyword = targetKeyword.toJoinedNonNumericCompositeDongNameOrNull()
            ?: targetKeyword

        if (joinedCompositeName?.matchesEupmyeondongKeyword(comparableKeyword) == true) {
            return joinedCompositeName
        }

        return takeIf { eupmyeondongName -> eupmyeondongName.matchesEupmyeondongKeyword(targetKeyword) }
    }

    private fun String.toJoinedNonNumericCompositeDongNameOrNull(): String? {
        val value = trim()
        if (!NON_NUMERIC_COMPOSITE_DONG_DELIMITER_REGEX.containsMatchIn(value)) return null
        if (value.any { character -> character.isDigit() }) return null
        if (value.lastOrNull() !in EUPMYEONDONG_SUFFIXES) return null

        val parts = value
            .split(NON_NUMERIC_COMPOSITE_DONG_DELIMITER_REGEX)
            .filter { part -> part.isNotBlank() }
        if (parts.size <= 1) return null

        return parts.joinToString(separator = "")
    }

    private fun LegalAdminDongMappingDto.hasSameSigunguCode(): Boolean {
        val legalSigunguCode = legalCode.trim().sigunguCodePrefixOrNull() ?: return true
        val adminSigunguCode = adminCode.trim().sigunguCodePrefixOrNull() ?: return true

        return legalSigunguCode == adminSigunguCode
    }

    private fun String.sigunguCodePrefixOrNull(): String? =
        takeIf { code -> code.length >= SIGUNGU_CODE_PREFIX_LENGTH }
            ?.take(SIGUNGU_CODE_PREFIX_LENGTH)

    private fun String?.trimToNull(): String? =
        this
            ?.trim()
            ?.takeIf { value -> value.isNotBlank() }

    private val REGION_NAME_COMPARATOR = compareBy<Region>(
        { region -> region.sido.orEmpty() },
        { region -> region.sigungu.orEmpty() },
        { region -> region.eupmyeondong.orEmpty() }
    )

    private val REGION_OPTION_NAME_COMPARATOR = Comparator<String> { first, second ->
        compareNaturalRegionName(first, second)
    }

    private fun compareNaturalRegionName(
        first: String,
        second: String
    ): Int {
        var firstIndex = 0
        var secondIndex = 0

        while (firstIndex < first.length && secondIndex < second.length) {
            val firstChar = first[firstIndex]
            val secondChar = second[secondIndex]

            if (firstChar.isDigit() && secondChar.isDigit()) {
                val firstNumberEnd = first.findNumberEndIndex(firstIndex)
                val secondNumberEnd = second.findNumberEndIndex(secondIndex)
                val numberComparison = compareNaturalNumberText(
                    first.substring(firstIndex, firstNumberEnd),
                    second.substring(secondIndex, secondNumberEnd)
                )

                if (numberComparison != 0) return numberComparison

                firstIndex = firstNumberEnd
                secondIndex = secondNumberEnd
            } else {
                val charComparison = firstChar.compareTo(secondChar)
                if (charComparison != 0) return charComparison

                firstIndex += 1
                secondIndex += 1
            }
        }

        return first.length.compareTo(second.length)
    }

    private fun String.findNumberEndIndex(
        startIndex: Int
    ): Int {
        var endIndex = startIndex

        while (endIndex < length && this[endIndex].isDigit()) {
            endIndex += 1
        }

        return endIndex
    }

    private fun compareNaturalNumberText(
        first: String,
        second: String
    ): Int {
        val normalizedFirst = first.trimStart('0').ifEmpty { "0" }
        val normalizedSecond = second.trimStart('0').ifEmpty { "0" }

        return normalizedFirst.length.compareTo(normalizedSecond.length)
            .takeIf { comparison -> comparison != 0 }
            ?: normalizedFirst.compareTo(normalizedSecond)
                .takeIf { comparison -> comparison != 0 }
            ?: first.length.compareTo(second.length)
    }

    private const val SEJONG_SIDO = "세종특별자치시"
    private const val NO_SIGUNGU_NAME = "없음"
    private const val CITY_SUFFIX = "시"
    private const val SIGUNGU_CODE_PREFIX_LENGTH = 5
    private val EUPMYEONDONG_SUFFIXES = setOf('읍', '면', '동')
    private val LEGAL_DONG_GA_REGEX = """[가-힣]+\d+가""".toRegex()
    private val NON_NUMERIC_COMPOSITE_DONG_DELIMITER_REGEX = Regex("[.·ㆍ]+")
}
