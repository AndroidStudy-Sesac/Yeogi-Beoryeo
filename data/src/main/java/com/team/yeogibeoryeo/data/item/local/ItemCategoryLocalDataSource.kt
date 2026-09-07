package com.team.yeogibeoryeo.data.item.local

import android.content.Context
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSection
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSectionRow
import com.team.yeogibeoryeo.domain.item.model.DisposalSubGuide
import com.team.yeogibeoryeo.domain.item.model.RelatedSpotType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ItemGuideDetail(
    val id: String,
    val legacyNames: List<String> = emptyList(),
    val steps: List<String>,
    val cautions: List<String>,
    val subGuides: List<DisposalSubGuide> = emptyList(),
    val sections: List<DisposalGuideSection> = emptyList(),
    val tip: String?,
    val relatedSpotTypes: List<RelatedSpotType>,
    val sourceCategory: String? = null,
)

data class WasteDictionaryItem(
    val id: String,
    val name: String,
    val legacyNames: List<String> = emptyList(),
    val categoryPaths: List<List<String>>,
    val similarItems: List<String>,
    val dischargeMethods: List<String>,
    val features: List<String>,
    val notes: List<String>,
)

class ItemCategoryLocalDataSource internal constructor(
    private val readAssetText: (String) -> String,
    private val reporter: NonFatalErrorReporter,
) : ItemCategoryLocalSource {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        reporter: NonFatalErrorReporter,
    ) : this(
        readAssetText = { fileName ->
            context.assets
                .open(fileName)
                .bufferedReader()
                .use { it.readText() }
        },
        reporter = reporter,
    )

    private val json = Json { ignoreUnknownKeys = true }

    override fun getSynonyms(): Map<String, String> = cachedSynonyms

    override fun getGuideDetails(): Map<String, ItemGuideDetail> = cachedGuideDetails

    override fun getWasteDictionaryItems(): List<WasteDictionaryItem> = cachedWasteDictionaryItems

    private val cachedSynonyms: Map<String, String> by lazy {
        val assetText = readAsset("synonyms.json")
        parseAsset {
            val raw = json.parseToJsonElement(assetText).jsonObject
            raw.entries.associate { (synonym, canonical) ->
                synonym to canonical.jsonPrimitive.content
            }
        }
    }

    private val cachedGuideDetails: Map<String, ItemGuideDetail> by lazy {
        val assetText = readAsset("representative_guide_details.json")
        parseAsset {
            val raw = json.parseToJsonElement(assetText).jsonObject
            raw.entries.associate { (itemNm, value) ->
                val obj = value.jsonObject
                val relatedSpotTypes =
                    obj["relatedSpotTypes"]
                        ?.jsonArray
                        ?.map { RelatedSpotType.valueOf(it.jsonPrimitive.content) }
                        .orEmpty()

                itemNm to
                    ItemGuideDetail(
                        id = obj.requiredString("id"),
                        legacyNames = obj.stringList("legacyNames"),
                        steps = obj.stringList("steps"),
                        cautions = obj.stringList("cautions"),
                        subGuides =
                            obj["subGuides"]
                                ?.jsonArray
                                ?.map { subGuide ->
                                    val subGuideObject = subGuide.jsonObject
                                    DisposalSubGuide(
                                        name = subGuideObject.requiredString("name"),
                                        summary = subGuideObject.requiredString("summary"),
                                    )
                                }.orEmpty(),
                        sections =
                            obj["sections"]
                                ?.jsonArray
                                ?.map { section ->
                                    val sectionObject = section.jsonObject
                                    DisposalGuideSection(
                                        title = sectionObject.requiredString("title"),
                                        lines = sectionObject.stringList("lines"),
                                        rows =
                                            sectionObject["rows"]
                                                ?.jsonArray
                                                ?.map { row ->
                                                    val rowObject = row.jsonObject
                                                    DisposalGuideSectionRow(
                                                        label = rowObject.requiredString("label"),
                                                        value = rowObject.requiredString("value"),
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
    }

    private val cachedWasteDictionaryItems: List<WasteDictionaryItem> by lazy {
        val assetText = readAsset("item_disposal_guides.json")
        parseAsset {
            val array = json.parseToJsonElement(assetText).jsonArray
            array.map { element ->
                val obj = element.jsonObject
                WasteDictionaryItem(
                    id = obj.requiredString("id"),
                    name = obj.requiredString("name"),
                    legacyNames = obj.stringList("legacyNames"),
                    categoryPaths =
                        obj.requiredArray("categoryPaths")
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
    }

    private fun readAsset(fileName: String): String =
        try {
            readAssetText(fileName)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            reporter.report(exception, ASSET_READ_ERROR_CONTEXT)
            throw exception
        }

    private fun <T> parseAsset(parse: () -> T): T =
        try {
            parse()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            reporter.report(exception, ASSET_PARSING_ERROR_CONTEXT)
            throw exception
        }

    private fun JsonObject.requiredString(key: String): String =
        requireNotNull(this[key]) { "필수 JSON key가 누락되었습니다: $key" }
            .jsonPrimitive
            .content

    private fun JsonObject.requiredArray(key: String): JsonArray =
        requireNotNull(this[key]) { "필수 JSON key가 누락되었습니다: $key" }
            .jsonArray

    private fun JsonObject.stringList(key: String): List<String> =
        this[key]
            ?.jsonArray
            ?.map { it.jsonPrimitive.content }
            .orEmpty()

    private companion object {
        val ASSET_READ_ERROR_CONTEXT =
            NonFatalErrorContext(
                api = NonFatalApi.ITEM_GUIDE,
                stage = NonFatalStage.ASSET_LOAD,
                category = NonFatalCategory.IO,
            )
        val ASSET_PARSING_ERROR_CONTEXT =
            NonFatalErrorContext(
                api = NonFatalApi.ITEM_GUIDE,
                stage = NonFatalStage.ASSET_LOAD,
                category = NonFatalCategory.PARSING,
            )
    }
}
