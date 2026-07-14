@file:OptIn(InternalSerializationApi::class)

package com.team.yeogibeoryeo.data.regionalguide.remote.dto

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 행정안전부 지역별 쓰레기 배출 가이드 단일 항목 DTO.
 * 공공데이터 API 특성상 응답 필드 누락이 발생할 수 있으므로, 모든 속성을 Nullable 및 기본값(null)으로 처리합니다.
 */
@Serializable
data class RegionalGuideItemDto(
    @SerialName("CTPV_NM") val sidoName: String? = null,
    @SerialName("SGG_NM") val sigunguName: String? = null,
    @SerialName("MNG_ZONE_NM") val managementZoneName: String? = null,
    @SerialName("MNG_ZONE_TRGT_RGN_NM") val dongName: String? = null,

    @SerialName("EMSN_PLC_TYPE") val disposalPlaceType: String? = null,
    @SerialName("EMSN_PLC") val disposalPlace: String? = null,
    @SerialName("UNCLLT_DAY") val uncollectedDay: String? = null,

    // 일반 쓰레기
    @SerialName("LF_WST_EMSN_DOW") val generalDisposalDays: String? = null,
    @SerialName("LF_WST_EMSN_BGNG_TM") val generalStartTime: String? = null,
    @SerialName("LF_WST_EMSN_END_TM") val generalEndTime: String? = null,
    @SerialName("LF_WST_EMSN_MTHD") val generalMethod: String? = null,

    // 음식물 쓰레기
    @SerialName("FOD_WST_EMSN_DOW") val foodDisposalDays: String? = null,
    @SerialName("FOD_WST_EMSN_BGNG_TM") val foodStartTime: String? = null,
    @SerialName("FOD_WST_EMSN_END_TM") val foodEndTime: String? = null,
    @SerialName("FOD_WST_EMSN_MTHD") val foodMethod: String? = null,

    // 재활용품
    @SerialName("RCYCL_EMSN_DOW") val recycleDisposalDays: String? = null,
    @SerialName("RCYCL_EMSN_BGNG_TM") val recycleStartTime: String? = null,
    @SerialName("RCYCL_EMSN_END_TM") val recycleEndTime: String? = null,
    @SerialName("RCYCL_EMSN_MTHD") val recycleMethod: String? = null,

    // 대형 폐기물
    @SerialName("TMPRY_BULK_WASTE_EMSN_DOW") val largeItemDisposalDays: String? = null,
    @SerialName("TMPRY_BULK_WASTE_EMSN_PLC") val largeItemDisposalPlace: String? = null,
    @SerialName("TMPRY_BULK_WASTE_EMSN_BGNG_TM") val largeItemStartTime: String? = null,
    @SerialName("TMPRY_BULK_WASTE_EMSN_END_TM") val largeItemEndTime: String? = null,
    @SerialName("TMPRY_BULK_WASTE_EMSN_MTHD") val largeItemMethod: String? = null,

    // 관리 부서 정보
    @SerialName("MNG_DEPT_NM") val departmentName: String? = null,
    @SerialName("MNG_DEPT_TELNO") val departmentPhoneNumber: String? = null
)
