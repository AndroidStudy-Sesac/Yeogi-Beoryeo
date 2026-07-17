package com.team.yeogibeoryeo.domain.regionalguide.model

object RegionalGuideEupmyeondongNamePolicy {

    fun isSameName(
        first: String?,
        second: String?,
    ): Boolean {
        val firstNames = comparableNames(first)
        val secondNames = comparableNames(second)

        return firstNames.isNotEmpty() && secondNames.any { name -> name in firstNames }
    }

    fun matchesKeyword(
        eupmyeondongName: String?,
        keyword: String,
    ): Boolean {
        val normalizedKeyword = keyword.normalizeName() ?: return false

        return comparableNames(eupmyeondongName).any { name ->
            name == normalizedKeyword ||
                name.startsWith(normalizedKeyword) &&
                name.lastOrNull() in EUPMYEONDONG_SUFFIXES
        }
    }

    fun toApiCompatibleDisplayName(name: String?): String? {
        val originalName = name?.trim()?.takeIf { value -> value.isNotBlank() } ?: return null
        val normalizedName = originalName.normalizeName() ?: return originalName

        return normalizedName.toJoinedNonNumericCompositeDongNameOrNull() ?: originalName
    }

    fun comparableNames(name: String?): Set<String> {
        val value = name.normalizeName() ?: return emptySet()
        val names = mutableSetOf(value)

        value.expandNumericCompositeDongNames()?.let(names::addAll)
        value.toJoinedNonNumericCompositeDongNameOrNull()?.let(names::add)
        value.toNumberedEupmyeondongWithoutJeOrNull()?.let(names::add)

        return names
    }

    fun containsSameName(
        regionName: String?,
        eupmyeondong: String?,
    ): Boolean {
        val requestedName = eupmyeondong.normalizeName() ?: return false

        return regionName
            ?.split(REGION_NAME_DELIMITER)
            ?.any { name -> isSameName(name, requestedName) }
            ?: false
    }

    fun containsSameNameOrGuideAreaName(
        regionName: String?,
        eupmyeondong: String?,
    ): Boolean {
        if (containsSameName(regionName, eupmyeondong)) return true

        return regionName
            ?.split(REGION_NAME_DELIMITER)
            ?.any { name ->
                val normalizedName = name.normalizeName() ?: return@any false

                normalizedName.matchesSuffixlessEupmyeondongName(eupmyeondong) ||
                    normalizedName.matchesNumberedEupmyeondongRange(eupmyeondong) ||
                    GUIDE_AREA_SUFFIXES.any { suffix ->
                        normalizedName
                            .removeSuffix(suffix)
                            .takeIf(String::isNotBlank)
                            ?.let { areaName -> isSameName(areaName, eupmyeondong) }
                            ?: false
                    }
            }
            ?: false
    }

    fun targetRegionStartsWithLegalDongName(
        targetRegionName: String?,
        eupmyeondong: String?,
    ): Boolean {
        val legalDongName = eupmyeondong.normalizeName()
            ?.takeIf { name -> name.endsWith(DONG_SUFFIX) }
            ?.removeSuffix(DONG_SUFFIX)
            ?.takeIf { name -> name.length >= MINIMUM_LEGAL_DONG_NAME_LENGTH }
            ?: return false

        return targetRegionName
            ?.split(REGION_NAME_DELIMITER)
            ?.any { name ->
                val normalizedName = name.normalizeName() ?: return@any false
                val targetRegionBaseName = normalizedName.removeGuideAreaSuffix()
                val followingName = targetRegionBaseName.removePrefix(legalDongName)

                targetRegionBaseName.startsWith(legalDongName) &&
                    followingName.isNotBlank() &&
                    !followingName.startsWithNumberedDongExpression() &&
                    !followingName.endsWithAdministrativeRegionSuffix()
            }
            ?: false
    }

    fun matchesManagementZoneOrTargetRegionName(
        managementZoneName: String?,
        targetRegionName: String?,
        eupmyeondong: String,
    ): Boolean {
        return containsSameNameOrGuideAreaName(
            regionName = managementZoneName,
            eupmyeondong = eupmyeondong,
        ) ||
            containsSameNameOrGuideAreaName(
                regionName = targetRegionName,
                eupmyeondong = eupmyeondong,
            ) ||
            targetRegionStartsWithLegalDongName(
                targetRegionName = targetRegionName,
                eupmyeondong = eupmyeondong,
            )
    }

    fun hasEupmyeondongCoverage(regionName: String?): Boolean {
        return regionName
            ?.split(REGION_NAME_DELIMITER)
            ?.any { name ->
                val normalizedName = name.normalizeName() ?: return@any false
                val areaName = GUIDE_AREA_SUFFIXES
                    .firstNotNullOfOrNull { suffix ->
                        normalizedName
                            .takeIf { value -> value.endsWith(suffix) }
                            ?.removeSuffix(suffix)
                            ?.takeIf(String::isNotBlank)
                    }
                    ?: normalizedName

                areaName.endsWithAny(EUPMYEONDONG_SUFFIXES) ||
                    areaName in GUIDE_AREA_NAMES
            }
            ?: false
    }

    fun isNumberedDongAliasOf(
        name: String?,
        keyword: String,
    ): Boolean {
        val normalizedKeyword = keyword.normalizeName()
            ?.takeIf(::isNumberOmittedDongKeyword)
            ?: return false

        val baseName = normalizedKeyword.removeSuffix(DONG_SUFFIX)

        val numberedDongAliasRegex = Regex(
            pattern = "^${Regex.escape(baseName)}(?:제)?\\d+$DONG_SUFFIX$"
        )

        return comparableNames(name).any { candidateName ->
            numberedDongAliasRegex.matches(candidateName)
        }
    }

