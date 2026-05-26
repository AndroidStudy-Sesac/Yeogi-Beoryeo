package com.team.yeogibeoryeo.data.item.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSection
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSectionRow
import com.team.yeogibeoryeo.domain.item.model.DisposalSubGuide
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class ItemGuideDetail(
    val steps: List<String>,
    val cautions: List<String>,
    val subGuides: List<DisposalSubGuide> = emptyList(),
    val sections: List<DisposalGuideSection> = emptyList(),
    val tip: String?,
    val relatedSpotTypes: List<RelatedSpotType>,
    val sourceCategory: String? = null,
)

data class WasteDictionaryItem(
    val name: String,
    val categoryPaths: List<List<String>>,
    val similarItems: List<String>,
    val dischargeMethods: List<String>,
    val features: List<String>,
    val notes: List<String>,
)

class ItemCategoryLocalDataSource
@Inject
constructor(
    @param:ApplicationContext
    private val context: Context,
) : ItemCategoryLocalSource {
    private val json = Json { ignoreUnknownKeys = true }

    private fun readAsset(fileName: String): String =
        context.assets
            .open(fileName)
            .bufferedReader()
            .use { it.readText() }

    override fun getSynonyms(): Map<String, String> = cachedSynonyms

    override fun getGuideDetails(): Map<String, ItemGuideDetail> = cachedGuideDetails

    override fun getWasteDictionaryItems(): List<WasteDictionaryItem> = cachedWasteDictionaryItems

    private val cachedSynonyms: Map<String, String> by lazy {
        val raw = json.parseToJsonElement(readAsset("synonyms.json")).jsonObject
        raw.entries.associate { (synonym, canonical) ->
            synonym to canonical.jsonPrimitive.content
        }
    }

    private val cachedGuideDetails: Map<String, ItemGuideDetail> by lazy {
        val raw = json.parseToJsonElement(readAsset("representative_guide_details.json")).jsonObject
        raw.entries.associate { (itemNm, value) ->
            val obj = value.jsonObject
            val relatedSpotTypes =
                obj["relatedSpotTypes"]
                    ?.jsonArray
                    ?.map { RelatedSpotType.valueOf(it.jsonPrimitive.content) }
                    .orEmpty()

            itemNm to
                    ItemGuideDetail(
                        steps = obj.stringList("steps"),
                        cautions = obj.stringList("cautions"),
                        subGuides =
                            obj["subGuides"]
                                ?.jsonArray
                                ?.map { subGuide ->
                                    val subGuideObject = subGuide.jsonObject
                                    DisposalSubGuide(
                                        name = subGuideObject["name"]!!.jsonPrimitive.content,
                                        summary = subGuideObject["summary"]!!.jsonPrimitive.content,
                                    )
                                }.orEmpty(),
                        sections =
                            obj["sections"]
                                ?.jsonArray
                                ?.map { section ->
                                    val sectionObject = section.jsonObject
                                    DisposalGuideSection(
                                        title = sectionObject["title"]!!.jsonPrimitive.content,
                                        lines = sectionObject.stringList("lines"),
                                        rows =
                                            sectionObject["rows"]
                                                ?.jsonArray
                                                ?.map { row ->
                                                    val rowObject = row.jsonObject
                                                    DisposalGuideSectionRow(
                                                        label = rowObject["label"]!!.jsonPrimitive.content,
                                                        value = rowObject["value"]!!.jsonPrimitive.content,
                                                    )
                                                }.orEmpty(),
                                    )
                                }.orEmpty(),
                        tip = obj["tip"]?.jsonPrimitive?.contentOrNull,
                        relatedSpotTypes = relatedSpotTypes,
                        sourceCategory = obj["sourceCategory"]?.jsonPrimitive?.contentOrNull,
                    )
        }
    }

    private val cachedWasteDictionaryItems: List<WasteDictionaryItem> by lazy {
        val array = json.parseToJsonElement(readAsset("item_disposal_guides.json")).jsonArray
        array.map { element ->
            val obj = element.jsonObject
            WasteDictionaryItem(
                name = obj["name"]!!.jsonPrimitive.content,
                categoryPaths =
                    obj["categoryPaths"]!!
                        .jsonArray
                        .map { path ->
                            path.jsonArray.map { it.jsonPrimitive.content }
                        },
                similarItems = obj.stringList("similarItems"),
                dischargeMethods = obj.stringList("dischargeMethods"),
                features = obj.stringList("features"),
                notes = obj.stringList("notes"),
            )
        }
    }

    private fun kotlinx.serialization.json.JsonObject.stringList(key: String): List<String> =
        this[key]
            ?.jsonArray
            ?.map { it.jsonPrimitive.content }
            .orEmpty()
}
