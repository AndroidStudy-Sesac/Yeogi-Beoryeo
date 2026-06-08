package com.team.yeogibeoryeo.data.item.local

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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

    private fun parseObject(fileName: String) =
        json.parseToJsonElement(File(assetsDir, fileName).readText()).jsonObject

    private fun parseArray(fileName: String) =
        json.parseToJsonElement(File(assetsDir, fileName).readText()).jsonArray
}