    fun isNumberOmittedDongKeyword(keyword: String?): Boolean {
        val normalizedKeyword = keyword.normalizeName() ?: return false

        return normalizedKeyword.endsWith(DONG_SUFFIX) &&
            normalizedKeyword.removeSuffix(DONG_SUFFIX).isNotBlank() &&
            normalizedKeyword.none(Char::isDigit)
    }

    private fun String?.normalizeName(): String? =
        this
            ?.replace(WHITESPACE_REGEX, "")
            ?.replace(MIDDLE_DOT, DOT)
            ?.replace(HANGUL_MIDDLE_DOT, DOT)
            ?.takeIf { value -> value.isNotBlank() }

    private fun String.expandNumericCompositeDongNames(): Set<String>? {
        val match = NUMERIC_COMPOSITE_DONG_REGEX.matchEntire(this) ?: return null
        val prefix = match.groupValues[1]
        val firstNumber = match.groupValues[2]
        val secondNumber = match.groupValues[3]
        val suffix = match.groupValues[4]

        return setOf(
            "$prefix$firstNumber$suffix",
            "$prefix$secondNumber$suffix",
        )
    }

    private fun String.toJoinedNonNumericCompositeDongNameOrNull(): String? {
        if (!contains(DOT) || any { character -> character.isDigit() } || !endsWith(DONG_SUFFIX)) {
            return null
        }

        val names = split(DOT)
            .filter { name -> name.isNotBlank() }
        if (names.size <= 1) return null

        return names.joinToString(separator = "")
    }

    private fun String.toNumberedEupmyeondongWithoutJeOrNull(): String? =
        replace(NUMBER_MARKER_REGEX, "")
            .takeIf { normalizedName -> normalizedName != this }

    private fun String.endsWithAny(suffixes: Set<Char>): Boolean =
        lastOrNull() in suffixes

    private fun String.matchesSuffixlessEupmyeondongName(
        eupmyeondong: String?,
    ): Boolean {
        val normalizedEupmyeondong = eupmyeondong.normalizeName() ?: return false

        return EUPMYEONDONG_SUFFIXES.any { suffix ->
            normalizedEupmyeondong
                .takeIf { name -> name.endsWith(suffix) }
                ?.removeSuffix(suffix.toString())
                ?.takeIf { name -> name.length >= MINIMUM_SUFFIXLESS_NAME_LENGTH }
                ?.let { suffixlessName -> this == suffixlessName }
                ?: false
        }
    }

    private fun String.startsWithNumberedDongExpression(): Boolean =
        firstOrNull()?.isDigit() == true ||
            startsWith(NUMBER_MARKER) && getOrNull(1)?.isDigit() == true

    private fun String.endsWithAdministrativeRegionSuffix(): Boolean =
        lastOrNull() in ADMINISTRATIVE_REGION_SUFFIXES

    private fun String.removeGuideAreaSuffix(): String =
        GUIDE_AREA_SUFFIXES
            .firstOrNull { suffix -> endsWith(suffix) }
            ?.let(::removeSuffix)
            ?.takeIf(String::isNotBlank)
            ?: this

    private fun String.matchesNumberedEupmyeondongRange(
        eupmyeondong: String?,
    ): Boolean {
        val rangeMatch = NUMBERED_EUPMYEONDONG_RANGE_REGEX.matchEntire(this) ?: return false
        val rangePrefix = rangeMatch.groupValues[1]
        val rangeStart = rangeMatch.groupValues[2].toInt()
        val rangeEnd = rangeMatch.groupValues[3].toInt()
        val rangeSuffix = rangeMatch.groupValues[4]

        return comparableNames(eupmyeondong).any { candidateName ->
            val candidateMatch = NUMBERED_EUPMYEONDONG_REGEX.matchEntire(candidateName)
                ?: return@any false

            val candidatePrefix = candidateMatch.groupValues[1]
            val candidateNumber = candidateMatch.groupValues[2].toInt()
            val candidateSuffix = candidateMatch.groupValues[3]

            candidatePrefix == rangePrefix &&
                candidateSuffix == rangeSuffix &&
                candidateNumber in rangeStart..rangeEnd
        }
    }

    private val WHITESPACE_REGEX = Regex("\\s+")
    private val REGION_NAME_DELIMITER = Regex("[,+/]")
    private val NUMERIC_COMPOSITE_DONG_REGEX =
        Regex("^([^\\d]+?)(\\d+)\\.(\\d+)([^\\d]*동)$")
    private val NUMBERED_EUPMYEONDONG_RANGE_REGEX =
        Regex("^(.+?)(?:제)?(\\d+)[~～-](?:제)?(\\d+)([읍면동])$")
    private val NUMBERED_EUPMYEONDONG_REGEX =
        Regex("^(.+?)(?:제)?(\\d+)([읍면동])$")
    private val NUMBER_MARKER_REGEX = Regex("제(?=\\d)")

    private const val DOT = '.'
    private const val MIDDLE_DOT = '·'
    private const val HANGUL_MIDDLE_DOT = 'ㆍ'
    private const val DONG_SUFFIX = "동"
    private const val NUMBER_MARKER = "제"
    private const val MINIMUM_LEGAL_DONG_NAME_LENGTH = 2
    private const val MINIMUM_SUFFIXLESS_NAME_LENGTH = 2
    private val GUIDE_AREA_SUFFIXES = listOf("일부지역", "일부", "일원", "전지역", "전체")
    private val GUIDE_AREA_NAMES = setOf("동지역", "읍면지역")
    private val EUPMYEONDONG_SUFFIXES = setOf('읍', '면', '동')
    private val ADMINISTRATIVE_REGION_SUFFIXES = EUPMYEONDONG_SUFFIXES + '리'
}
