package com.moon.yeogi_beoryeo.spike

import com.google.gson.annotations.SerializedName

data class SpotBasicResponse(val response: SpotBasicBodyWrapper)
data class SpotBasicBodyWrapper(val body: SpotBasicItemsWrapper)
data class SpotBasicItemsWrapper(val items: SpotBasicItemListWrapper?)
data class SpotBasicItemListWrapper(
    @SerializedName("item") val itemList: List<SpotBasicItem> = emptyList()
)

data class SpotBasicItem(
    @SerializedName("spotNm") val spotName: String?,
    @SerializedName("addrBase") val address: String?,
    @SerializedName("addrDtl") val addressDetail: String?
)

data class SpotDetailResponse(val response: SpotDetailBodyWrapper)
data class SpotDetailBodyWrapper(val body: SpotDetailItemsWrapper)
data class SpotDetailItemsWrapper(val items: SpotDetailItemListWrapper?)
data class SpotDetailItemListWrapper(
    @SerializedName("item") val itemList: List<SpotDetailItem> = emptyList()
)

data class SpotDetailItem(
    @SerializedName("SGG_NM") val sggName: String?,
    @SerializedName("MNG_ZONE_TRGT_RGN_NM") val dongName: String?,
    @SerializedName("EMSN_PLC_TYPE") val placeType: String?,
    @SerializedName("EMSN_PLC") val placeDetail: String?,

    // 1. 일반 쓰레기 (LF_WST)
    @SerializedName("LF_WST_EMSN_DOW") val generalDays: String?,
    @SerializedName("LF_WST_EMSN_BGNG_TM") val generalStart: String?,
    @SerializedName("LF_WST_EMSN_END_TM") val generalEnd: String?,
    @SerializedName("LF_WST_EMSN_MTHD") val generalMethod: String?,

    // 2. 음식물 쓰레기 (FOD_WST)
    @SerializedName("FOD_WST_EMSN_DOW") val foodDays: String?,
    @SerializedName("FOD_WST_EMSN_BGNG_TM") val foodStart: String?,
    @SerializedName("FOD_WST_EMSN_END_TM") val foodEnd: String?,
    @SerializedName("FOD_WST_EMSN_MTHD") val foodMethod: String?,

    // 3. 재활용품 (RCYCL)
    @SerializedName("RCYCL_EMSN_DOW") val recyclingDays: String?,
    @SerializedName("RCYCL_EMSN_BGNG_TM") val recyclingStart: String?,
    @SerializedName("RCYCL_EMSN_END_TM") val recyclingEnd: String?,
    @SerializedName("RCYCL_EMSN_MTHD") val recyclingMethod: String?,

    // 4. 대형 폐기물 및 기타
    @SerializedName("TMPRY_BULK_WASTE_EMSN_MTHD") val bulkMethod: String?,
    @SerializedName("UNCLLT_DAY") val uncollectedDay: String?
)