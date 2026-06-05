package com.team.yeogibeoryeo.data.region.parser

import com.team.yeogibeoryeo.data.region.RegionNormalizer
import com.team.yeogibeoryeo.domain.region.model.Region
import javax.inject.Inject

class RegionAddressParser @Inject constructor() {

    fun parse(address: String): Region {
        val normalizedAddress = address.trim()
        val parts = normalizedAddress
            .split(WHITESPACE_REGEX)
            .map { part -> part.cleanRegionToken() }
            .filter { part -> part.isNotBlank() }

        var sido: String? = null
        var sigungu: String? = null
        var eupmyeondong: String? = null

        var index = 0
        while (index < parts.size) {
            val part = parts[index]

            when {
                part.isSidoName() && sido == null -> {
                    sido = part
                }

                part.isSigunguName() && part != sido -> {
                    if (sigungu == null) {
                        val nextPart = parts.getOrNull(index + 1)

                        sigungu = if (part.endsWith("시") && nextPart?.endsWith("구") == true) {
                            "$part $nextPart"
                        } else {
                            part
                        }
                    }
                }

                part.isEupmyeondongName() -> {
                    if (eupmyeondong == null) eupmyeondong = part
                }
            }

            index += 1
        }

        val parsedRegion = Region(
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = eupmyeondong ?: normalizedAddress.extractParenthesizedEupmyeondong()
        )

        return RegionNormalizer.normalize(parsedRegion)
    }

    private fun String.cleanRegionToken(): String =
        trim().trim('(', ')', '[', ']', ',', '.', ' ')

    private fun String.extractParenthesizedEupmyeondong(): String? {
        return PARENTHESIZED_REGION_REGEX
            .findAll(this)
            .mapNotNull { matchResult ->
                matchResult.groupValues.getOrNull(1)?.cleanRegionToken()
            }
            .firstOrNull { token -> token.isEupmyeondongName() }
    }

    private fun String.isSidoName(): Boolean =
        RegionNormalizer.isSidoName(this)

    private fun String.isSigunguName(): Boolean =
        endsWith("시") || endsWith("군") || endsWith("구")

    private fun String.isEupmyeondongName(): Boolean =
        endsWith("읍") || endsWith("면") || endsWith("동")

    companion object {
        private val WHITESPACE_REGEX = "\\s+".toRegex()
        private val PARENTHESIZED_REGION_REGEX = "\\(([^)]+)\\)".toRegex()
    }
}
