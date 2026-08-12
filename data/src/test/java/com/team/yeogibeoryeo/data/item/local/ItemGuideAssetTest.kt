package com.team.yeogibeoryeo.data.item.local

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ItemGuideAssetTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val assetsDir = File("src/main/assets")

    @Test
    fun `모든 상세 가이드의 sourceCategory가 mapper에서 해석된다`() {
        val guideDetails = parseObject("representative_guide_details.json")
        val unmappedSourceCategories =
            guideDetails
                .entries
                .mapNotNull { (guideKey, value) ->
                    val sourceCategory = value.jsonObject["sourceCategory"]?.jsonPrimitive?.contentOrNull
                    when {
                        sourceCategory.isNullOrBlank() -> "$guideKey: <empty>"
                        DisposalCategory.fromDisplayName(sourceCategory) == null -> sourceCategory
                        else -> null
                    }
                }
                .sorted()

        assertTrue(
            "mapper에서 해석되지 않는 상세 가이드 sourceCategory: $unmappedSourceCategories",
            unmappedSourceCategories.isEmpty(),
        )
    }

    @Test
    fun `synonyms의 대상 검색어는 비어 있지 않다`() {
        val map = parseObject("synonyms.json")
        val emptyTargets =
            map.entries
                .filter { (_, value) -> value.jsonPrimitive.content.isBlank() }
                .map { it.key }

        assertTrue("대상 검색어가 비어 있는 synonyms 항목: $emptyTargets", emptyTargets.isEmpty())
    }

    @Test
    fun `스트로폼 별칭은 스티로폼 품목 검색어를 가리킨다`() {
        val searchTarget =
            parseObject("synonyms.json")["스트로폼"]
                ?.jsonPrimitive
                ?.content
        val searchableTerms =
            parseArray("item_disposal_guides.json")
                .flatMap { element ->
                    val item = element.jsonObject
                    listOf(item["name"]!!.jsonPrimitive.content) +
                            item["similarItems"]!!.jsonArray.map { it.jsonPrimitive.content }
                }.map { term -> term.filterNot { it.isWhitespace() } }

        assertEquals("스티로폼", searchTarget)
        assertTrue(
            "스티로폼으로 검색할 수 있는 품목이 없습니다.",
            searchableTerms.any { term -> term.contains(searchTarget.orEmpty()) },
        )
    }

    @Test
    fun `상세 가이드의 relatedSpotTypes가 유효한 RelatedSpotType을 참조한다`() {
        val map = parseObject("representative_guide_details.json")
        val invalidValues =
            map
                .flatMap { (_, value) ->
                    value.jsonObject["relatedSpotTypes"]
                        ?.jsonArray
                        ?.map { it.jsonPrimitive.content }
                        .orEmpty()
                }.filter { name -> runCatching { RelatedSpotType.valueOf(name) }.isFailure }
                .distinct()
                .sorted()

        assertTrue("상세 가이드에 유효하지 않은 RelatedSpotType 값: $invalidValues", invalidValues.isEmpty())
    }

    @Test
    fun `대표 상세 가이드의 key와 sourceCategory는 같은 공식 분류명이다`() {
        val mismatchedItems =
            parseObject("representative_guide_details.json")
                .entries
                .mapNotNull { (guideKey, value) ->
                    val sourceCategory = value.jsonObject["sourceCategory"]?.jsonPrimitive?.contentOrNull
                    if (guideKey == sourceCategory) null else "$guideKey: $sourceCategory"
                }

        assertTrue("key와 sourceCategory가 다른 상세 가이드: $mismatchedItems", mismatchedItems.isEmpty())
    }

    @Test
    fun `대표 상세 가이드는 공식 분리배출 분류 순서를 따른다`() {
        val expectedGuideKeys =
            listOf(
                "종이",
                "종이팩",
                "무색페트병",
                "플라스틱류",
                "비닐류",
                "발포합성수지",
                "유리병",
                "금속류",
                "의류 및 원단",
                "전지",
                "조명제품",
                "전기전자제품",
                "음식물류폐기물",
                "일반종량제폐기물",
                "불연성종량제폐기물",
                "대형폐기물",
                "공사장 생활폐기물",
                "생활계 유해폐기물",
                "기타",
            )

        val actualGuideKeys = parseObject("representative_guide_details.json").keys.toList()

        assertTrue(
            "대표 상세 가이드 순서가 공식 분류 순서와 다릅니다: $actualGuideKeys",
            actualGuideKeys == expectedGuideKeys,
        )
    }

    @Test
    fun `상세 가이드는 문서 섹션 기반 구조를 가진다`() {
        val map = parseObject("representative_guide_details.json")
        val invalidItems =
            map.entries.mapNotNull { (name, value) ->
                val obj = value.jsonObject
                val sections = obj["sections"]?.jsonArray.orEmpty()
                val hasLegacyFields =
                    obj["steps"] != null ||
                            obj["cautions"] != null ||
                            obj["subGuides"] != null ||
                            obj["tip"] != null
                val hasInvalidSection =
                    sections.isEmpty() ||
                            sections.any { section ->
                                val sectionObject = section.jsonObject
                                val lines = sectionObject["lines"]?.jsonArray.orEmpty()
                                val rows = sectionObject["rows"]?.jsonArray.orEmpty()
                                sectionObject["title"]?.jsonPrimitive?.content.isNullOrBlank() ||
                                        (lines.isEmpty() && rows.isEmpty()) ||
                                        rows.any { row ->
                                            val rowObject = row.jsonObject
                                            rowObject["label"]?.jsonPrimitive?.content.isNullOrBlank() ||
                                                    rowObject["value"]?.jsonPrimitive?.content.isNullOrBlank()
                                        }
                            }

                if (hasLegacyFields || hasInvalidSection) name else null
            }

        assertTrue("sections 구조가 유효하지 않은 상세 가이드: $invalidItems", invalidItems.isEmpty())
    }

    @Test
    fun `품목사전 asset은 검색과 상세 화면에 필요한 필드를 가진다`() {
        val array = parseArray("item_disposal_guides.json")
        val invalidItems =
            array.mapNotNull { element ->
                val obj = element.jsonObject
                val name = obj["name"]?.jsonPrimitive?.content
                val categoryPaths = obj["categoryPaths"]?.jsonArray.orEmpty()
                val dischargeMethods = obj["dischargeMethods"]?.jsonArray.orEmpty()
                val hasRequiredCollections =
                    obj["similarItems"]?.jsonArray != null &&
                            obj["features"]?.jsonArray != null &&
                            obj["notes"]?.jsonArray != null
                if (name.isNullOrBlank() || categoryPaths.isEmpty() || dischargeMethods.isEmpty() || !hasRequiredCollections) {
                    name ?: "<unknown>"
                } else {
                    null
                }
            }

        assertTrue("필수 필드가 누락된 품목사전 항목: $invalidItems", invalidItems.isEmpty())
    }

    @Test
    fun `품목사전 asset의 품목명은 중복되지 않는다`() {
        val duplicatedNames =
            parseArray("item_disposal_guides.json")
                .map { it.jsonObject["name"]!!.jsonPrimitive.content }
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys

        assertTrue("중복된 품목사전 품목명: $duplicatedNames", duplicatedNames.isEmpty())
    }

    @Test
    fun `내용이 다른 대표 상세 가이드와 품목사전 가이드는 ID를 공유하지 않는다`() {
        val representativeIds =
            parseObject("representative_guide_details.json")
                .mapValues { (_, value) -> value.jsonObject["id"]?.jsonPrimitive?.content }
        val dictionaryIds =
            parseArray("item_disposal_guides.json")
                .associate { element ->
                    val item = element.jsonObject
                    item["name"]!!.jsonPrimitive.content to item["id"]?.jsonPrimitive?.content
                }
        val distinctGuidePairs =
            listOf(
                "플라스틱류" to "플라스틱",
                "비닐류" to "비닐",
                "금속류" to "금속",
                "의류 및 원단" to "의류, 원단",
            )
        val invalidPairs =
            distinctGuidePairs.mapNotNull { (representativeName, dictionaryName) ->
                val representativeId = representativeIds[representativeName]
                val dictionaryId = dictionaryIds[dictionaryName]
                if (representativeId != null && dictionaryId != null && representativeId != dictionaryId) {
                    null
                } else {
                    "$representativeName=$representativeId, $dictionaryName=$dictionaryId"
                }
            }

        assertTrue("서로 다른 가이드가 ID를 공유하거나 누락되었습니다: $invalidPairs", invalidPairs.isEmpty())
    }

    @Test
    fun `품목 가이드 ID와 이전 표시명은 모든 asset에서 하나의 품목만 가리킨다`() {
        val identities = parseGuideIdentities()
        val invalidIds =
            identities
                .filterNot { STABLE_ID_PATTERN.matches(it.id) }
                .map { "${it.name}: ${it.id}" }
        val idConflicts =
            identities
                .groupBy { it.id }
                .filterValues { matches -> matches.map { it.name }.distinct().size > 1 }
        val nameConflicts =
            identities
                .flatMap { identity ->
                    (listOf(identity.name) + identity.legacyNames).map { name -> name to identity.id }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, ids) -> ids.distinct() }
                .filterValues { it.size > 1 }

        assertTrue("형식이 잘못된 품목 가이드 ID: $invalidIds", invalidIds.isEmpty())
        assertTrue("여러 표시명에 연결된 품목 가이드 ID: $idConflicts", idConflicts.isEmpty())
        assertTrue("여러 ID에 연결된 현재·이전 표시명: $nameConflicts", nameConflicts.isEmpty())
    }

    @Test
    fun `대표 상세 가이드 ID는 카테고리 순서와 무관하게 고정된다`() {
        val expectedIds =
            mapOf(
                "종이" to "item-guide-0001",
                "종이팩" to "item-guide-0002",
                "무색페트병" to "item-guide-0003",
                "플라스틱류" to "item-guide-0004",
                "비닐류" to "item-guide-0005",
                "발포합성수지" to "item-guide-0006",
                "유리병" to "item-guide-0007",
                "금속류" to "item-guide-0008",
                "의류 및 원단" to "item-guide-0009",
                "전지" to "item-guide-0010",
                "조명제품" to "item-guide-0011",
                "전기전자제품" to "item-guide-0012",
                "음식물류폐기물" to "item-guide-0013",
                "일반종량제폐기물" to "item-guide-0014",
                "불연성종량제폐기물" to "item-guide-0015",
                "대형폐기물" to "item-guide-0016",
                "공사장 생활폐기물" to "item-guide-0017",
                "생활계 유해폐기물" to "item-guide-0018",
                "기타" to "item-guide-0019",
            )
        val actualIds =
            parseObject("representative_guide_details.json")
                .mapValues { (_, value) -> value.jsonObject["id"]?.jsonPrimitive?.content }
        val mismatches =
            (expectedIds.keys + actualIds.keys)
                .mapNotNull { name ->
                    val expectedId = expectedIds[name]
                    val actualId = actualIds[name]
                    if (actualId == expectedId) null else "$name: expected=$expectedId actual=$actualId"
                }

        assertTrue("대표 상세 가이드 ID가 고정 계약과 다릅니다: $mismatches", mismatches.isEmpty())
    }

    private fun parseGuideIdentities(): List<GuideIdentity> {
        val dictionaryIdentities =
            parseArray("item_disposal_guides.json").map { element ->
                val item = element.jsonObject
                GuideIdentity(
                    id = item["id"]!!.jsonPrimitive.content,
                    name = item["name"]!!.jsonPrimitive.content,
                    legacyNames =
                        item["legacyNames"]
                            ?.jsonArray
                            ?.map { it.jsonPrimitive.content }
                            .orEmpty(),
                )
            }
        val representativeIdentities =
            parseObject("representative_guide_details.json").map { (name, element) ->
                val detail = element.jsonObject
                GuideIdentity(
                    id = detail["id"]!!.jsonPrimitive.content,
                    name = name,
                    legacyNames =
                        detail["legacyNames"]
                            ?.jsonArray
                            ?.map { it.jsonPrimitive.content }
                            .orEmpty(),
                )
            }

        return dictionaryIdentities + representativeIdentities
    }

    private fun parseObject(fileName: String) =
        json.parseToJsonElement(File(assetsDir, fileName).readText(Charsets.UTF_8)).jsonObject

    private fun parseArray(fileName: String) =
        json.parseToJsonElement(File(assetsDir, fileName).readText(Charsets.UTF_8)).jsonArray

    private data class GuideIdentity(
        val id: String,
        val name: String,
        val legacyNames: List<String>,
    )

    private companion object {
        val STABLE_ID_PATTERN = Regex("item-guide-\\d{4}")
    }
}
