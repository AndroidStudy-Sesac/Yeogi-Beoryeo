package com.team.yeogibeoryeo.data.spot.repository

import com.team.yeogibeoryeo.data.spot.geocoder.SpotGeocoder
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionSpotGeocodingRepositoryImplTest {

    @Test
    fun `단일 장소 주소를 geocoding하여 좌표를 설정한다`() = runBlocking {
        val repository = createRepository()
        val spot = collectionSpot(
            id = "battery-bin",
            address = "서울특별시 영등포구 문래동",
        )

        val result = repository.geocodeSpot(spot)

        assertEquals(37.5, result.coordinate?.latitude)
        assertEquals(126.9, result.coordinate?.longitude)
    }

    @Test
    fun `같은 주소는 한 번만 geocoding하고 기존 순서를 유지한다`() = runBlocking {
        val spotGeocoder = FakeSpotGeocoder(
            coordinatesByAddress = mapOf(
                "서울특별시 영등포구 문래동" to Coordinate(
                    latitude = 37.5,
                    longitude = 126.9,
                ),
                "서울특별시 구로구 구로동" to Coordinate(
                    latitude = 37.4,
                    longitude = 126.8,
                ),
            ),
        )
        val repository = createRepository(spotGeocoder)

        val result = repository.geocodeSpots(
            listOf(
                collectionSpot(
                    id = "battery-bin-1",
                    name = "폐건전지 수거함",
                    address = "서울특별시 영등포구 문래동",
                ),
                collectionSpot(
                    id = "battery-bin-2",
                    name = "폐건전지 수거함 2",
                    address = " 서울특별시 영등포구 문래동 ",
                ),
                collectionSpot(
                    id = "recycling-center",
                    name = "재활용센터",
                    address = "서울특별시 구로구 구로동",
                ),
            ),
        )

        assertEquals(
            listOf(
                "폐건전지 수거함",
                "폐건전지 수거함 2",
                "재활용센터",
            ),
            result.map { spot -> spot.name },
        )
        assertEquals(
            listOf(
                "서울특별시 영등포구 문래동",
                "서울특별시 구로구 구로동",
            ),
            spotGeocoder.requestedAddresses,
        )
        assertEquals(result[0].coordinate, result[1].coordinate)
        assertEquals(37.4, result[2].coordinate?.latitude)
    }

    @Test
    fun `괄호가 포함된 도로명 주소는 괄호를 제거한 주소로 geocoding한다`() = runBlocking {
        val spotGeocoder = FakeSpotGeocoder(
            coordinatesByAddress = mapOf(
                "서울특별시 성동구 행당로 35" to Coordinate(
                    latitude = 37.5570,
                    longitude = 127.0332,
                ),
            ),
        )
        val repository = createRepository(spotGeocoder)

        val result = repository.geocodeSpots(
            listOf(
                collectionSpot(
                    id = "standard-bag-store",
                    address = "서울특별시 성동구 행당로 35 (금호동1가, 금북빌딩)",
                ),
            ),
        )

        assertEquals(
            listOf("서울특별시 성동구 행당로 35"),
            spotGeocoder.requestedAddresses,
        )
        assertEquals(37.5570, result.first().coordinate?.latitude)
        assertEquals(127.0332, result.first().coordinate?.longitude)
    }

    @Test
    fun `전남광주통합특별시 광주 5개 구 주소는 광주광역시 주소로 geocoding한다`() = runBlocking {
        val spotGeocoder = FakeSpotGeocoder(
            coordinatesByAddress = mapOf(
                "광주광역시 광산구 하남대로 248-10" to Coordinate(
                    latitude = 35.1771,
                    longitude = 126.8062,
                ),
            ),
        )
        val repository = createRepository(spotGeocoder)

        val spot = collectionSpot(
            id = "gwangju-unnam",
            address = "전남광주통합특별시 광산구 하남대로 248-10",
        )
        val result = repository.geocodeSpot(spot)

        assertEquals(
            listOf("광주광역시 광산구 하남대로 248-10"),
            spotGeocoder.requestedAddresses,
        )
        assertEquals("전남광주통합특별시 광산구 하남대로 248-10", result.address)
        assertEquals(35.1771, result.coordinate?.latitude)
        assertEquals(126.8062, result.coordinate?.longitude)
    }

    @Test
    fun `전남광주통합특별시 전남 시군 주소는 전라남도 주소로 geocoding한다`() = runBlocking {
        val spotGeocoder = FakeSpotGeocoder(
            coordinatesByAddress = mapOf(
                "전라남도 광양시 금호동 1" to Coordinate(
                    latitude = 34.9332,
                    longitude = 127.7253,
                ),
            ),
        )
        val repository = createRepository(spotGeocoder)

        val spot = collectionSpot(
            id = "gwangyang-geumho",
            address = "전남광주통합특별시 광양시 금호동 1",
        )
        val result = repository.geocodeSpot(spot)

        assertEquals(
            listOf("전라남도 광양시 금호동 1"),
            spotGeocoder.requestedAddresses,
        )
        assertEquals("전남광주통합특별시 광양시 금호동 1", result.address)
        assertEquals(34.9332, result.coordinate?.latitude)
        assertEquals(127.7253, result.coordinate?.longitude)
    }

    @Test
    fun `공백 주소는 geocoding하지 않고 좌표 없이 유지한다`() = runBlocking {
        val spotGeocoder = FakeSpotGeocoder()
        val repository = createRepository(spotGeocoder)

        val result = repository.geocodeSpots(
            listOf(
                collectionSpot(
                    id = "blank-address",
                    address = " ",
                ),
            ),
        )

        assertEquals(1, result.size)
        assertNull(result.first().coordinate)
        assertEquals(emptyList<String>(), spotGeocoder.requestedAddresses)
    }

    @Test
    fun `일부 geocoding 실패가 전체 검색 결과를 실패시키지 않는다`() = runBlocking {
        val spotGeocoder = FakeSpotGeocoder(
            coordinatesByAddress = mapOf(
                "서울특별시 구로구 구로동" to Coordinate(
                    latitude = 37.4,
                    longitude = 126.8,
                ),
            ),
            failureAddresses = setOf("서울특별시 영등포구 문래동"),
        )
        val repository = createRepository(spotGeocoder)

        val result = repository.geocodeSpots(
            listOf(
                collectionSpot(
                    id = "battery-bin",
                    name = "폐건전지 수거함",
                    address = "서울특별시 영등포구 문래동",
                ),
                collectionSpot(
                    id = "recycling-center",
                    name = "재활용센터",
                    address = "서울특별시 구로구 구로동",
                ),
            ),
        )

        assertEquals(2, result.size)
        assertEquals("폐건전지 수거함", result[0].name)
        assertNull(result[0].coordinate)
        assertEquals("재활용센터", result[1].name)
        assertEquals(37.4, result[1].coordinate?.latitude)
    }

    private fun createRepository(
        spotGeocoder: SpotGeocoder = FakeSpotGeocoder(),
    ): CollectionSpotGeocodingRepositoryImpl {
        return CollectionSpotGeocodingRepositoryImpl(
            spotGeocoder = spotGeocoder,
        )
    }

    private fun collectionSpot(
        id: String,
        name: String = "수거 장소 $id",
        address: String,
    ): CollectionSpot {
        return CollectionSpot(
            id = id,
            name = name,
            type = CollectionSpotType.STANDARD_BAG_STORE,
            address = address,
            detailLocation = null,
            coordinate = null,
        )
    }

    private class FakeSpotGeocoder(
        private val coordinatesByAddress: Map<String, Coordinate> = emptyMap(),
        private val failureAddresses: Set<String> = emptySet(),
    ) : SpotGeocoder {

        val requestedAddresses = mutableListOf<String>()

        override suspend fun geocode(address: String): Coordinate? {
            requestedAddresses += address

            if (address in failureAddresses) {
                error("geocoding failed")
            }

            return coordinatesByAddress[address] ?: Coordinate(
                latitude = 37.5,
                longitude = 126.9,
            )
        }
    }
}
