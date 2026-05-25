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
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSection
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
                    remoteDataSource = recordingRemote(
                        capturedQueries,
                        results = listOf("음료캔" to "재활용폐기물")
                    ),
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
                    remoteDataSource = recordingRemote(
                        capturedQueries,
                        results = listOf("종이" to "재활용폐기물")
                    ),
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
                    remoteDataSource = recordingRemote(
                        capturedQueries,
                        results = listOf("핸드폰" to "재활용폐기물")
                    ),
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
                    remoteDataSource = recordingRemote(
                        capturedQueries,
                        results = listOf("종이" to "재활용폐기물")
                    ),
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
                    remoteDataSource = recordingRemote(
                        capturedQueries,
                        results = listOf("내열냄비" to "일반쓰레기")
                    ),
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
    fun `searchItemGuides는 직접 이름 결과가 있으면 유사 품목 결과보다 우선한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "우유팩",
                                        similarItems = listOf("종이팩"),
                                        categoryPaths = listOf(listOf("재활용폐기물", "종이팩")),
                                        dischargeMethods = listOf("우유팩은 종이팩으로 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        name = "스틱봉지",
                                        similarItems = listOf("삼각커피우유"),
                                        categoryPaths = listOf(listOf("재활용폐기물", "비닐류")),
                                        dischargeMethods = listOf("스틱봉지는 비닐류로 배출합니다."),
                                    ),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.searchItemGuides("우유")

            assertEquals(listOf("우유팩"), results.map { it.name })
        }

    @Test
    fun `searchItemGuides는 이름 정확 일치를 이름 부분 일치보다 우선한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "유리",
                                        categoryPaths = listOf(listOf("재활용폐기물", "유리병")),
                                        dischargeMethods = listOf("유리는 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        name = "유리병",
                                        categoryPaths = listOf(listOf("재활용폐기물", "유리병")),
                                        dischargeMethods = listOf("유리병은 배출합니다."),
                                    ),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.searchItemGuides("유리")

            assertEquals(listOf("유리"), results.map { it.name })
        }

    @Test
    fun `searchItemGuides는 이름 시작 일치를 이름 중간 일치보다 우선한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "항아리",
                                        categoryPaths = listOf(listOf("일반폐기물", "불연성종량제폐기물")),
                                        dischargeMethods = listOf("항아리는 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        name = "아이스팩",
                                        categoryPaths = listOf(listOf("일반폐기물", "일반종량제폐기물")),
                                        dischargeMethods = listOf("아이스팩은 배출합니다."),
                                    ),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.searchItemGuides("아")

            assertEquals(listOf("아이스팩", "항아리"), results.map { it.name })
        }

    @Test
    fun `searchItemGuides는 유사 품목 정확 일치를 유사 품목 부분 일치보다 우선한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "종이팩",
                                        similarItems = listOf("우유"),
                                        categoryPaths = listOf(listOf("재활용폐기물", "종이팩")),
                                        dischargeMethods = listOf("종이팩은 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        name = "스틱봉지",
                                        similarItems = listOf("삼각커피우유"),
                                        categoryPaths = listOf(listOf("재활용폐기물", "비닐류")),
                                        dischargeMethods = listOf("스틱봉지는 배출합니다."),
                                    ),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.searchItemGuides("우유")

            assertEquals(listOf("종이팩"), results.map { it.name })
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
                                    "종이류" to (DisposalCategory.PAPER to null),
                                ),
                            guideDetails =
                                mapOf(
                                    "종이" to
                                            ItemGuideDetail(
                                                steps = emptyList(),
                                                cautions = emptyList(),
                                                tip = null,
                                                relatedSpotTypes = emptyList(),
                                                sourceCategory = "종이류",
                                            ),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            repository.getCategoryGuides(DisposalCategory.PAPER)

            assertTrue("원격 API가 호출되면 안 됩니다", capturedQueries.isEmpty())
        }

    @Test
    fun `getCategoryGuides는 sourceCategory 기반 초기 가이드를 반환한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            categoryMap =
                                mapOf(
                                    "종이류" to (DisposalCategory.PAPER to null),
                                    "유리병" to (DisposalCategory.GLASS to DisposalSubCategory.GLASS_BOTTLE),
                                ),
                            guideDetails =
                                mapOf(
                                    "종이" to
                                            ItemGuideDetail(
                                                steps = emptyList(),
                                                cautions = emptyList(),
                                                tip = null,
                                                relatedSpotTypes = emptyList(),
                                                sourceCategory = "종이류",
                                            ),
                                    "유리병" to
                                            ItemGuideDetail(
                                                steps = emptyList(),
                                                cautions = emptyList(),
                                                tip = null,
                                                relatedSpotTypes = emptyList(),
                                                sourceCategory = "유리병",
                                            ),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.getCategoryGuides(DisposalCategory.PAPER)

            assertEquals(1, results.size)
            assertEquals("종이", results.first().name)
            assertNull(results.first().subCategory)
        }

    @Test
    fun `getCategoryGuides는 재활용 카테고리를 재활용 가능으로 표시한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            categoryMap = mapOf("종이류" to (DisposalCategory.PAPER to null)),
                            guideDetails =
                                mapOf(
                                    "종이" to
                                            ItemGuideDetail(
                                                steps = emptyList(),
                                                cautions = emptyList(),
                                                tip = null,
                                                relatedSpotTypes = emptyList(),
                                                sourceCategory = "종이류",
                                            ),
                                ),
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
                            categoryMap = mapOf("대형폐기물" to (DisposalCategory.LARGE_WASTE to null)),
                            guideDetails =
                                mapOf(
                                    "대형폐기물" to
                                            ItemGuideDetail(
                                                steps = emptyList(),
                                                cautions = emptyList(),
                                                tip = null,
                                                relatedSpotTypes = emptyList(),
                                                sourceCategory = "대형폐기물",
                                            ),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.getCategoryGuides(DisposalCategory.LARGE_WASTE)

            assertFalse(results.first().isRecyclable)
        }

    @Test
    fun `getCategoryGuides는 대표 가이드 상세 정보를 포함한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            categoryMap = mapOf("종이류" to (DisposalCategory.PAPER to null)),
                            guideDetails =
                                mapOf(
                                    "종이" to
                                            ItemGuideDetail(
                                                steps = listOf("물기 제거"),
                                                cautions = listOf("기름 묻은 종이 제외"),
                                                sections = listOf(
                                                    DisposalGuideSection(
                                                        "배출방법",
                                                        listOf("물기 제거")
                                                    )
                                                ),
                                                tip = "상자는 펼쳐서",
                                                relatedSpotTypes = listOf(RelatedSpotType.RECYCLING_BIN),
                                                sourceCategory = "종이류",
                                            ),
                                ),
                        ),
                    publicDataKeyProvider = fakePublicDataKeyProvider,
                )

            val results = repository.getCategoryGuides(DisposalCategory.PAPER)

            assertEquals(listOf("물기 제거"), results.first().steps)
            assertEquals(listOf("기름 묻은 종이 제외"), results.first().cautions)
            assertEquals(
                listOf(DisposalGuideSection("배출방법", listOf("물기 제거"))),
                results.first().detailSections
            )
            assertEquals("상자는 펼쳐서", results.first().tip)
            assertEquals(listOf(RelatedSpotType.RECYCLING_BIN), results.first().relatedSpotTypes)
        }

    @Test
    fun `getCategoryGuides는 상세 가이드가 없으면 관련 장소 맵을 사용한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    remoteDataSource = fakeRemote(results = emptyList()),
                    localDataSource =
                        FakeLocalSource(
                            categoryMap = mapOf("전기전자제품" to (DisposalCategory.ELECTRONICS to null)),
                            relatedSpots = mapOf(
                                "전기전자제품" to listOf(
                                    RelatedSpotType.FREE_PICKUP,
                                    RelatedSpotType.E_WASTE_BIN
                                )
                            ),
                            guideDetails =
                                mapOf(
                                    "전기전자제품" to
                                            ItemGuideDetail(
                                                steps = emptyList(),
                                                cautions = emptyList(),
                                                tip = null,
                                                relatedSpotTypes = emptyList(),
                                                sourceCategory = "전기전자제품",
                                            ),
                                ),
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
                            categoryMap = mapOf("기타" to (DisposalCategory.OTHER to null)),
                            guideDetails =
                                mapOf(
                                    "기타" to
                                            ItemGuideDetail(
                                                steps = emptyList(),
                                                cautions = emptyList(),
                                                tip = null,
                                                relatedSpotTypes = emptyList(),
                                                sourceCategory = "기타",
                                            ),
                                ),
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
        val items =
            results.map { (name, method) -> ItemGuideDto(itemNm = name, dschgMthd = method) }
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
        private val wasteDictionaryItems: List<WasteDictionaryItem> = emptyList(),
    ) : ItemCategoryLocalSource {
        override fun getCategoryMap() = categoryMap

        override fun getSynonyms() = synonyms

        override fun getRelatedSpots() = relatedSpots

        override fun getGuideDetails() = guideDetails

        override fun getGuideDetailAliases() = guideDetailAliases

        override fun getWasteDictionaryItems() = wasteDictionaryItems
    }
}
