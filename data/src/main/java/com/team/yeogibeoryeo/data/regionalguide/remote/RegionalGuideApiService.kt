package com.team.yeogibeoryeo.data.regionalguide.remote

import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideResponseDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideRootDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 행정안전부 지역별 쓰레기 배출 가이드 API 인터페이스 정의.
 * Base URL: https://apis.data.go.kr/
 */
interface RegionalGuideApiService {

    /**
     * 특정 시군구(SGG_NM)의 배출 가이드 정보를 필터링하여 조회합니다.
     *
     * @param serviceKey 공공데이터포털 API 인증키
     * @param pageNo 조회할 페이지 번호 (기본값: 1)
     * @param numOfRows 한 페이지당 데이터 개수 (최대: 100)
     * @param returnType 응답 포맷 (기본값: json)
     * @param sigunguName 검색 조건: 시군구명 필터
     * @return [Response]로 감싸진 최상위 응답 DTO
     */
    @GET("info")
    suspend fun getRegionalGuides(
        @Query("serviceKey") serviceKey: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 100,
        @Query("returnType") returnType: String = "json",
        @Query("cond[SGG_NM::LIKE]") sigunguName: String
    ): Response<RegionalGuideRootDto>
}