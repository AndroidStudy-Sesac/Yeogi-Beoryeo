package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotSearchResult
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidate
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotGeocodingRepository
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchCollectionSpotsByKeywordUseCaseTest {

    private val repository = FakeCollectionSpotRepository()
    private val useCase = SearchCollectionSpotsByKeywordUseCase(
        repository = repository,
        geocodingRepository = repository,
        normalizeKeywordUseCase = NormalizeCollectionSpotSearchKeywordUseCase(),
    )

    @Test
    fun `명동 검색에서 괄호 속 봉명동 결과는 제외한다`() =
        runSuspendTest {
            val myeongDongSpot = collectionSpot(
                id = "myeongdong",
                address = "서울특별시 중구 명동길 26 (명동)",
            )
            val bongMyeongDongSpot = collectionSpot(
                id = "bongmyeongdong",
                address = "충청북도 청주시 흥덕구 송절로124번길 65 (봉명동)",
            )
            repository.keywordSpots = listOf(myeongDongSpot, bongMyeongDongSpot)

            val result = useCase(keyword = "명동")

            assertEquals(listOf("명동"), repository.keywords)
            assertEquals(listOf(myeongDongSpot), result)
        }

    @Test
    fun `명시 지역 필터에서 제외되는 결과는 geocoding하지 않는다`() =
        runSuspendTest {
            val myeongDongSpot = collectionSpot(
                id = "myeongdong",
                address = "서울특별시 중구 명동길 26 (명동)",
                coordinate = null,
            )
            val bongMyeongDongSpot = collectionSpot(
                id = "bongmyeongdong",
                address = "충청북도 청주시 흥덕구 송절로124번길 65 (봉명동)",
                coordinate = null,
            )
            repository.keywordSpots = listOf(myeongDongSpot, bongMyeongDongSpot)

            val result = useCase(keyword = "명동")

            assertEquals(listOf("myeongdong"), repository.geocodedSpotIds)
            assertEquals(listOf("myeongdong"), result.map { spot -> spot.id })
            assertEquals(DEFAULT_COORDINATE, result.first().coordinate)
        }

    @Test
    fun `명동 검색에서 번지에 붙은 괄호 속 봉명동 결과는 제외한다`() =
        runSuspendTest {
            val spot = collectionSpot(
                id = "bongmyeongdong-attached-parentheses",
                address = "충청북도 청주시 흥덕구 직지대로 609(봉명동)",
            )
            repository.keywordSpots = listOf(spot)

            val result = useCase(keyword = "명동")

            assertEquals(emptyList<CollectionSpot>(), result)
        }

    @Test
    fun `명동 검색에서 주소 본문 속 봉명동 결과는 제외한다`() =
        runSuspendTest {
            val spot = collectionSpot(
                id = "bongmyeongdong-body",
                address = "충청북도 청주시 흥덕구 봉명동 1순환로584번길 59",
            )
            repository.keywordSpots = listOf(spot)

            val result = useCase(keyword = "명동")

            assertEquals(emptyList<CollectionSpot>(), result)
        }

    @Test
    fun `명동 검색에서 명동1가 같은 법정동 결과는 유지한다`() =
        runSuspendTest {
            val spot = collectionSpot(
                id = "myeongdong-1ga",
                address = "서울특별시 중구 명동길 14 (명동1가)",
            )
            repository.keywordSpots = listOf(spot)

            val result = useCase(keyword = "명동")

            assertEquals(listOf(spot), result)
        }

    @Test
    fun `동 정보가 없는 도로명 주소 결과는 유지한다`() =
        runSuspendTest {
            val spot = collectionSpot(
                id = "road-address",
                address = "서울특별시 성동구 독서당로 303-5",
            )
            repository.keywordSpots = listOf(spot)

            val result = useCase(keyword = "성동구 금호동")

            assertEquals(listOf("금호동"), repository.keywords)
            assertEquals(listOf(spot), result)
        }

    @Test
    fun `복합 설명에서 명확한 동명이 검색어와 다르면 제외한다`() =
        runSuspendTest {
            val spot = collectionSpot(
                id = "complex-parentheses",
                address = "충청북도 청주시 흥덕구 월명로 76 (봉명동, 청주 SK VIEW 자이)",
            )
            repository.keywordSpots = listOf(spot)

            val result = useCase(keyword = "명동")

            assertEquals(emptyList<CollectionSpot>(), result)
        }

    @Test
    fun `선택한 지역 후보와 명확히 다른 시도 결과는 제외한다`() =
        runSuspendTest {
            val seoulSpot = collectionSpot(
                id = "seoul-myeongdong",
                address = "서울특별시 중구 명동길 3",
            )
            val jecheonSpot = collectionSpot(
                id = "jecheon-myeongdong",
                address = "충청북도 제천시 명동 1",
            )
            repository.keywordSpots = listOf(seoulSpot, jecheonSpot)

            val result = useCase(
                keyword = "명동",
                selectedRegionCandidate = MapRegionSearchCandidate(
                    region = Region(
                        sido = "서울특별시",
                        sigungu = "중구",
                        eupmyeondong = "명동",
                    ),
                    searchKeyword = "명동",
                ),
            )

            assertEquals(listOf(seoulSpot), result)
        }

    @Test
    fun `선택 지역 필터에서 제외되는 결과는 geocoding하지 않는다`() =
        runSuspendTest {
            val seoulSpot = collectionSpot(
                id = "seoul-myeongdong",
                address = "서울특별시 중구 명동길 3",
                coordinate = null,
            )
            val jecheonSpot = collectionSpot(
                id = "jecheon-myeongdong",
                address = "충청북도 제천시 명동 1",
                coordinate = null,
            )
            repository.keywordSpots = listOf(seoulSpot, jecheonSpot)

            val result = useCase(
                keyword = "명동",
                selectedRegionCandidate = MapRegionSearchCandidate(
                    region = Region(
                        sido = "서울특별시",
                        sigungu = "중구",
                        eupmyeondong = "명동",
                    ),
                    searchKeyword = "명동",
                ),
            )

            assertEquals(listOf("seoul-myeongdong"), repository.geocodedSpotIds)
            assertEquals(listOf("seoul-myeongdong"), result.map { spot -> spot.id })
            assertEquals(DEFAULT_COORDINATE, result.first().coordinate)
        }

    @Test
    fun `광주 후보 선택 시 전남광주통합특별시 광주 5개 구 결과만 유지한다`() =
        runSuspendTest {
            val gwangjuSpot = collectionSpot(
                id = "gwangju-geumho",
                address = "전남광주통합특별시 서구 풍금로151번길 14(금호동)",
                coordinate = null,
            )
            val jeonnamSpot = collectionSpot(
                id = "gwangyang-geumho",
                address = "전남광주통합특별시 광양시 금호동 1",
                coordinate = null,
            )
            repository.keywordSpots = listOf(gwangjuSpot, jeonnamSpot)

            val result = useCase(
                keyword = "금호동",
                selectedRegionCandidate = MapRegionSearchCandidate(
                    region = Region(
                        sido = "광주광역시",
                        sigungu = "서구",
                        eupmyeondong = "금호동",
                    ),
                    searchKeyword = "금호동",
                ),
            )

            assertEquals(listOf("gwangju-geumho"), repository.geocodedSpotIds)
            assertEquals(listOf("gwangju-geumho"), result.map { spot -> spot.id })
        }

    @Test
    fun `전남광주통합특별시 광주 후보 선택 시 광주 5개 구 결과만 유지한다`() =
        runSuspendTest {
            val gwangjuSpot = collectionSpot(
                id = "gwangju-geumho",
                address = "전남광주통합특별시 서구 풍금로151번길 14(금호동)",
                coordinate = null,
            )
            val jeonnamSpot = collectionSpot(
                id = "gwangyang-geumho",
                address = "전남광주통합특별시 광양시 금호동 1",
                coordinate = null,
            )
            repository.keywordSpots = listOf(gwangjuSpot, jeonnamSpot)

            val result = useCase(
                keyword = "금호동",
                selectedRegionCandidate = MapRegionSearchCandidate(
                    region = Region(
                        sido = "전남광주통합특별시",
                        sigungu = "서구",
                        eupmyeondong = "금호동",
                    ),
                    searchKeyword = "금호동",
                ),
            )

            assertEquals(listOf("gwangju-geumho"), repository.geocodedSpotIds)
            assertEquals(listOf("gwangju-geumho"), result.map { spot -> spot.id })
        }

    @Test
    fun `전남 후보 선택 시 전남광주통합특별시 전남 시군 결과만 유지한다`() =
        runSuspendTest {
            val gwangjuSpot = collectionSpot(
                id = "gwangju-geumho",
                address = "전남광주통합특별시 서구 풍금로151번길 14(금호동)",
                coordinate = null,
            )
            val jeonnamSpot = collectionSpot(
                id = "gwangyang-geumho",
                address = "전남광주통합특별시 광양시 금호동 1",
                coordinate = null,
            )
            repository.keywordSpots = listOf(gwangjuSpot, jeonnamSpot)

            val result = useCase(
                keyword = "금호동",
                selectedRegionCandidate = MapRegionSearchCandidate(
                    region = Region(
                        sido = "전라남도",
                        sigungu = "광양시",
                        eupmyeondong = "금호동",
                    ),
                    searchKeyword = "금호동",
                ),
            )

            assertEquals(listOf("gwangyang-geumho"), repository.geocodedSpotIds)
            assertEquals(listOf("gwangyang-geumho"), result.map { spot -> spot.id })
        }

    @Test
    fun `전남광주통합특별시 전남 후보 선택 시 전남 시군 결과만 유지한다`() =
        runSuspendTest {
            val gwangjuSpot = collectionSpot(
                id = "gwangju-geumho",
                address = "전남광주통합특별시 서구 풍금로151번길 14(금호동)",
                coordinate = null,
            )
            val jeonnamSpot = collectionSpot(
                id = "gwangyang-geumho",
                address = "전남광주통합특별시 광양시 금호동 1",
                coordinate = null,
            )
            repository.keywordSpots = listOf(gwangjuSpot, jeonnamSpot)

            val result = useCase(
                keyword = "금호동",
                selectedRegionCandidate = MapRegionSearchCandidate(
                    region = Region(
                        sido = "전남광주통합특별시",
                        sigungu = "광양시",
                        eupmyeondong = "금호동",
                    ),
                    searchKeyword = "금호동",
                ),
            )

            assertEquals(listOf("gwangyang-geumho"), repository.geocodedSpotIds)
            assertEquals(listOf("gwangyang-geumho"), result.map { spot -> spot.id })
        }

    @Test
    fun `선택 지역 후보가 있어도 지역 범위가 불명확한 도로명 주소 결과는 유지한다`() =
        runSuspendTest {
            val roadAddressSpot = collectionSpot(
                id = "road-address",
                address = "명동길 3",
            )
            repository.keywordSpots = listOf(roadAddressSpot)

            val result = useCase(
                keyword = "명동",
                selectedRegionCandidate = MapRegionSearchCandidate(
                    region = Region(
                        sido = "서울특별시",
                        sigungu = "중구",
                        eupmyeondong = "명동",
                    ),
                    searchKeyword = "명동",
                ),
            )

            assertEquals(listOf(roadAddressSpot), result)
        }

    private fun runSuspendTest(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking {
            block()
        }
    }

    private fun collectionSpot(
        id: String,
        address: String,
        coordinate: Coordinate? = Coordinate(latitude = 37.5666102, longitude = 126.9783881),
    ): CollectionSpot {
        return CollectionSpot(
            id = id,
            name = "수거 장소 $id",
            type = CollectionSpotType.STANDARD_BAG_STORE,
            address = address,
            detailLocation = null,
            coordinate = coordinate,
        )
    }

    private class FakeCollectionSpotRepository : CollectionSpotRepository, CollectionSpotGeocodingRepository {
        var keywordSpots: List<CollectionSpot> = emptyList()
        val keywords = mutableListOf<String>()
        val geocodedSpotIds = mutableListOf<String>()

        override suspend fun searchRawByKeyword(
            keyword: String,
            types: Set<CollectionSpotType>,
        ) = CollectionSpotSearchResult(
            spots = keywordSpots,
        ).also {
            keywords += keyword
        }

        override suspend fun searchRawByLocation(
            coordinate: Coordinate,
            radiusMeter: Int,
            types: Set<CollectionSpotType>,
        ): List<CollectionSpot> = emptyList()

        override suspend fun geocodeSpot(spot: CollectionSpot): CollectionSpot {
            geocodedSpotIds += spot.id
            return spot.copy(
                coordinate = spot.coordinate ?: DEFAULT_COORDINATE,
            )
        }
    }

    private companion object {
        val DEFAULT_COORDINATE = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
    }
}
