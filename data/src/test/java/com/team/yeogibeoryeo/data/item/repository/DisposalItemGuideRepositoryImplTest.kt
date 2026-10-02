package com.team.yeogibeoryeo.data.item.repository

import com.team.yeogibeoryeo.data.item.local.ItemCategoryLocalSource
import com.team.yeogibeoryeo.data.item.local.ItemCategoryLocalDataSource
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import io.mockk.mockk
import java.io.File
import com.team.yeogibeoryeo.data.item.local.ItemGuideDetail
import com.team.yeogibeoryeo.data.item.local.WasteDictionaryItem
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSection
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisposalItemGuideRepositoryImplTest {
    @Test
    fun `실제 품목의 오타 후보는 선택하면 검색 결과를 반환한다`() = runBlocking {
        val repository = DisposalItemGuideRepositoryImpl(
            ItemCategoryLocalDataSource(
                readAssetText = { File("src/main/assets", it).readText(Charsets.UTF_8) },
                reporter = mockk<NonFatalErrorReporter>(relaxed = true),
            ),
        )
        val examples = mapOf(
            "유리뱡" to "유리병",
            "전기밥숱" to "전기밥솥",
            "프라이펜" to "프라이팬",
            "플라스틳" to "플라스틱",
            "무색페트뱡" to "무색페트병",
        )
        examples.forEach { (typo, expected) ->
            assertTrue(repository.searchItemGuides(typo).isEmpty())
            val suggestions = repository.suggestSearchQueries(typo)
            assertTrue("$typo: $suggestions", expected in suggestions)
            assertTrue(suggestions.size <= 3)
            suggestions.forEach { assertTrue(repository.searchItemGuides(it).isNotEmpty()) }
        }
        assertTrue(repository.searchItemGuides("스트로폼").isNotEmpty())
        assertTrue(repository.suggestSearchQueries("스트로폼").isEmpty())
        listOf("", "유", "유뱡", "ㄱㄴㄷ", "맛집예약", "오늘날씨알려주세요").forEach {
            assertTrue("$it should not suggest", repository.suggestSearchQueries(it).isEmpty())
        }
    }

    @Test
    fun `후보는 한 글자 차이만 허용하고 중복 없이 최대 세 개를 정렬한다`() = runBlocking {
        val names = listOf("가나다마", "가나다라", "가나 다라", "가나마바", "가나다바", "가나마사", "가나다사")
        val repository = DisposalItemGuideRepositoryImpl(
            FakeLocalSource(
                wasteDictionaryItems = names.map {
                    sampleDictionaryItem(it, listOf(listOf("일반폐기물")), emptyList())
                },
            ),
        )
        assertEquals(listOf("가나다라", "가나다마", "가나다바"), repository.suggestSearchQueries("가나다카"))
        assertTrue(repository.suggestSearchQueries("없는검색어").isEmpty())
    }

    @Test
    fun `공백 괄호와 대소문자를 정규화하고 검색 가능한 별칭만 제안한다`() = runBlocking {
        val repository = DisposalItemGuideRepositoryImpl(
            FakeLocalSource(
                synonyms = mapOf("스마트폰" to "핸드폰", "미등록품" to "없는품목"),
                wasteDictionaryItems = listOf(
                    sampleDictionaryItem("핸드폰", emptyList(), emptyList(), searchTerms = listOf("MOBILE")),
                ),
            ),
        )
        assertEquals(listOf("스마트폰"), repository.suggestSearchQueries("스마 트（퐁）"))
        assertEquals(listOf("MOBILE"), repository.suggestSearchQueries("mobilx"))
        assertTrue(repository.suggestSearchQueries("미등록푼").isEmpty())
        assertTrue(repository.suggestSearchQueries("스마트폰").isEmpty())
        assertTrue(repository.suggestSearchQueries("핸드폰").isEmpty())
    }

    @Test
    fun `searchItemGuides는 local 조회를 지정된 dispatcher에서 실행한다`() =
        runBlocking {
            val callingThread = Thread.currentThread()
            var sourceThread: Thread? = null

            Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
                val repository =
                    DisposalItemGuideRepositoryImpl(
                        localDataSource =
                            FakeLocalSource(
                                wasteDictionaryItems =
                                    listOf(
                                        sampleDictionaryItem(
                                            name = "종이",
                                            categoryPaths = listOf(listOf("재활용폐기물", "종이류")),
                                            dischargeMethods = listOf("종이류로 배출합니다."),
                                        ),
                                    ),
                                onGetWasteDictionaryItems = { sourceThread = Thread.currentThread() },
                            ),
                        ioDispatcher = dispatcher,
                    )

                repository.searchItemGuides("종이")
            }

            assertNotNull(sourceThread)
            assertNotSame(callingThread, sourceThread)
        }

    @Test
    fun `searchItemGuides는 원문 검색 결과가 없을 때 동의어로 다시 검색한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            synonyms = mapOf("스마트폰" to "핸드폰"),
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "핸드폰",
                                        categoryPaths = listOf(listOf("재활용폐기물", "전기전자 제품류")),
                                        dischargeMethods = listOf("핸드폰은 폐가전 수거 기준에 따라 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val results = repository.searchItemGuides("스마트폰")

            assertEquals(listOf("핸드폰"), results.map { it.name })
            assertEquals(DisposalCategory.ELECTRONICS, results.first().category)
        }

    @Test
    fun `searchItemGuides는 원문 검색 결과가 있으면 동의어보다 우선한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            synonyms = mapOf("캔" to "음료캔"),
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "캔",
                                        categoryPaths = listOf(listOf("재활용폐기물", "금속류 금속캔")),
                                        dischargeMethods = listOf("캔은 캔류로 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        name = "음료캔",
                                        categoryPaths = listOf(listOf("재활용폐기물", "금속류 금속캔")),
                                        dischargeMethods = listOf("음료캔은 캔류로 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val results = repository.searchItemGuides("캔")

            assertEquals(listOf("캔"), results.map { it.name })
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

            val results = repository.searchItemGuides("택배")

            assertEquals(listOf("아이스박스"), results.map { it.name })
        }

    @Test
    fun `유사 품목명 내부 공백 차이를 무시하고 검색한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "소형 폐가전",
                                        similarItems = listOf("핸드폰 충전기"),
                                        categoryPaths = listOf(listOf("재활용폐기물", "전기전자제품")),
                                        dischargeMethods = listOf("폐가전 수거 기준에 따라 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val results = repository.searchItemGuides("핸드폰충전기")

            assertEquals(listOf("소형 폐가전"), results.map { it.name })
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
    fun `품목명 내부 공백 차이를 무시하고 검색한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "태블릿 PC",
                                        categoryPaths = listOf(listOf("재활용폐기물", "전기전자제품")),
                                        dischargeMethods = listOf("폐가전 수거 기준에 따라 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        name = "핸드폰 충전기",
                                        categoryPaths = listOf(listOf("재활용폐기물", "전기전자제품")),
                                        dischargeMethods = listOf("폐가전 수거 기준에 따라 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val tabletResults = repository.searchItemGuides("태블릿PC")
            val chargerResults = repository.searchItemGuides("핸드폰충전기")

            assertEquals(listOf("태블릿 PC"), tabletResults.map { it.name })
            assertEquals(listOf("핸드폰 충전기"), chargerResults.map { it.name })
        }

    @Test
    fun `품목명 반각과 전각 괄호 차이를 무시하고 정확 일치를 우선한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "냄비뚜껑(내열)",
                                        categoryPaths = listOf(listOf("일반폐기물", "불연성종량제폐기물")),
                                        dischargeMethods = listOf("불연성 종량제봉투로 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        name = "냄비뚜껑내열판",
                                        categoryPaths = listOf(listOf("일반폐기물", "불연성종량제폐기물")),
                                        dischargeMethods = listOf("불연성 종량제봉투로 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        name = "택배 박스（스티로폼）",
                                        categoryPaths = listOf(listOf("일반폐기물", "일반종량제폐기물")),
                                        dischargeMethods = listOf("종량제봉투로 배출합니다."),
                                    ),
                                ),
                        ),
                )

            listOf(
                "냄비뚜껑 내열" to "냄비뚜껑(내열)",
                "냄비뚜껑(내열)" to "냄비뚜껑(내열)",
                "냄비뚜껑（내열）" to "냄비뚜껑(내열)",
                "택배 박스 스티로폼" to "택배 박스（스티로폼）",
            ).forEach { (query, expectedName) ->
                val results = repository.searchItemGuides(query)

                assertEquals("검색어: $query", listOf(expectedName), results.map { it.name })
            }
        }

    @Test
    fun `공백 제거 후 정확 일치를 부분 일치보다 우선한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "AAA건전지",
                                        categoryPaths = listOf(listOf("재활용폐기물", "전지류")),
                                        dischargeMethods = listOf("전용 수거함에 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        name = "AA 건전지",
                                        categoryPaths = listOf(listOf("재활용폐기물", "전지류")),
                                        dischargeMethods = listOf("전용 수거함에 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val results = repository.searchItemGuides("AA건전지")

            assertEquals(listOf("AA 건전지"), results.map { it.name })
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
    fun `searchItemGuides는 같은 매칭 등급이면 품목명 가나다순으로 반환한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        name = "전기히터",
                                        categoryPaths = listOf(listOf("재활용폐기물", "전기전자제품")),
                                        dischargeMethods = listOf("전기히터는 폐가전 수거 기준에 따라 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        name = "전기다리미",
                                        categoryPaths = listOf(listOf("재활용폐기물", "전기전자제품")),
                                        dischargeMethods = listOf("전기다리미는 폐가전 수거 기준에 따라 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val results = repository.searchItemGuides("전기")

            assertEquals(listOf("전기다리미", "전기히터"), results.map { it.name })
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
    fun `searchItemGuides는 전용 검색어 대상을 같은 등급의 품목명 결과보다 먼저 반환한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        id = "item-guide-0736",
                                        name = "화장품 내용물과 복합 용기",
                                        searchTerms = listOf("화장품"),
                                        categoryPaths = listOf(listOf("일반폐기물")),
                                        dischargeMethods = listOf("종량제봉투로 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        id = "item-guide-0739",
                                        name = "화장품 금속 용기",
                                        categoryPaths = listOf(listOf("재활용폐기물", "금속류")),
                                        dischargeMethods = listOf("금속류로 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val results = repository.searchItemGuides("화장품")

            assertEquals(
                listOf("item-guide-0736", "item-guide-0739"),
                results.map { it.id },
            )
        }

    @Test
    fun `searchItemGuides는 전용 검색어의 공백 차이를 무시하고 다른 품목과 섞지 않는다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        id = "item-guide-0736",
                                        name = "화장품 내용물과 복합 용기",
                                        searchTerms = listOf("쿠션 팩트"),
                                        categoryPaths = listOf(listOf("일반폐기물")),
                                        dischargeMethods = listOf("종량제봉투로 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        id = "item-guide-cushion",
                                        name = "쿠션",
                                        categoryPaths = listOf(listOf("일반폐기물")),
                                        dischargeMethods = listOf("종량제봉투로 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        id = "item-guide-0230",
                                        name = "의류, 원단",
                                        searchTerms = listOf("양말"),
                                        categoryPaths = listOf(listOf("재활용폐기물", "의류 및 원단")),
                                        dischargeMethods = listOf("의류수거함으로 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        id = "item-guide-0740",
                                        name = "티슈 종이 상자",
                                        searchTerms = listOf("갑 티슈", "각티슈"),
                                        categoryPaths = listOf(listOf("재활용폐기물", "종이")),
                                        dischargeMethods = listOf("종이류로 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val expectedIdsByQuery =
                mapOf(
                    "쿠션 팩트" to "item-guide-0736",
                    "쿠션팩트" to "item-guide-0736",
                    "양말" to "item-guide-0230",
                    "갑 티슈" to "item-guide-0740",
                    "갑티슈" to "item-guide-0740",
                    "각티슈" to "item-guide-0740",
                )
            expectedIdsByQuery.forEach { (query, expectedId) ->
                val results = repository.searchItemGuides(query)

                assertEquals("검색어: $query", listOf(expectedId), results.map { it.id })
            }
        }

    @Test
    fun `컵라면 용기 종이는 기존 이름과 공식 품목명으로 검색하면 대상 품목 하나만 반환한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        id = "item-guide-0351",
                                        name = "컵라면 용기(종이)",
                                        similarItems = listOf("종이 컵라면", "종이컵라면 용기"),
                                        categoryPaths = listOf(listOf("재활용폐기물", "종이류")),
                                        dischargeMethods = listOf("종이류로 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        name = "컵라면 뚜껑",
                                        similarItems = listOf("종이컵라면 용기 뚜껑"),
                                        categoryPaths = listOf(listOf("일반폐기물", "일반종량제폐기물")),
                                        dischargeMethods = listOf("종량제봉투로 배출합니다."),
                                    ),
                                ),
                        ),
                )

            listOf("종이 컵라면", "종이컵라면 용기").forEach { query ->
                val results = repository.searchItemGuides(query)

                assertEquals("검색어: $query", listOf("item-guide-0351"), results.map { it.id })
            }
        }

    @Test
    fun `searchItemGuides는 표시명이 같아도 안정 ID가 다르면 모두 유지한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        id = "item-guide-0100",
                                        name = "동일 표시명",
                                        categoryPaths = listOf(listOf("재활용폐기물", "종이")),
                                        dischargeMethods = listOf("종이류로 배출합니다."),
                                    ),
                                    sampleDictionaryItem(
                                        id = "item-guide-0101",
                                        name = "동일 표시명",
                                        categoryPaths = listOf(listOf("재활용폐기물", "종이")),
                                        dischargeMethods = listOf("종이류로 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val results = repository.searchItemGuides("동일 표시명")

            assertEquals(2, results.size)
            assertEquals(
                setOf("item-guide-0100", "item-guide-0101"),
                results.map { it.id }.toSet(),
            )
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
                                                id = "item-guide-test",
                                                steps = emptyList(),
                                                cautions = emptyList(),
                                                tip = null,
                                                relatedSpotTypes = emptyList(),
                                                sourceCategory = "종이",
                                            ),
                                    "유리병" to
                                            ItemGuideDetail(
                                                id = "item-guide-test",
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
            assertEquals("item-guide-test", results.first().id)
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
                                                id = "item-guide-test",
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
                                                id = "item-guide-test",
                                                steps = emptyList(),
                                                cautions = emptyList(),
                                                tip = null,
                                                relatedSpotTypes = emptyList(),
                                                sourceCategory = "종이",
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
                                                id = "item-guide-test",
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
                                                id = "item-guide-test",
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
                                                sourceCategory = "종이",
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
                                                id = "item-guide-test",
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
    fun `getItemGuide는 대표 가이드를 안정 ID와 이전 표시명으로 정확히 찾는다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            guideDetails =
                                mapOf(
                                    "플라스틱류" to
                                        ItemGuideDetail(
                                            id = "item-guide-0004",
                                            legacyNames = listOf("합성수지류"),
                                            steps = listOf("내용물을 비웁니다."),
                                            cautions = emptyList(),
                                            tip = null,
                                            relatedSpotTypes = emptyList(),
                                            sourceCategory = "플라스틱류",
                                        ),
                                ),
                        ),
                )

            val stableIdResult = repository.getItemGuide("item-guide-0004")
            val currentNameResult = repository.getItemGuide("플라스틱류")
            val legacyNameResult = repository.getItemGuide("합성수지류")

            assertEquals("item-guide-0004", stableIdResult?.id)
            assertEquals("플라스틱류", stableIdResult?.name)
            assertEquals(stableIdResult, currentNameResult)
            assertEquals(stableIdResult, legacyNameResult)
            assertEquals(DisposalCategory.PLASTIC, stableIdResult?.category)
            assertNull(stableIdResult?.subCategory)
            assertEquals(listOf("내용물을 비웁니다."), stableIdResult?.steps)
        }

    @Test
    fun `getItemGuide는 품목 사전을 안정 ID와 이전 표시명으로 정확히 찾는다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        id = "item-guide-0100",
                                        name = "유리병",
                                        legacyNames = listOf("유리용기"),
                                        categoryPaths = listOf(listOf("재활용폐기물", "유리병")),
                                        dischargeMethods = listOf("유리병 수거함으로 배출합니다."),
                                    ),
                                ),
                        ),
                )

            val stableIdResult = repository.getItemGuide("item-guide-0100")
            val legacyNameResult = repository.getItemGuide("유리용기")

            assertEquals("item-guide-0100", stableIdResult?.id)
            assertEquals("유리병", stableIdResult?.name)
            assertEquals(stableIdResult, legacyNameResult)
            assertEquals(DisposalCategory.GLASS, stableIdResult?.category)
        }

    @Test
    fun `getItemGuide는 같은 분류의 대표 가이드와 품목 사전을 각각 조회한다`() =
        runBlocking {
            val repository =
                DisposalItemGuideRepositoryImpl(
                    localDataSource =
                        FakeLocalSource(
                            guideDetails =
                                mapOf(
                                    "플라스틱류" to
                                        ItemGuideDetail(
                                            id = "item-guide-0004",
                                            steps = listOf("대표 가이드 내용"),
                                            cautions = emptyList(),
                                            tip = null,
                                            relatedSpotTypes = emptyList(),
                                            sourceCategory = "플라스틱류",
                                        ),
                                ),
                            wasteDictionaryItems =
                                listOf(
                                    sampleDictionaryItem(
                                        id = "item-guide-0233",
                                        name = "플라스틱",
                                        categoryPaths = listOf(listOf("재활용폐기물", "합성수지 재질")),
                                        dischargeMethods = listOf("품목 사전 내용"),
                                    ),
                                ),
                        ),
                )

            val representativeGuide = repository.getItemGuide("item-guide-0004")
            val dictionaryGuide = repository.getItemGuide("item-guide-0233")

            assertEquals("플라스틱류", representativeGuide?.name)
            assertEquals(listOf("대표 가이드 내용"), representativeGuide?.steps)
            assertEquals("플라스틱", dictionaryGuide?.name)
            assertEquals("품목 사전 내용", dictionaryGuide?.instructions?.single()?.method)
        }

    @Test
    fun `getItemGuide는 정확히 일치하는 ID가 없으면 null을 반환한다`() =
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
                                        dischargeMethods = listOf("유리병 수거함으로 배출합니다."),
                                    ),
                                ),
                        ),
                )

            assertNull(repository.getItemGuide("유리"))
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
        id: String = "item-guide-$name",
        legacyNames: List<String> = emptyList(),
        searchTerms: List<String> = emptyList(),
    ): WasteDictionaryItem =
        WasteDictionaryItem(
            id = id,
            name = name,
            legacyNames = legacyNames,
            searchTerms = searchTerms,
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
        private val onGetWasteDictionaryItems: () -> Unit = {},
    ) : ItemCategoryLocalSource {
        override fun getSynonyms() = synonyms

        override fun getGuideDetails() = guideDetails

        override fun getWasteDictionaryItems(): List<WasteDictionaryItem> {
            onGetWasteDictionaryItems()
            return wasteDictionaryItems
        }
    }
}
