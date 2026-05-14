package com.team.yeogibeoryeo.domain.regionalguide.model

import com.team.yeogibeoryeo.domain.region.model.Region

/**
 * 특정 행정구역(Region)의 종합적인 쓰레기 배출 가이드 모델
 *
 * @property region 대상 행정구역
 * @property schedules 폐기물 종류별 배출 스케줄 목록
 * @property uncollectedDays 수거 제외일
 * @property disposalPlaceType 배출장소 유형
 * @property guideUrl 지자체 공식 배출 안내 페이지 링크 (선택)
 * @property inquiryPhone 관할 부서 문의 전화번호 (선택)
 */
data class RegionalDisposalGuide(
    val region: Region,
    val schedules: List<RegionalWasteSchedule>,
    val uncollectedDays: String? = null,
    val disposalPlaceType: String? = null,
    val guideUrl: String? = null,
    val inquiryPhone: String? = null
)