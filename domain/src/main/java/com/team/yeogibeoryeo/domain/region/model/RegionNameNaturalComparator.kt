package com.team.yeogibeoryeo.domain.region.model

object RegionNameNaturalComparator : Comparator<String> {
    private val naturalSortTokenRegex = Regex("\\d+|\\D+")

    override fun compare(
        first: String,
        second: String
    ): Int {
        val firstTokens = first.naturalSortTokens()
        val secondTokens = second.naturalSortTokens()
        val minSize = minOf(firstTokens.size, secondTokens.size)

        for (index in 0 until minSize) {
            val comparison = compareNaturalToken(firstTokens[index], secondTokens[index])
            if (comparison != 0) return comparison
        }

        return firstTokens.size.compareTo(secondTokens.size)
    }

    private fun String.naturalSortTokens(): List<String> =
        naturalSortTokenRegex.findAll(this).map { match -> match.value }.toList()

    private fun compareNaturalToken(
        first: String,
        second: String
    ): Int {
        val firstNumber = first.toLongOrNull()
        val secondNumber = second.toLongOrNull()

        return if (firstNumber != null && secondNumber != null) {
            firstNumber.compareTo(secondNumber)
                .takeIf { comparison -> comparison != 0 }
                ?: first.length.compareTo(second.length)
        } else {
            first.compareTo(second)
        }
    }
}

object RegionCandidateComparator : Comparator<Region> {
    override fun compare(
        first: Region,
        second: Region
    ): Int {
        val sidoComparison = first.sido.orEmpty().compareTo(second.sido.orEmpty())
        if (sidoComparison != 0) return sidoComparison

        val sigunguComparison = first.sigungu.orEmpty().compareTo(second.sigungu.orEmpty())
        if (sigunguComparison != 0) return sigunguComparison

        return RegionNameNaturalComparator.compare(
            first.eupmyeondong.orEmpty(),
            second.eupmyeondong.orEmpty()
        )
    }
}
