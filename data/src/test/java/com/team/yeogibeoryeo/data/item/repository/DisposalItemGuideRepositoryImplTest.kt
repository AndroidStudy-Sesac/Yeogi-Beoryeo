package com.team.yeogibeoryeo.data.item.repository

import com.team.yeogibeoryeo.data.item.local.ItemCategoryLocalSource
import com.team.yeogibeoryeo.data.item.local.ItemGuideDetail
import com.team.yeogibeoryeo.data.item.local.WasteDictionaryItem
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
    fun `searchItemGuides는 동의어를 local 품목사전 검색어로 사용한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            synonyms = mapOf("캔" to "음료캔"),
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "음료캔",
                                        categoryPaths = listOf(listOf("재활용폐기물", "금속류 금속캔")),
                                        dischargeMethods = listOf("음료캔은 캔류로 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val results = repository.searchItemGuides("캔")

            assertEquals(listOf("음료캔"), results.map { it.name })
            assertEquals(DisposalCategory.METAL, results.first().category)
        }

    @Test
    fun `searchItemGuides는 빈 검색어면 빈 목록을 반환한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "종이",
                                        categoryPaths = listOf(listOf("재활용폐기물", "종이류")),
                                        dischargeMethods = listOf("종이는 종이류로 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val results = repository.searchItemGuides("   ")

            assertTrue(results.isEmpty())
        }

    @Test
    fun `searchItemGuides는 local 품목사전 결과를 반환한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
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
                )

            val results = repository.searchItemGuides("내열냄비")

            assertEquals(1, results.size)
            assertEquals(
                "내열냄비는 불연성 종량제봉투(마대)로 배출합니다.",
                results.first().instructions.first().method,
            )
        }

    @Test
    fun `searchItemGuides는 결과가 없으면 fallback 없이 빈 목록을 반환한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "유리병",
                                        categoryPaths = listOf(listOf("재활용폐기물", "유리병")),
                                        dischargeMethods = listOf("유리병은 유리병으로 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val results = repository.searchItemGuides("없는품목")

            assertTrue(results.isEmpty())
        }

    @Test
    fun `searchItemGuides는 품목사전 유사 품목도 검색한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
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
                )

            val results = repository.searchItemGuides("박스")

            assertEquals(listOf("아이스박스"), results.map { it.name })
        }

    @Test
    fun `searchItemGuides는 직접 이름 결과가 있으면 유사 품목 결과보다 우선한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
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
                )

            val results = repository.searchItemGuides("우유")

            assertEquals(listOf("우유팩"), results.map { it.name })
        }

    @Test
    fun `searchItemGuides는 이름 정확 일치를 이름 부분 일치보다 우선한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
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
                )

            val results = repository.searchItemGuides("유리")

            assertEquals(listOf("유리"), results.map { it.name })
        }

    @Test
    fun `searchItemGuides는 이름 시작 일치를 이름 중간 일치보다 우선한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
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
                )

            val results = repository.searchItemGuides("아")

            assertEquals(listOf("아이스팩", "항아리"), results.map { it.name })
        }

    @Test
    fun `searchItemGuides는 유사 품목 정확 일치를 유사 품목 부분 일치보다 우선한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
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
                )

            val results = repository.searchItemGuides("우유")

            assertEquals(listOf("종이팩"), results.map { it.name })
        }

    @Test
    fun `getCategoryGuides는 sourceCategory 기반 초기 가이드를 반환한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
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
                )

            val results = repository.getCategoryGuides(DisposalCategory.PAPER)

            assertEquals(1, results.size)
            assertEquals("종이", results.first().name)
            assertNull(results.first().subCategory)
        }

    @Test
    fun `getCategoryGuides는 sourceCategory가 없으면 기타 카테고리로 분류한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            guideDetails =
                                mapOf(
                                    "분류없음" to
                                            ItemGuideDetail(
                                                steps = emptyList(),
                                                cautions = emptyList(),
                                                tip = null,
                                                relatedSpotTypes = emptyList(),
                                            ),
                                ),
                        ),
                )

            val results = repository.getCategoryGuides(DisposalCategory.OTHER)

            assertEquals(listOf("분류없음"), results.map { it.name })
        }

    @Test
    fun `getCategoryGuides는 재활용 카테고리를 재활용 가능으로 표시한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
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
                )

            val results = repository.getCategoryGuides(DisposalCategory.PAPER)

            assertTrue(results.first().isRecyclable)
        }

    @Test
    fun `getCategoryGuides는 비재활용 카테고리를 재활용 불가로 표시한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
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
                )

            val results = repository.getCategoryGuides(DisposalCategory.LARGE_WASTE)

            assertFalse(results.first().isRecyclable)
        }

    @Test
    fun `getCategoryGuides는 대표 가이드 상세 정보를 포함한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            guideDetails =
                                mapOf(
                                    "종이" to
                                            ItemGuideDetail(
                                                steps = listOf("물기 제거"),
                                                cautions = listOf("기름 묻은 종이 제외"),
                                                sections = listOf(
                                                    DisposalGuideSection(
                                                        "배출방법",
                                                        listOf("물기 제거"),
                                                    ),
                                                ),
                                                tip = "상자는 펼쳐서",
                                                relatedSpotTypes = listOf(RelatedSpotType.RECYCLING_BIN),
                                                sourceCategory = "종이류",
                                            ),
                                ),
                        ),
                )

            val results = repository.getCategoryGuides(DisposalCategory.PAPER)

            assertEquals(listOf("물기 제거"), results.first().steps)
            assertEquals(listOf("기름 묻은 종이 제외"), results.first().cautions)
            assertEquals(
                listOf(DisposalGuideSection("배출방법", listOf("물기 제거"))),
                results.first().detailSections,
            )
            assertEquals("상자는 펼쳐서", results.first().tip)
            assertEquals(listOf(RelatedSpotType.RECYCLING_BIN), results.first().relatedSpotTypes)
        }

    @Test
    fun `getCategoryGuides는 관련 장소 정보가 없으면 relatedSpotTypes를 null로 둔다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
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
                )

            val results = repository.getCategoryGuides(DisposalCategory.OTHER)

            assertNull(results.first().relatedSpotTypes)
        }

    @Test
    fun `getCategories는 모든 도메인 카테고리를 반환한다`() {
        val repository =
            DisposalItemGuideRepositoryImpl(
                localDataSource = FakeLocalSource(),
            )

        assertEquals(DisposalCategory.entries.toList(), repository.getCategories())
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

    private class FakeLocalSource(
        private val synonyms: Map<String, String> = emptyMap(),
        private val guideDetails: Map<String, ItemGuideDetail> = emptyMap(),
        private val wasteDictionaryItems: List<WasteDictionaryItem> = emptyList(),
    ) : ItemCategoryLocalSource {
        override fun getSynonyms() = synonyms

        override fun getGuideDetails() = guideDetails

        override fun getWasteDictionaryItems() = wasteDictionaryItems
    }
}
