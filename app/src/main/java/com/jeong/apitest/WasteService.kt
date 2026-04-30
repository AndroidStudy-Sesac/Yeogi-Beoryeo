package com.jeong.apitest

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

enum class SearchType { ITEM, SPOT, HOUSEHOLD_INFO }

// 기후에너지환경부 API
interface WasteService {
    @GET("getItem")
    suspend fun getItem(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("itemNm") itemNm: String
    ): WasteResponse<ItemResponse>

    @GET("getSpot")
    suspend fun getSpot(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("addr") addr: String,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("radius") radius: Int? = null
    ): WasteResponse<SpotResponse>
}

// 행정안전부 API
interface HouseholdWasteService {
    @GET("info")
    suspend fun getInfo(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int,
        @Query("numOfRows") numOfRows: Int,
        @Query("returnType") returnType: String = "JSON",
        @Query("cond[SGG_NM::LIKE]") sggNameQuery: String? = null,
        @Query("cond[DAT_UPDT_PNT::GTE]") updatedFrom: String? = null,
        @Query("cond[DAT_UPDT_PNT::LT]") updatedUntil: String? = null,
        @Query("cond[OPN_ATMY_GRP_CD::EQ]") administrativeCode: String? = null,
        @Query("cond[DAT_CRTR_YMD::GTE]") baseDateFrom: String? = null,
        @Query("cond[DAT_CRTR_YMD::LT]") baseDateUntil: String? = null
    ): WasteResponse<HouseholdWasteInfo>
}

@Serializable
data class WasteResponse<T>(
    val response: ResponseData<T>
)

@Serializable
data class ResponseData<T>(
    val header: Header,
    val body: Body<T>
)

@Serializable
data class Header(
    val resultCode: String,
    val resultMsg: String
)

@Serializable
data class Body<T>(
    val items: Items<T>? = null,
    val pageNo: Int,
    val numOfRows: Int,
    val totalCount: Int,
    val dataType: String? = null
)

@Serializable
data class Items<T>(
    val item: List<T> = emptyList()
)

// 모델 1: 품목 정보
@Serializable
data class ItemResponse(
    val itemNm: String,
    val dschgMthd: String
)

// 모델 2: 배출 장소
@Serializable
data class SpotResponse(
    val spotNm: String,
    val addrBase: String,
    val addrDtl: String
)

// 모델 3: 생활쓰레기배출정보
@Serializable
data class HouseholdWasteInfo(
    val CTPV_NM: String? = null,                  // 시도명
    val SGG_NM: String? = null,                   // 시군구명
    val MNG_ZONE_NM: String? = null,              // 관리구역명
    val MNG_ZONE_TRGT_RGN_NM: String? = null,     // 관리구역대상지역명
    val EMSN_PLC_TYPE: String? = null,            // 배출장소유형
    val EMSN_PLC: String? = null,                 // 배출장소
    val LF_WST_EMSN_MTHD: String? = null,         // 생활쓰레기배출방법
    val FOD_WST_EMSN_MTHD: String? = null,        // 음식물쓰레기배출방법
    val RCYCL_EMSN_MTHD: String? = null,          // 재활용품배출방법
    val TMPRY_BULK_WASTE_EMSN_MTHD: String? = null, // 일시적다량폐기물배출방법
    val TMPRY_BULK_WASTE_EMSN_PLC: String? = null,  // 일시적다량폐기물배출장소
    val LF_WST_EMSN_DOW: String? = null,          // 생활쓰레기배출요일
    val FOD_WST_EMSN_DOW: String? = null,         // 음식물쓰레기배출요일
    val RCYCL_EMSN_DOW: String? = null,           // 재활용품배출요일
    val LF_WST_EMSN_BGNG_TM: String? = null,      // 생활쓰레기배출시작시각
    val LF_WST_EMSN_END_TM: String? = null,       // 생활쓰레기배출종료시각
    val FOD_WST_EMSN_BGNG_TM: String? = null,     // 음식물쓰레기배출시작시각
    val FOD_WST_EMSN_END_TM: String? = null,      // 음식물쓰레기배출종료시각
    val RCYCL_EMSN_BGNG_TM: String? = null,       // 재활용품배출시작시각
    val RCYCL_EMSN_END_TM: String? = null,        // 재활용품배출종료시각
    val TMPRY_BULK_WASTE_EMSN_BGNG_TM: String? = null, // 일시적다량배출시작
    val TMPRY_BULK_WASTE_EMSN_END_TM: String? = null,  // 일시적다량배출종료
    val UNCLLT_DAY: String? = null,               // 미수거일
    val MNG_DEPT_NM: String? = null,              // 관리부서명
    val MNG_DEPT_TELNO: String? = null,           // 관리부서전화번호
    val OPN_ATMY_GRP_CD: String? = null,          // 개방자치단체코드
    val MNG_NO: String? = null,                   // 관리번호
    val DAT_CRTR_YMD: String? = null,             // 데이터기준일자
    val DAT_UPDT_SE: String? = null,              // 데이터갱신구분
    val DAT_UPDT_PNT: String? = null,             // 데이터갱신시점
    val LAST_MDFCN_PNT: String? = null            // 최종수정시점
)
