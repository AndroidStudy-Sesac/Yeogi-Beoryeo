package com.team.yeogibeoryeo.data.item.local

import com.team.yeogibeoryeo.data.item.mapper.toSourceCategoryInfo
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
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
    fun `모든 가이드 별칭의 대상 키가 상세 가이드에 존재한다`() {
        val guideDetails = parseObject("representative_guide_details.json")
        val aliases = parseObject("guide_detail_aliases.json")

        val missingTargets =
            aliases
                .values
                .map { it.jsonPrimitive.content }
                .filterNot { it in guideDetails.keys }
                .distinct()
                .sorted()

        assertTrue(
            "상세 가이드에 없는 별칭 대상: $missingTargets",
            missingTargets.isEmpty(),
        )
    }

    @Test
    fun `category_map이 유효한 DisposalCategory와 DisposalSubCategory를 참조한다`() {
        val map = parseObject("category_map.json")
        val invalidEntries =
            map.entries.mapNotNull { (itemNm, value) ->
                val obj = value.jsonObject
                val categoryName = obj["category"]!!.jsonPrimitive.content
                val subCategoryName = obj["subCategory"]?.jsonPrimitive?.contentOrNull
                val errors = mutableListOf<String>()
                runCatching { DisposalCategory.valueOf(categoryName) }
                    .onFailure { errors += "category=$categoryName" }
                if (subCategoryName != null) {
                    runCatching { DisposalSubCategory.valueOf(subCategoryName) }
                        .onFailure { errors += "subCategory=$subCategoryName" }
                }
                if (errors.isEmpty()) null else "$itemNm: ${errors.joinToString()}"
            }

        assertTrue("유효하지 않은 category_map 항목: $invalidEntries", invalidEntries.isEmpty())
    }

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
                        sourceCategory.toSourceCategoryInfo() == null -> sourceCategory
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
    fun `related_spots가 유효한 RelatedSpotType을 참조한다`() {
        val map = parseObject("related_spots.json")
        val invalidValues =
            map
                .flatMap { (_, array) ->
                    array.jsonArray.map { it.jsonPrimitive.content }
                }.filter { name -> runCatching { RelatedSpotType.valueOf(name) }.isFailure }
                .distinct()
                .sorted()

        assertTrue("유효하지 않은 RelatedSpotType 값: $invalidValues", invalidValues.isEmpty())
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
