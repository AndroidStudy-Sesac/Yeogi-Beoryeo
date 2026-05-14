package com.team.yeogibeoryeo.domain.regionalguide.model

/**
 * 특정 폐기물 종류별 배출 스케줄 정보
 *
 * @property wasteType 폐기물 종류
 * @property disposalDays 배출 가능 요일
 * @property disposalStartTime 배출 시작 시간
 * @property disposalEndTime 배출 종료 시간
 * @property disposalMethod 배출 방법
 * @property notice 추가 안내사항
 */
data class RegionalWasteSchedule(
    val wasteType: RegionalWasteType,
    val disposalDays: String,
    val disposalStartTime: String? = null,
    val disposalEndTime: String? = null,
    val disposalMethod: String? = null,
    val notice: String? = null
)