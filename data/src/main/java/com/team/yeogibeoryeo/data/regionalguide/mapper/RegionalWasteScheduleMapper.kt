package com.team.yeogibeoryeo.data.regionalguide.mapper

import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteSchedule
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalWasteType

/**
 * 공공데이터 API의 폐기물 종류별 필드(Prefix)를 분석하여 도메인 모델 스케줄 리스트로 변환하는 매퍼.
 */
object RegionalWasteScheduleMapper {

    fun mapToSchedules(dto: RegionalGuideItemDto): List<RegionalWasteSchedule> {
        return listOfNotNull(
            createSchedule(RegionalWasteType.GENERAL, dto.generalDisposalDays, dto.generalStartTime, dto.generalEndTime, dto.generalMethod),
            createSchedule(RegionalWasteType.FOOD, dto.foodDisposalDays, dto.foodStartTime, dto.foodEndTime, dto.foodMethod),
            createSchedule(RegionalWasteType.RECYCLABLE, dto.recycleDisposalDays, dto.recycleStartTime, dto.recycleEndTime, dto.recycleMethod),
            createSchedule(
                type = RegionalWasteType.LARGE_ITEM,
                days = null,
                start = dto.largeItemStartTime,
                end = dto.largeItemEndTime,
                method = dto.largeItemMethod,
                place = dto.largeItemDisposalPlace,
                useDefaultDayWhenBlank = false,
                useDefaultMethodWhenBlank = false,
            )
        )
    }

    private fun createSchedule(
        type: RegionalWasteType,
        days: String?,
        start: String?,
        end: String?,
        method: String?,
        place: String? = null,
        useDefaultDayWhenBlank: Boolean = true,
        useDefaultMethodWhenBlank: Boolean = true,
    ): RegionalWasteSchedule? {
        if (days.isNullOrBlank() && start.isNullOrBlank() &&
            end.isNullOrBlank() && method.isNullOrBlank() && place.isNullOrBlank()) return null

        return RegionalWasteSchedule(
            wasteType = type,
            disposalDays = if (useDefaultDayWhenBlank) parseDays(days) else parseDaysOrNull(days),
            disposalStartTime = parseTime(start),
            disposalEndTime = parseTime(end),
            disposalMethod = if (useDefaultMethodWhenBlank) parseMethod(method) else parseMethodOrNull(method),
            disposalPlace = place?.trim()?.takeIf { value -> value.isNotBlank() },
        )
    }

    // [로직] 요일 텍스트 정제 (예: "월요일, 화요일" -> "월, 화")
    internal fun parseDays(days: String?): String {
        if (days.isNullOrBlank()) return "미지정"
        return days.replace("요일", "")
            .replace("+", ", ")
            .split(Regex("[,/|]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")
    }

    // [로직] 시간 데이터 예외 처리
    internal fun parseTime(time: String?): String? {
        val trimmed = time?.trim()
        if (trimmed.isNullOrBlank()) return null

        val normalized = COMPACT_TIME_REGEX.matchEntire(trimmed)
            ?.destructured
            ?.let { (hourText, minuteText) ->
                val hour = hourText.toInt()
                val minute = minuteText.toInt()
                if ((hour in 0..23 && minute in 0..59) || (hour == 24 && minute == 0)) {
                    "$hourText:$minuteText"
                } else {
                    trimmed
                }
            }
            ?: trimmed

        return normalized.takeUnless { it in EMPTY_TIME_VALUES }
    }

    // [로직] 배출 방법 정제
    internal fun parseMethod(method: String?): String {
        if (method.isNullOrBlank()) return "지정된 배출 방법이 없습니다."
        return method.replace(Regex("\\s+"), " ").trim()
    }

    private fun parseDaysOrNull(days: String?): String? =
        days
            ?.takeIf { value -> value.isNotBlank() }
            ?.let(::parseDays)

    private fun parseMethodOrNull(method: String?): String? =
        method
            ?.takeIf { value -> value.isNotBlank() }
            ?.let(::parseMethod)

    private val COMPACT_TIME_REGEX = Regex("^(\\d{2})(\\d{2})$")
    private val EMPTY_TIME_VALUES = setOf("00:00", "00:00:00")
}
