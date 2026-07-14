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

    private val WHITESPACE_REGEX = Regex("\\s+")
    private val REGION_NAME_DELIMITER = Regex("[,+/]")
    private val NUMERIC_COMPOSITE_DONG_REGEX =
        Regex("^([^\\d]+?)(\\d+)\\.(\\d+)([^\\d]*동)$")

    private const val DOT = '.'
    private const val MIDDLE_DOT = '·'
    private const val HANGUL_MIDDLE_DOT = 'ㆍ'
    private const val DONG_SUFFIX = "동"
    private val EUPMYEONDONG_SUFFIXES = setOf('읍', '면', '동')
}
