package com.team.yeogibeoryeo.domain.favorite.model

import com.team.yeogibeoryeo.domain.region.model.Region

data class RegionalGuideFavoriteKey(
    val sido: String?,
    val sigungu: String?,
    val eupmyeondong: String?,
    val targetRegionName: String?,
    val managementZoneName: String? = null,
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
            appendEncoded(managementZoneName)
        }

    fun encodeLegacy(): String =
        buildString {
            append(LEGACY_VERSION)
            append(FIELD_DELIMITER)
            appendEncoded(sido)
            appendEncoded(sigungu)
            appendEncoded(eupmyeondong)
            appendEncoded(targetRegionName)
        }

    companion object {
        private const val VERSION = "regional-guide-v2"
        private const val LEGACY_VERSION = "regional-guide-v1"
        private const val FIELD_DELIMITER = '|'
        private const val NULL_LENGTH = -1

        fun decodeOrNull(value: String): RegionalGuideFavoriteKey? {
            val version = value.substringBefore(FIELD_DELIMITER)
            val fieldCount = when (version) {
                VERSION -> KEY_FIELD_COUNT
                LEGACY_VERSION -> LEGACY_KEY_FIELD_COUNT
                else -> return null
            }

            val payload = value.substringAfter(FIELD_DELIMITER)
            val fields = mutableListOf<String?>()
            var cursor = 0

            repeat(fieldCount) {
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
                managementZoneName = fields.getOrNull(4),
            )
                .takeIf { key -> !key.sido.isNullOrBlank() || !key.sigungu.isNullOrBlank() }
        }

        private const val LEGACY_KEY_FIELD_COUNT = 4
        private const val KEY_FIELD_COUNT = 5

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
