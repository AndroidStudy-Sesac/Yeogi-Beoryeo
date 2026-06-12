package com.team.yeogibeoryeo.domain.favorite.model

import com.team.yeogibeoryeo.domain.region.model.Region

data class RegionalGuideFavoriteKey(
    val sido: String?,
    val sigungu: String?,
    val eupmyeondong: String?,
    val targetRegionName: String?,
) {
    fun toRegion(): Region =
        Region(
            sido = sido,
            sigungu = sigungu,
            eupmyeondong = eupmyeondong,
        )

    fun encode(): String =
        buildString {
            append(VERSION)
            append(FIELD_DELIMITER)
            appendEncoded(sido)
            appendEncoded(sigungu)
            appendEncoded(eupmyeondong)
            appendEncoded(targetRegionName)
        }

    companion object {
        private const val VERSION = "regional-guide-v1"
        private const val FIELD_DELIMITER = '|'
        private const val NULL_LENGTH = -1

        fun decodeOrNull(value: String): RegionalGuideFavoriteKey? {
            if (!value.startsWith("$VERSION$FIELD_DELIMITER")) return null

            val payload = value.substringAfter(FIELD_DELIMITER)
            val fields = mutableListOf<String?>()
            var cursor = 0

            repeat(KEY_FIELD_COUNT) {
                val delimiterIndex = payload.indexOf(':', startIndex = cursor)
                if (delimiterIndex == -1) return null

                val length = payload
                    .substring(startIndex = cursor, endIndex = delimiterIndex)
                    .toIntOrNull()
                    ?: return null

                cursor = delimiterIndex + 1

                if (length == NULL_LENGTH) {
                    fields += null
                    return@repeat
                }

                if (length < 0 || cursor + length > payload.length) return null

                fields += payload.substring(startIndex = cursor, endIndex = cursor + length)
                cursor += length
            }

            if (cursor != payload.length) return null

            return RegionalGuideFavoriteKey(
                sido = fields[0],
                sigungu = fields[1],
                eupmyeondong = fields[2],
                targetRegionName = fields[3],
            )
                .takeIf { key -> !key.sido.isNullOrBlank() || !key.sigungu.isNullOrBlank() }
        }

        private const val KEY_FIELD_COUNT = 4

        private fun StringBuilder.appendEncoded(value: String?) {
            val normalizedValue = value?.trim()?.takeIf { it.isNotBlank() }

            if (normalizedValue == null) {
                append(NULL_LENGTH)
                append(':')
            } else {
                append(normalizedValue.length)
                append(':')
                append(normalizedValue)
            }
        }
    }
}
