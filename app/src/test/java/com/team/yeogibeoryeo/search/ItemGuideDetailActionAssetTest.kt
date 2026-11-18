package com.team.yeogibeoryeo.search

import com.team.yeogibeoryeo.data.item.local.WasteDictionaryItem
import com.team.yeogibeoryeo.data.item.mapper.toDomain
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.search.model.ItemGuideDetailAction
import com.team.yeogibeoryeo.presentation.search.model.toDetailActions
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ItemGuideDetailActionAssetTest {
    private val dictionaryItems = Json.parseToJsonElement(
        File("../data/src/main/assets/item_disposal_guides.json").readText(Charsets.UTF_8),
    ).jsonArray.associate { element ->
        val item = element.jsonObject
        val id = item.getValue("id").jsonPrimitive.content
        id to WasteDictionaryItem(
            id = id,
            name = item.getValue("name").jsonPrimitive.content,
            legacyNames = item.stringList("legacyNames"),
            searchTerms = item.stringList("searchTerms"),
            categoryPaths = item.getValue("categoryPaths").jsonArray.map { path ->
                path.jsonArray.map { it.jsonPrimitive.content }
            },
            similarItems = item.stringList("similarItems"),
            dischargeMethods = item.stringList("dischargeMethods"),
            features = item.stringList("features"),
            notes = item.stringList("notes"),
        )
    }

    @Test
    fun `의류수거함 배출 금지 품목은 의류수거함 CTA를 만들지 않는다`() {
        val excludedItemIds = listOf(
            "item-guide-0034",
            "item-guide-0084",
            "item-guide-0265",
            "item-guide-0270",
            "item-guide-0276",
            "item-guide-0319",
            "item-guide-0320",
            "item-guide-0321",
            "item-guide-0344",
            "item-guide-0367",
            "item-guide-0409",
            "item-guide-0410",
            "item-guide-0424",
            "item-guide-0548",
            "item-guide-0555",
        )

        excludedItemIds.forEach { id ->
            assertFalse(
                dictionaryItems.getValue(id).name,
                CollectionSpotType.CLOTHING_BIN in mapTypes(id),
            )
        }
    }

    @Test
    fun `캐리어 인형 군복은 실제 배출방법에 해당하는 CTA만 만든다`() {
        assertEquals(emptyList<CollectionSpotType>(), mapTypes("item-guide-0084"))
        assertEquals(listOf(CollectionSpotType.STANDARD_BAG_STORE), mapTypes("item-guide-0270"))
        assertEquals(listOf(CollectionSpotType.STANDARD_BAG_STORE), mapTypes("item-guide-0409"))
    }

    @Test
    fun `약통 용기의 내용물 안내는 폐의약품 수거함 CTA를 만들지 않는다`() {
        assertEquals(emptyList<CollectionSpotType>(), mapTypes("item-guide-0237"))
    }

    @Test
    fun `영양제의 조건부 안내는 폐의약품 수거함 CTA를 만들지 않는다`() {
        assertEquals(listOf(CollectionSpotType.STANDARD_BAG_STORE), mapTypes("item-guide-0355"))
    }

    @Test
    fun `스카프와 해열진통제는 실제 배출방법의 수거함 CTA를 유지한다`() {
        assertEquals(
            listOf(CollectionSpotType.STANDARD_BAG_STORE, CollectionSpotType.CLOTHING_BIN),
            mapTypes("item-guide-0117"),
        )
        assertEquals(listOf(CollectionSpotType.MEDICINE_DROP_BOX), mapTypes("item-guide-0068"))
    }

    private fun mapTypes(id: String): List<CollectionSpotType> =
        dictionaryItems.getValue(id).toDomain().toDetailActions()
            .filterIsInstance<ItemGuideDetailAction.MapSpot>()
            .map { it.type }

    private fun JsonObject.stringList(key: String): List<String> =
        get(key)?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
}
