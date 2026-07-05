package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchCollectionSpotsByKeywordUseCaseTest {

    private val repository = FakeCollectionSpotRepository()
    private val useCase = SearchCollectionSpotsByKeywordUseCase(
        repository = repository,
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

    private fun runSuspendTest(block: suspend () -> Unit) {
        kotlinx.coroutines.runBlocking {
            block()
        }
    }

    private fun collectionSpot(
        id: String,
        address: String,
    ): CollectionSpot {
        return CollectionSpot(
            id = id,
            name = "수거 장소 $id",
            type = CollectionSpotType.STANDARD_BAG_STORE,
            address = address,
            detailLocation = null,
            coordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881),
        )
    }

    private class FakeCollectionSpotRepository : CollectionSpotRepository {
        var keywordSpots: List<CollectionSpot> = emptyList()
        val keywords = mutableListOf<String>()

        override suspend fun searchByKeyword(
            keyword: String,
            types: Set<CollectionSpotType>,
        ): List<CollectionSpot> {
            keywords += keyword
            return keywordSpots
        }

        override suspend fun searchByLocation(
            coordinate: Coordinate,
            radiusMeter: Int,
            types: Set<CollectionSpotType>,
        ): List<CollectionSpot> = emptyList()

        override suspend fun geocodeSpot(spot: CollectionSpot): CollectionSpot = spot
    }
}
