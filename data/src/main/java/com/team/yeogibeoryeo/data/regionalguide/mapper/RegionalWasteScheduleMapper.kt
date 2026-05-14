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
            createSchedule(RegionalWasteType.LARGE_ITEM, dto.largeItemDisposalDays, dto.largeItemStartTime, dto.largeItemEndTime, dto.largeItemMethod)
        )
    }

    private fun createSchedule(
        type: RegionalWasteType,
        days: String?,
        start: String?,
        end: String?,
        method: String?
    ): RegionalWasteSchedule? {
        if (days.isNullOrBlank() && start.isNullOrBlank() &&
            end.isNullOrBlank() && method.isNullOrBlank()) return null

        return RegionalWasteSchedule(
            wasteType = type,
            disposalDays = parseDays(days),
            disposalStartTime = parseTime(start),
            disposalEndTime = parseTime(end),
            disposalMethod = parseMethod(method)
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
        return if (trimmed.isNullOrBlank() || trimmed == "00:00" || trimmed == "00:00:00") null else trimmed
    }

    // [로직] 배출 방법 정제
    internal fun parseMethod(method: String?): String {
        if (method.isNullOrBlank()) return "지정된 배출 방법이 없습니다."
        return method.replace(Regex("\\s+"), " ").trim()
    }
}