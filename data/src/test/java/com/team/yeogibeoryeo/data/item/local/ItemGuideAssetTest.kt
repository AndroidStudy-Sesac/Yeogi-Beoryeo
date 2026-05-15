package com.team.yeogibeoryeo.data.item.local

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
        val guideDetails = parseObject("item_guide_details.json")
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
    fun `모든 상세 가이드 키가 category_map에 존재한다`() {
        val guideDetails = parseObject("item_guide_details.json")
        val categoryMap = parseObject("category_map.json")
        val missingCategoryKeys =
            guideDetails
                .keys
                .filterNot { it in categoryMap.keys }
                .sorted()

        assertTrue(
            "category_map에 없는 상세 가이드 키: $missingCategoryKeys",
            missingCategoryKeys.isEmpty(),
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
    fun `local_items가 유효한 카테고리와 하위 카테고리를 참조한다`() {
        val array =
            json
                .parseToJsonElement(File(assetsDir, "local_items.json").readText())
                .jsonArray
        val invalidEntries =
            array.mapNotNull { element ->
                val obj = element.jsonObject
                val name = obj["name"]!!.jsonPrimitive.content
                val categoryName = obj["category"]!!.jsonPrimitive.content
                val subCategoryName = obj["subCategory"]?.jsonPrimitive?.contentOrNull
                val errors = mutableListOf<String>()
                runCatching { DisposalCategory.valueOf(categoryName) }
                    .onFailure { errors += "category=$categoryName" }
                if (subCategoryName != null) {
                    runCatching { DisposalSubCategory.valueOf(subCategoryName) }
                        .onFailure { errors += "subCategory=$subCategoryName" }
                }
                if (errors.isEmpty()) null else "$name: ${errors.joinToString()}"
            }

        assertTrue("유효하지 않은 local_items 항목: $invalidEntries", invalidEntries.isEmpty())
    }

    @Test
    fun `상세 가이드의 relatedSpotTypes가 유효한 RelatedSpotType을 참조한다`() {
        val map = parseObject("item_guide_details.json")
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

    private fun parseObject(fileName: String) = json.parseToJsonElement(File(assetsDir, fileName).readText()).jsonObject
}
