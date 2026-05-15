package com.team.yeogibeoryeo.domain.regionalguide.repository

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide

/**
 * 특정 지역(Region)에 대한 배출 가이드 데이터를 제공하는 Repository 인터페이스
 */
interface RegionalDisposalGuideRepository {

    /**
     * 특정 행정구역의 쓰레기 배출 가이드 정보를 비동기로 조회합니다.
     * 향후 네트워크 API 통신을 고려하여 suspend 함수로 설계합니다.
     *
     * @param region 조회할 대상 지역
     * @return 성공 시 [RegionalDisposalGuide], 해당 지역 정보가 없거나 실패 시 null
     */
    suspend fun getRegionalDisposalGuide(region: Region): RegionalDisposalGuide?
}