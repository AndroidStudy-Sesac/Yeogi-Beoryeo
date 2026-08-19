package com.team.yeogibeoryeo.data.region

import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.data.region.local.dto.LegalAdminDongMappingDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideAvailabilityDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideRegionDto
import com.team.yeogibeoryeo.domain.region.model.Region
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class RegionOptionsRepositoryImplTest {

    @Test
    fun `지역 옵션 조회는 repository 경유로 mapper 결과를 반환한다`() =
        runBlocking {
            val repository = createRepository()

            assertEquals(listOf("서울특별시"), repository.getSidoOptions())
            assertEquals(listOf("영등포구", "중구"), repository.getSigunguOptions("서울특별시"))
            assertEquals(
                listOf("명동", "명동1가"),
                repository.getEupmyeondongOptions("서울특별시", "중구"),
            )
        }

    @Test
    fun `지역 검색과 법정동 후보 조회는 repository 경유로 mapper 결과를 반환한다`() =
        runBlocking {
            val repository = createRepository()

            assertEquals(
                listOf(Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동")),
                repository.findRegionsByEupmyeondongKeyword("명동"),
            )
            assertEquals(
                listOf(Region(sido = "서울특별시", sigungu = "중구")),
                repository.findRegionsBySigunguKeyword("중구"),
            )
            assertEquals(
                listOf("명동1가"),
                repository.findLegalDongKeywordsByRegion(
                    region = Region(sido = "서울특별시", sigungu = "중구"),
                    keyword = "명동",
                ),
            )
            assertEquals(
                listOf(Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동")),
                repository.findAdminDongCandidatesForLegalDong(
                    Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동1가"),
                ),
            )
        }

    @Test
    fun `지역별 가이드 옵션과 검색 결과는 availability 범위를 반영한다`() =
        runBlocking {
            val repository = createRepository()

            assertEquals(listOf("영등포구", "중구"), repository.getRegionalGuideSigunguOptions("서울특별시"))
            assertEquals(
                listOf("명동"),
                repository.getRegionalGuideEupmyeondongOptions("서울특별시", "중구"),
            )
            assertEquals(
                listOf(Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동")),
                repository.findRegionalGuideRegionsByEupmyeondongKeyword("명동"),
            )
            assertEquals(
                listOf(Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동")),
                repository.findAvailableRegionalGuideRegionsByEupmyeondongKeyword("명동"),
            )
            assertEquals(
                listOf(Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동")),
                repository.filterAvailableRegionalGuideRegions(
                    listOf(
                        Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동"),
                        Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동1가"),
                    ),
                ),
            )
        }

    @Test
    fun `지역별 가이드 정규화는 지원 시군구 표기 기준을 따른다`() =
        runBlocking {
            val repository = createRepository(
                regionalGuideRegionOptions = listOf(
                    RegionalGuideRegionDto(sidoName = "서울특별시", sigunguName = "고양시 덕양구"),
                ),
                regionalGuideAvailability = emptyList(),
            )

            assertEquals(
                Region(sido = "서울특별시", sigungu = "덕양구", eupmyeondong = "화정동"),
                repository.normalizeRegionForRegionalGuide(
                    Region(sido = "서울특별시", sigungu = "덕양구", eupmyeondong = "화정동"),
                ),
            )
        }

    @Test
    fun `availability가 비어 있으면 지역별 가이드 옵션 source를 fallback으로 사용한다`() =
        runBlocking {
            val repository = createRepository(
                regionalGuideAvailability = emptyList(),
                regionalGuideRegionOptions = listOf(
                    RegionalGuideRegionDto(sidoName = "대전광역시", sigunguName = "유성구"),
                ),
            )

            assertEquals(listOf("대전광역시"), repository.getSidoOptions())
            assertEquals(
                listOf("명동", "명동1가"),
                repository.getRegionalGuideEupmyeondongOptions("서울특별시", "중구"),
            )
            assertEquals(
                listOf(Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동1가")),
                repository.filterAvailableRegionalGuideRegions(
                    listOf(Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동1가")),
                ),
            )
        }

    @Test
    fun `지역별 가이드 availability 변환은 주입한 dispatcher에서 실행한다`() =
        runBlocking {
            val callingThread = Thread.currentThread()
            var availabilityIterationThread: Thread? = null
            val availability = object : AbstractList<RegionalGuideAvailabilityDto>() {
                private val items = sampleRegionalGuideAvailability()

                override val size: Int
                    get() = items.size

                override fun get(index: Int): RegionalGuideAvailabilityDto = items[index]

                override fun iterator(): Iterator<RegionalGuideAvailabilityDto> {
                    availabilityIterationThread = Thread.currentThread()
                    return super.iterator()
                }
            }

            Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
                val repository = createRepository(
                    regionalGuideAvailability = availability,
                    defaultDispatcher = dispatcher,
                )

                assertEquals(listOf("서울특별시"), repository.getSidoOptions())
            }

            assertNotNull(availabilityIterationThread)
            assertNotSame(callingThread, availabilityIterationThread)
        }

    @Test
    fun `지역 검색 인덱스는 주입한 dispatcher에서 한 번 생성해 재사용한다`() =
        runBlocking {
            val callingThread = Thread.currentThread()
            var administrativeRegionAccessThread: Thread? = null
            var administrativeRegionAccessCount = 0
            val administrativeRegions = object : AbstractList<AdministrativeRegionDto>() {
                private val items = sampleAdministrativeRegions()

                override val size: Int
                    get() = items.size

                override fun get(index: Int): AdministrativeRegionDto {
                    administrativeRegionAccessThread = Thread.currentThread()
                    administrativeRegionAccessCount++
                    return items[index]
                }
            }

            Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
                val repository = createRepository(
                    administrativeRegions = administrativeRegions,
                    legalAdminDongMappings = emptyList(),
                    regionalGuideAvailability = emptyList(),
                    regionalGuideRegionOptions = emptyList(),
                    defaultDispatcher = dispatcher,
                )

                assertEquals(
                    listOf(Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동")),
                    repository.findRegionsByEupmyeondongKeyword("명동"),
                )
                val accessCountAfterFirstSearch = administrativeRegionAccessCount

                assertEquals(
                    listOf(Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동")),
                    repository.findRegionsByEupmyeondongKeyword("명동"),
                )

                assertEquals(accessCountAfterFirstSearch, administrativeRegionAccessCount)
            }

            assertNotNull(administrativeRegionAccessThread)
            assertNotSame(callingThread, administrativeRegionAccessThread)
        }

    @Test
    fun `초기 매핑 이후 취소된 지역 검색 인덱스 생성은 다음 검색을 막지 않는다`() =
        runBlocking {
            val initialMappingCompleted = CountDownLatch(1)
            val allowGrouping = CountDownLatch(1)
            val regionAccessCount = AtomicInteger()
            val waitsForGrouping = AtomicBoolean(true)
            val administrativeRegion = sampleAdministrativeRegions().first()
            val administrativeRegions = object : AbstractList<AdministrativeRegionDto>() {
                override val size: Int = 10_000

                override fun get(index: Int): AdministrativeRegionDto {
                    regionAccessCount.incrementAndGet()
                    if (index == size - 1 && waitsForGrouping.compareAndSet(true, false)) {
                        initialMappingCompleted.countDown()
                        check(allowGrouping.await(3, TimeUnit.SECONDS))
                    }
                    return administrativeRegion
                }
            }

            Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
                val repository = createRepository(
                    administrativeRegions = administrativeRegions,
                    defaultDispatcher = dispatcher,
                )
                val initialSearch = launch(dispatcher) {
                    repository.findRegionsByEupmyeondongKeyword("명동")
                }

                assertTrue(initialMappingCompleted.await(3, TimeUnit.SECONDS))
                initialSearch.cancel()
                val nextSearch = async(dispatcher) {
                    repository.findRegionsByEupmyeondongKeyword("명동")
                }
                allowGrouping.countDown()
                initialSearch.cancelAndJoin()

                assertEquals(
                    listOf(Region(sido = "서울특별시", sigungu = "중구", eupmyeondong = "명동")),
                    withTimeout(3_000) {
                        nextSearch.await()
                    },
                )
            }

            assertTrue(regionAccessCount.get() >= administrativeRegions.size * 2)
        }

    private fun createRepository(
        administrativeRegions: List<AdministrativeRegionDto> = sampleAdministrativeRegions(),
        legalAdminDongMappings: List<LegalAdminDongMappingDto> = sampleLegalAdminDongMappings(),
        regionalGuideAvailability: List<RegionalGuideAvailabilityDto> = sampleRegionalGuideAvailability(),
        regionalGuideRegionOptions: List<RegionalGuideRegionDto> = sampleRegionalGuideRegionOptions(),
        defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    ): RegionOptionsRepositoryImpl =
        RegionOptionsRepositoryImpl(
            getAdministrativeRegions = { administrativeRegions },
            getLegalAdminDongMappings = { legalAdminDongMappings },
            getRegionalGuideAvailabilityRegions = { regionalGuideAvailability },
            getRegionalGuideRegionOptions = { regionalGuideRegionOptions },
            defaultDispatcher = defaultDispatcher,
        )

    private fun sampleAdministrativeRegions(): List<AdministrativeRegionDto> =
        listOf(
            administrativeRegion(
                adminCode = "1114055000",
                sidoName = "서울특별시",
                sigunguName = "중구",
                eupmyeondongName = "명동",
            ),
            administrativeRegion(
                adminCode = "1114057000",
                sidoName = "서울특별시",
                sigunguName = "중구",
                eupmyeondongName = "명동1가",
            ),
            administrativeRegion(
                adminCode = "1156060500",
                sidoName = "서울특별시",
                sigunguName = "영등포구",
                eupmyeondongName = "문래동",
            ),
        )

    private fun sampleLegalAdminDongMappings(): List<LegalAdminDongMappingDto> =
        listOf(
            LegalAdminDongMappingDto(
                legalCode = "1114010100",
                legalDongName = "명동1가",
                adminCode = "1114055000",
                sidoName = "서울특별시",
                sigunguName = "중구",
                adminDongName = "명동",
                adminFullName = "서울특별시 중구 명동",
                legalFullName = "서울특별시 중구 명동1가",
            ),
        )

    private fun sampleRegionalGuideAvailability(): List<RegionalGuideAvailabilityDto> =
        listOf(
            RegionalGuideAvailabilityDto(
                sidoName = "서울특별시",
                sigunguName = "중구",
                managementZoneName = "명동",
                targetRegionName = "명동",
            ),
            RegionalGuideAvailabilityDto(
                sidoName = "서울특별시",
                sigunguName = "영등포구",
                managementZoneName = "문래동",
                targetRegionName = "문래동",
            ),
        )

    private fun sampleRegionalGuideRegionOptions(): List<RegionalGuideRegionDto> =
        listOf(
            RegionalGuideRegionDto(sidoName = "서울특별시", sigunguName = "중구"),
            RegionalGuideRegionDto(sidoName = "서울특별시", sigunguName = "영등포구"),
        )

    private fun administrativeRegion(
        adminCode: String,
        sidoName: String,
        sigunguName: String,
        eupmyeondongName: String,
    ): AdministrativeRegionDto =
        AdministrativeRegionDto(
            adminCode = adminCode,
            sidoName = sidoName,
            sigunguName = sigunguName,
            eupmyeondongName = eupmyeondongName,
            fullName = "$sidoName $sigunguName $eupmyeondongName",
        )
}
