package com.team.yeogibeoryeo.data.item.repository

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.item.local.ItemCategoryLocalSource
import com.team.yeogibeoryeo.data.item.local.ItemGuideDetail
import com.team.yeogibeoryeo.data.item.local.WasteDictionaryItem
import com.team.yeogibeoryeo.data.item.remote.ItemApiService
import com.team.yeogibeoryeo.data.item.remote.datasource.ItemRemoteDataSource
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideBodyDto
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideDto
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideHeaderDto
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideItemsDto
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideResponseBodyDto
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideResponseDto
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisposalItemGuideRepositoryImplTest {
    @Test
    fun `searchItemGuides는 원격 호출 전에 동의어를 검색어로 치환한다`() =
        runBlocking {
            val capturedQueries = mutableListOf<String>()
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = recordingRemote(capturedQueries, results = listOf("음료캔" to "재활용폐기물")),
                    localDataSource =
                        FakeLocalSource(
                            synonyms = mapOf("캔" to "음료캔"),
                            categoryMap = mapOf("음료캔" to (DisposalCategory.METAL to DisposalSubCategory.ALUMINUM_CAN)),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.searchItemGuides("캔")

            assertEquals(listOf("음료캔"), capturedQueries)
            assertEquals(1, results.size)
            assertEquals("음료캔", results.first().name)
            assertEquals(DisposalCategory.METAL, results.first().category)
        }

    @Test
    fun `searchItemGuides는 동의어가 없으면 원본 검색어를 사용한다`() =
        runBlocking {
            val capturedQueries = mutableListOf<String>()
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = recordingRemote(capturedQueries, results = listOf("종이" to "재활용폐기물")),
                    localDataSource = FakeLocalSource(),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            repository.searchItemGuides("종이")

            assertEquals(listOf("종이"), capturedQueries)
        }

    @Test
    fun `searchItemGuides는 검색어 공백 제거 후 동의어 치환과 원격 호출을 수행한다`() =
        runBlocking {
            val capturedQueries = mutableListOf<String>()
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = recordingRemote(capturedQueries, results = listOf("핸드폰" to "재활용폐기물")),
                    localDataSource =
                        FakeLocalSource(
                            synonyms = mapOf("휴대폰" to "핸드폰"),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            repository.searchItemGuides("  휴대폰  ")

            assertEquals(listOf("핸드폰"), capturedQueries)
        }

    @Test
    fun `searchItemGuides는 빈 검색어면 원격 호출 없이 빈 목록을 반환한다`() =
        runBlocking {
            val capturedQueries = mutableListOf<String>()
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = recordingRemote(capturedQueries, results = listOf("종이" to "재활용폐기물")),
                    localDataSource = FakeLocalSource(),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.searchItemGuides("   ")

            assertTrue(results.isEmpty())
            assertTrue(capturedQueries.isEmpty())
        }

    @Test
    fun `searchItemGuides는 품목사전 결과가 있으면 원격 API보다 우선 사용한다`() =
        runBlocking {
            val capturedQueries = mutableListOf<String>()
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = recordingRemote(capturedQueries, results = listOf("내열냄비" to "일반쓰레기")),
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "내열냄비",
                                        categoryPaths = listOf(listOf("일반폐기물", "불연성종량제폐기물")),
                                        dischargeMethods = listOf("내열냄비는 불연성 종량제봉투(마대)로 배출합니다."),
                                    ),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.searchItemGuides("내열냄비")

            assertTrue(capturedQueries.isEmpty())
            assertEquals(1, results.size)
            assertEquals(
                "내열냄비는 불연성 종량제봉투(마대)로 배출합니다.",
                results.first().instructions.first().method,
            )
        }

    @Test
    fun `searchItemGuides는 품목사전 유사 품목도 검색한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "아이스박스",
                                        similarItems = listOf("택배 박스（스티로폼）"),
                                        categoryPaths = listOf(listOf("일반폐기물", "일반종량제폐기물")),
                                        dischargeMethods = listOf("아이스박스는 종량제봉투로 배출합니다."),
                                    ),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.searchItemGuides("박스")

            assertEquals(listOf("아이스박스"), results.map { it.name })
        }

    @Test
    fun `searchItemGuides는 원격 결과를 이름 기준으로 중복 제거한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource =
                        fakeRemote(
                            results =
                                listOf(
                                    "종이" to "재활용폐기물",
                                    "종이" to "재활용폐기물",
                                ),
                        ),
                    localDataSource = FakeLocalSource(),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.searchItemGuides("종이")

            assertEquals(1, results.size)
        }

    @Test
    fun `searchItemGuides는 원격 결과가 비어 있으면 로컬 품목으로 보완한다`() =
        runBlocking {
            val localItem = sampleLocalItem(name = "이불", category = DisposalCategory.LARGE_WASTE)
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource = FakeLocalSource(localItems = listOf(localItem)),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.searchItemGuides("이불")

            assertEquals(1, results.size)
            assertEquals("이불", results.first().name)
        }

    @Test
    fun `searchItemGuides 로컬 보완은 원본 검색어와 치환 검색어를 모두 비교한다`() =
        runBlocking {
            val localItem = sampleLocalItem(name = "음료캔", category = DisposalCategory.METAL)
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            synonyms = mapOf("캔" to "음료캔"),
                            localItems = listOf(localItem),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.searchItemGuides("캔")

            assertEquals(1, results.size)
            assertEquals("음료캔", results.first().name)
        }

    @Test
    fun `searchItemGuides는 원격 결과가 있으면 로컬 품목을 반환하지 않는다`() =
        runBlocking {
            val localItem = sampleLocalItem(name = "종이", category = DisposalCategory.PAPER)
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = listOf("종이" to "재활용폐기물")),
                    localDataSource = FakeLocalSource(localItems = listOf(localItem)),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.searchItemGuides("종이")

            assertEquals(1, results.size)
            assertTrue(results.first().instructions.isNotEmpty())
        }

    @Test
    fun `getCategoryGuides는 원격 API를 호출하지 않는다`() =
        runBlocking {
            val capturedQueries = mutableListOf<String>()
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = recordingRemote(capturedQueries, results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            categoryMap =
                                mapOf(
                                    "신문지" to (DisposalCategory.PAPER to DisposalSubCategory.NEWSPAPER),
                                    "골판지" to (DisposalCategory.PAPER to DisposalSubCategory.CARDBOARD),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            repository.getCategoryGuides(DisposalCategory.PAPER)

            assertTrue("원격 API가 호출되면 안 됩니다", capturedQueries.isEmpty())
        }

    @Test
    fun `getCategoryGuides는 category_map 기반 초기 가이드를 반환한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            categoryMap =
                                mapOf(
                                    "신문지" to (DisposalCategory.PAPER to DisposalSubCategory.NEWSPAPER),
                                    "유리병" to (DisposalCategory.GLASS to DisposalSubCategory.GLASS_BOTTLE),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.getCategoryGuides(DisposalCategory.PAPER)

            assertEquals(1, results.size)
            assertEquals("신문지", results.first().name)
            assertEquals(DisposalSubCategory.NEWSPAPER, results.first().subCategory)
        }

    @Test
    fun `getCategoryGuides는 재활용 카테고리를 재활용 가능으로 표시한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            categoryMap = mapOf("신문지" to (DisposalCategory.PAPER to DisposalSubCategory.NEWSPAPER)),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.getCategoryGuides(DisposalCategory.PAPER)

            assertTrue(results.first().isRecyclable)
        }

    @Test
    fun `getCategoryGuides는 비재활용 카테고리를 재활용 불가로 표시한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            categoryMap = mapOf("이불" to (DisposalCategory.LARGE_WASTE to null)),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.getCategoryGuides(DisposalCategory.LARGE_WASTE)

            assertFalse(results.first().isRecyclable)
        }

    @Test
    fun `getCategoryGuides는 별칭으로 상세 가이드를 보강한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            categoryMap = mapOf("골판지" to (DisposalCategory.PAPER to DisposalSubCategory.CARDBOARD)),
                            guideDetailAliases = mapOf("골판지" to "종이"),
                            guideDetails =
                                mapOf(
                                    "종이" to
                                        ItemGuideDetail(
                                            steps = listOf("물기 제거"),
                                            cautions = listOf("기름 묻은 종이 제외"),
                                            tip = "상자는 펼쳐서",
                                            relatedSpotTypes = listOf(RelatedSpotType.RECYCLING_BIN),
                                        ),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.getCategoryGuides(DisposalCategory.PAPER)

            assertEquals(listOf("물기 제거"), results.first().steps)
            assertEquals(listOf("기름 묻은 종이 제외"), results.first().cautions)
            assertEquals("상자는 펼쳐서", results.first().tip)
            assertEquals(listOf(RelatedSpotType.RECYCLING_BIN), results.first().relatedSpotTypes)
        }

    @Test
    fun `getCategoryGuides는 초기 가이드와 로컬 품목을 중복 없이 합친다`() =
        runBlocking {
            val localItem = sampleLocalItem(name = "이불", category = DisposalCategory.LARGE_WASTE)
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            categoryMap = mapOf("이불" to (DisposalCategory.LARGE_WASTE to null)),
                            localItems = listOf(localItem),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.getCategoryGuides(DisposalCategory.LARGE_WASTE)

            assertEquals(1, results.size)
        }

    @Test
    fun `getCategoryGuides는 상세 가이드가 없으면 관련 장소 맵을 사용한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            categoryMap = mapOf("냉장고" to (DisposalCategory.ELECTRONICS to DisposalSubCategory.LARGE_APPLIANCE)),
                            relatedSpots = mapOf("냉장고" to listOf(RelatedSpotType.FREE_PICKUP, RelatedSpotType.E_WASTE_BIN)),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.getCategoryGuides(DisposalCategory.ELECTRONICS)

            assertEquals(
                listOf(RelatedSpotType.FREE_PICKUP, RelatedSpotType.E_WASTE_BIN),
                results.first().relatedSpotTypes,
            )
        }

    @Test
    fun `getCategoryGuides는 관련 장소 정보가 없으면 relatedSpotTypes를 null로 둔다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            categoryMap = mapOf("미상" to (DisposalCategory.OTHER to null)),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.getCategoryGuides(DisposalCategory.OTHER)

            assertNull(results.first().relatedSpotTypes)
        }

    @Test
    fun `getCategories는 모든 도메인 카테고리를 반환한다`() {
        val repository =
            DisposalItemGuideRepositoryImpl(
                remoteDataSource = fakeRemote(results = emptyList()),
                localDataSource = FakeLocalSource(),
                publicDataKeyProvider = fakePublicDataKeyProvider,
            )

        assertEquals(DisposalCategory.entries.toList(), repository.getCategories())
    }

    private fun fakeRemote(results: List<Pair<String, String>>): ItemRemoteDataSource =
        ItemRemoteDataSource(
            object : ItemApiService {
                override suspend fun getItem(
                    serviceKey: String,
                    pageNo: Int,
                    numOfRows: Int,
                    itemNm: String,
                ): ItemGuideResponseDto = buildResponse(results)
            },
        )

    private fun recordingRemote(
        captured: MutableList<String>,
        results: List<Pair<String, String>>,
    ): ItemRemoteDataSource =
        ItemRemoteDataSource(
            object : ItemApiService {
                override suspend fun getItem(
                    serviceKey: String,
                    pageNo: Int,
                    numOfRows: Int,
                    itemNm: String,
                ): ItemGuideResponseDto {
                    captured += itemNm
                    return buildResponse(results)
                }
            },
        )

    private fun buildResponse(results: List<Pair<String, String>>): ItemGuideResponseDto {
        val items = results.map { (name, method) -> ItemGuideDto(itemNm = name, dschgMthd = method) }
        return ItemGuideResponseDto(
            response =
                ItemGuideResponseBodyDto(
                    header =
                        ItemGuideHeaderDto(
                            resultCode = if (items.isEmpty()) "03" else "00",
                            resultMsg = if (items.isEmpty()) "NODATA_ERROR" else "NORMAL SERVICE.",
                        ),
                    body =
                        ItemGuideBodyDto(
                            items = ItemGuideItemsDto(item = items),
                            numOfRows = 100,
                            pageNo = 1,
                            totalCount = items.size,
                        ),
                ),
        )
    }

    private fun sampleLocalItem(
        name: String,
        category: DisposalCategory,
    ): DisposalItemGuide =
        DisposalItemGuide(
            id = "local_$name",
            name = name,
            category = category,
            subCategory = null,
            instructions = listOf(DisposalInstruction(method = "종량제봉투")),
            steps = emptyList(),
            cautions = emptyList(),
            tip = null,
            isRecyclable = false,
            relatedSpotTypes = null,
        )

    private fun sampleDictionaryItem(
        name: String,
        categoryPaths: List<List<String>>,
        dischargeMethods: List<String>,
        similarItems: List<String> = emptyList(),
    ): WasteDictionaryItem =
        WasteDictionaryItem(
            name = name,
            categoryPaths = categoryPaths,
            similarItems = similarItems,
            dischargeMethods = dischargeMethods,
            features = emptyList(),
            notes = emptyList(),
        )

    private val fakePublicDataKeyProvider =
        object : AppKeyProvider {
            override val publicDataServiceKey: String = "key"
            override val naverClientId: String = "naver-client-id"
        }

    private class FakeLocalSource(
        private val categoryMap: Map<String, Pair<DisposalCategory, DisposalSubCategory?>> = emptyMap(),
        private val synonyms: Map<String, String> = emptyMap(),
        private val relatedSpots: Map<String, List<RelatedSpotType>> = emptyMap(),
        private val guideDetails: Map<String, ItemGuideDetail> = emptyMap(),
        private val guideDetailAliases: Map<String, String> = emptyMap(),
        private val localItems: List<DisposalItemGuide> = emptyList(),
        private val wasteDictionaryItems: List<WasteDictionaryItem> = emptyList(),
    ) : ItemCategoryLocalSource {
        override fun getCategoryMap() = categoryMap

        override fun getSynonyms() = synonyms

        override fun getRelatedSpots() = relatedSpots

        override fun getGuideDetails() = guideDetails

        override fun getGuideDetailAliases() = guideDetailAliases

        override fun getLocalItems() = localItems

        override fun getWasteDictionaryItems() = wasteDictionaryItems
    }
}
