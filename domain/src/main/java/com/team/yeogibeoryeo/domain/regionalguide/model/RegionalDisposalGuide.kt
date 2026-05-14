package com.team.yeogibeoryeo.domain.regionalguide.model

import com.team.yeogibeoryeo.domain.region.model.Region

/**
 * 특정 행정구역(Region)의 종합적인 쓰레기 배출 가이드 모델
 *
 * @property region 대상 행정구역 정보
 * @property managementZoneName 관리구역명
 * @property targetRegionName 관리구역 대상 지역명
 * @property disposalPlaceType 배출장소 유형
 * @property disposalPlaceDescription 배출장소 상세 설명
 * @property schedules 폐기물 유형별 배출 스케줄 목록
 * @property uncollectedDays 수거 제외일
 * @property departmentName 담당 관리부서명
 * @property departmentPhoneNumber 담당 부서 연락처
 */
data class RegionalDisposalGuide(
    val region: Region,
    val managementZoneName: String? = null,
    val targetRegionName: String? = null,
    val disposalPlaceType: String? = null,
    val disposalPlaceDescription: String? = null,
    val schedules: List<RegionalWasteSchedule>,
    val uncollectedDays: String? = null,
    val departmentName: String? = null,
    val departmentPhoneNumber: String? = null,
)