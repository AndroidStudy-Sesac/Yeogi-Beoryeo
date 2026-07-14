package com.team.yeogibeoryeo.domain.regionalguide.model

import com.team.yeogibeoryeo.domain.region.model.Region

object RegionalGuideLegacyRegionCompatibilityPolicy {

    fun replacementRegions(region: Region): List<Region> {
        val sido = region.sido.normalizeRegionName()
        val sigungu = region.sigungu.normalizeRegionName()
        val eupmyeondong = region.eupmyeondong.normalizeRegionName()

        val incheonRegions = incheonReplacementRegions(sido = sido, sigungu = sigungu)
        if (incheonRegions.isNotEmpty()) return incheonRegions

        return anyangReplacementRegion(
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = eupmyeondong,
        )?.let(::listOf).orEmpty()
    }

    fun keywordReplacementRegions(keyword: String): List<Region> {
        val normalizedKeyword = keyword.normalizeRegionName() ?: return emptyList()

        return when (normalizedKeyword) {
            INCHEON_OLD_JUNGGU -> incheonSplitRegions(
                first = INCHEON_YEONGJONGGU,
                second = INCHEON_JEMULPOGU,
            )

            INCHEON_OLD_DONGGU -> listOf(incheonRegion(INCHEON_JEMULPOGU))

            INCHEON_OLD_SEOGU -> incheonSplitRegions(
                first = INCHEON_SEOHAEGU,
                second = INCHEON_GEOMDANGU,
            )

            ANYANG_OLD_8_DONG -> listOf(anyangRegion(ANYANG_MYEONGHAKDONG))
            ANYANG_OLD_9_DONG -> listOf(anyangRegion(ANYANG_BYEONGMOKANDONG))
            else -> emptyList()
        }
    }

    fun isSplitReplacement(region: Region): Boolean =
        replacementRegions(region).size > 1

    fun isSameRegion(
        requestedRegion: Region,
        candidateRegion: Region,
    ): Boolean {
        val replacements = replacementRegions(requestedRegion)
        if (replacements.isNotEmpty()) {
            return replacements.any { replacement ->
                replacement.hasSameRegion(candidateRegion)
            }
        }

        return requestedRegion.hasSameRegion(candidateRegion)
    }

    fun normalizeRegionalGuideName(
        region: Region,
        name: String?,
    ): String? {
        val normalizedName = name.normalizeRegionName() ?: return null

        return when {
            isAnyangLegacyRegion(region, ANYANG_OLD_8_DONG) &&
                normalizedName.contains(ANYANG_OLD_8_DONG) ->
                normalizedName.replace(ANYANG_OLD_8_DONG, ANYANG_MYEONGHAKDONG)

            isAnyangLegacyRegion(region, ANYANG_OLD_9_DONG) &&
                normalizedName.contains(ANYANG_OLD_9_DONG) ->
                normalizedName.replace(ANYANG_OLD_9_DONG, ANYANG_BYEONGMOKANDONG)

            isIncheonLegacyDongguRegion(region) &&
                normalizedName.contains(INCHEON_OLD_DONGGU) ->
                normalizedName.replace(INCHEON_OLD_DONGGU, INCHEON_JEMULPOGU)

            else -> normalizedName
        }
    }

    private fun incheonReplacementRegions(
        sido: String?,
        sigungu: String?,
    ): List<Region> {
        if (sido != null && sido != INCHEON_SIDO) return emptyList()

        return when (sigungu) {
            INCHEON_OLD_JUNGGU -> incheonSplitRegions(
                first = INCHEON_YEONGJONGGU,
                second = INCHEON_JEMULPOGU,
            )

            INCHEON_OLD_DONGGU -> listOf(incheonRegion(INCHEON_JEMULPOGU))

            INCHEON_OLD_SEOGU -> incheonSplitRegions(
                first = INCHEON_SEOHAEGU,
                second = INCHEON_GEOMDANGU,
            )

            else -> emptyList()
        }
    }

    private fun anyangReplacementRegion(
        sido: String?,
        sigungu: String?,
        eupmyeondong: String?,
    ): Region? {
        if (sido != null && sido != GYEONGGI_SIDO) return null
        if (sigungu != null && sigungu !in ANYANG_SIGUNGU_NAMES) return null

        return when (eupmyeondong) {
            ANYANG_OLD_8_DONG -> anyangRegion(ANYANG_MYEONGHAKDONG)
            ANYANG_OLD_9_DONG -> anyangRegion(ANYANG_BYEONGMOKANDONG)
            else -> null
        }
    }

    private fun isAnyangLegacyRegion(
        region: Region,
        eupmyeondong: String,
    ): Boolean {
        val normalizedRegion = region.normalized()

        return normalizedRegion.sido == GYEONGGI_SIDO &&
            normalizedRegion.sigungu in ANYANG_SIGUNGU_NAMES &&
            normalizedRegion.eupmyeondong == eupmyeondong
    }

    private fun isIncheonLegacyDongguRegion(region: Region): Boolean {
        val normalizedRegion = region.normalized()

        return normalizedRegion.sido == INCHEON_SIDO &&
            normalizedRegion.sigungu == INCHEON_OLD_DONGGU
    }

    private fun Region.hasSameRegion(other: Region): Boolean =
        sido.normalizeRegionName() == other.sido.normalizeRegionName() &&
            sigungu.normalizeRegionName() == other.sigungu.normalizeRegionName() &&
            eupmyeondong.normalizeRegionName() == other.eupmyeondong.normalizeRegionName()

    private fun Region.normalized(): Region =
        Region(
            sido = sido.normalizeRegionName(),
            sigungu = sigungu.normalizeRegionName(),
            eupmyeondong = eupmyeondong.normalizeRegionName(),
        )

    private fun String?.normalizeRegionName(): String? =
        this
            ?.trim()
            ?.takeIf { value -> value.isNotBlank() }

    private fun incheonSplitRegions(
        first: String,
        second: String,
    ): List<Region> =
        listOf(
            incheonRegion(first),
            incheonRegion(second),
        )

    private fun incheonRegion(sigungu: String): Region =
        Region(
            sido = INCHEON_SIDO,
            sigungu = sigungu,
        )

    private fun anyangRegion(eupmyeondong: String): Region =
        Region(
            sido = GYEONGGI_SIDO,
            sigungu = ANYANG_MANANGU,
            eupmyeondong = eupmyeondong,
        )

    private const val INCHEON_SIDO = "인천광역시"
    private const val INCHEON_OLD_JUNGGU = "중구"
    private const val INCHEON_OLD_DONGGU = "동구"
    private const val INCHEON_OLD_SEOGU = "서구"
    private const val INCHEON_YEONGJONGGU = "영종구"
    private const val INCHEON_JEMULPOGU = "제물포구"
    private const val INCHEON_SEOHAEGU = "서해구"
    private const val INCHEON_GEOMDANGU = "검단구"

    private const val GYEONGGI_SIDO = "경기도"
    private const val ANYANG_MANANGU = "안양시 만안구"
    private const val ANYANG_SIGUNGU = "안양시"
    private const val ANYANG_OLD_8_DONG = "안양8동"
    private const val ANYANG_OLD_9_DONG = "안양9동"
    private const val ANYANG_MYEONGHAKDONG = "명학동"
    private const val ANYANG_BYEONGMOKANDONG = "병목안동"
    private val ANYANG_SIGUNGU_NAMES = setOf(ANYANG_MANANGU, ANYANG_SIGUNGU)
}
