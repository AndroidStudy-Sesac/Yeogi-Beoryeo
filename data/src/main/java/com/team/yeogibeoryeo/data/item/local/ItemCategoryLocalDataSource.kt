package com.team.yeogibeoryeo.data.item.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject

data class ItemGuideDetail(
    val steps: List<String>,
    val cautions: List<String>,
    val tip: String?,
    val relatedSpotTypes: List<RelatedSpotType>,
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

    override fun getCategoryMap(): Map<String, Pair<DisposalCategory, DisposalSubCategory?>> = cachedCategoryMap

    override fun getSynonyms(): Map<String, String> = cachedSynonyms

    override fun getRelatedSpots(): Map<String, List<RelatedSpotType>> = cachedRelatedSpots

    override fun getGuideDetails(): Map<String, ItemGuideDetail> = cachedGuideDetails

    override fun getGuideDetailAliases(): Map<String, String> = cachedGuideDetailAliases

    override fun getLocalItems(): List<DisposalItemGuide> = cachedLocalItems

    private val cachedCategoryMap: Map<String, Pair<DisposalCategory, DisposalSubCategory?>> by lazy {
        val raw = json.parseToJsonElement(readAsset("category_map.json")).jsonObject
        raw.entries.associate { (itemNm, value) ->
            val obj = value.jsonObject
            val category = DisposalCategory.valueOf(obj["category"]!!.jsonPrimitive.content)
            val subCategoryStr = obj["subCategory"]?.jsonPrimitive?.contentOrNull
            val subCategory = subCategoryStr?.let { DisposalSubCategory.valueOf(it) }
            itemNm to (category to subCategory)
        }
    }

    private val cachedSynonyms: Map<String, String> by lazy {
        val raw = json.parseToJsonElement(readAsset("synonyms.json")).jsonObject
        raw.entries.associate { (synonym, canonical) ->
            synonym to canonical.jsonPrimitive.content
        }
    }

    private val cachedRelatedSpots: Map<String, List<RelatedSpotType>> by lazy {
        val raw = json.parseToJsonElement(readAsset("related_spots.json")).jsonObject
        raw.entries.associate { (itemNm, spotsArray) ->
            val spots =
                spotsArray.jsonArray.map {
                    RelatedSpotType.valueOf(it.jsonPrimitive.content)
                }
            itemNm to spots
        }
    }

    private val cachedGuideDetails: Map<String, ItemGuideDetail> by lazy {
        val raw = json.parseToJsonElement(readAsset("item_guide_details.json")).jsonObject
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
                    tip = obj["tip"]?.jsonPrimitive?.contentOrNull,
                    relatedSpotTypes = relatedSpotTypes,
                )
        }
    }

    private val cachedGuideDetailAliases: Map<String, String> by lazy {
        val raw = json.parseToJsonElement(readAsset("guide_detail_aliases.json")).jsonObject
        raw.entries.associate { (itemNm, guideKey) ->
            itemNm to guideKey.jsonPrimitive.content
        }
    }

    private val cachedLocalItems: List<DisposalItemGuide> by lazy {
        val array = json.parseToJsonElement(readAsset("local_items.json")).jsonArray
        array.map { element ->
            val obj = element.jsonObject
            val name = obj["name"]!!.jsonPrimitive.content
            val category = DisposalCategory.valueOf(obj["category"]!!.jsonPrimitive.content)
            val subCategoryStr = obj["subCategory"]?.jsonPrimitive?.contentOrNull
            val subCategory = subCategoryStr?.let { DisposalSubCategory.valueOf(it) }
            val method = obj["method"]!!.jsonPrimitive.content
            val tip = obj["tip"]?.jsonPrimitive?.contentOrNull
            val isRecyclable = obj["isRecyclable"]!!.jsonPrimitive.boolean
            val guideDetailKey = cachedGuideDetailAliases[name] ?: name
            val guideDetail = cachedGuideDetails[name] ?: cachedGuideDetails[guideDetailKey]
            val mergedRelatedSpotTypes =
                guideDetail
                    ?.relatedSpotTypes
                    ?.takeIf { it.isNotEmpty() }
                    ?: cachedRelatedSpots[name]

            DisposalItemGuide(
                id = "local_$name",
                name = name,
                category = category,
                subCategory = subCategory,
                instructions = listOf(DisposalInstruction(method = method, tip = tip)),
                steps = guideDetail?.steps.orEmpty(),
                cautions = guideDetail?.cautions.orEmpty(),
                tip = tip ?: guideDetail?.tip,
                isRecyclable = isRecyclable,
                relatedSpotTypes = mergedRelatedSpotTypes,
            )
        }
    }

    private fun kotlinx.serialization.json.JsonObject.stringList(key: String): List<String> =
        this[key]
            ?.jsonArray
            ?.map { it.jsonPrimitive.content }
            .orEmpty()
}
