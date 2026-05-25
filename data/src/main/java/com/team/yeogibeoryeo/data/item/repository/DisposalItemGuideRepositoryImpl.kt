package com.team.yeogibeoryeo.data.item.repository

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.item.local.ItemCategoryLocalSource
import com.team.yeogibeoryeo.data.item.local.ItemGuideDetail
import com.team.yeogibeoryeo.data.item.local.WasteDictionaryItem
import com.team.yeogibeoryeo.data.item.mapper.toSourceCategoryInfo
import com.team.yeogibeoryeo.data.item.mapper.toDomain
import com.team.yeogibeoryeo.data.item.mapper.toDomain as dictionaryToDomain
import com.team.yeogibeoryeo.data.item.remote.datasource.ItemRemoteDataSource
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalRecyclability
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository
import javax.inject.Inject

class DisposalItemGuideRepositoryImpl
@Inject
constructor(
    private val remoteDataSource: ItemRemoteDataSource,
    private val localDataSource: ItemCategoryLocalSource,
    private val publicDataKeyProvider: AppKeyProvider,
) : DisposalItemGuideRepository {
    override suspend fun searchItemGuides(query: String): List<DisposalItemGuide> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isBlank()) return emptyList()

        val categoryMap = localDataSource.getCategoryMap()
        val synonyms = localDataSource.getSynonyms()
        val relatedSpots = localDataSource.getRelatedSpots()
        val guideDetails = localDataSource.getGuideDetails()
        val guideDetailAliases = localDataSource.getGuideDetailAliases()

        val resolvedQuery = synonyms[normalizedQuery] ?: normalizedQuery
        val dictionaryItems = localDataSource.getWasteDictionaryItems()
        val rankedDictionaryMatches =
            dictionaryItems
                .mapNotNull { item ->
                    val rank = item.dictionarySearchRank(normalizedQuery, resolvedQuery)
                        ?: return@mapNotNull null
                    item to rank
                }
        val bestDictionaryRank = rankedDictionaryMatches.minOfOrNull { it.second }
        val dictionaryMatches =
            rankedDictionaryMatches
                .filter { (_, rank) -> rank.isEligibleDictionaryRank(bestDictionaryRank) }
                .sortedWith(
                    compareBy(
                        { (_, rank) -> rank },
                        { (item, _) -> item.name.length },
                        { (item, _) -> item.name },
                    ),
                )
                .map { (item, _) -> item.dictionaryToDomain() }
                .distinctBy { it.name }

        if (dictionaryMatches.isNotEmpty()) return dictionaryMatches

        val remoteItems =
            remoteDataSource
                .searchItems(
                    serviceKey = publicDataKeyProvider.publicDataServiceKey,
                    itemNm = resolvedQuery,
                ).map { it.toDomain(categoryMap, relatedSpots, guideDetails, guideDetailAliases) }
                .distinctBy { it.name }

        return remoteItems
    }

    override suspend fun getCategoryGuides(category: DisposalCategory): List<DisposalItemGuide> {
        val categoryMap = localDataSource.getCategoryMap()
        val relatedSpots = localDataSource.getRelatedSpots()
        val guideDetails = localDataSource.getGuideDetails()

        return guideDetails
            .mapNotNull { (guideDetailKey, guideDetail) ->
                val categoryInfo =
                    resolveCategory(
                        guideDetailKey = guideDetailKey,
                        guideDetail = guideDetail,
                        categoryMap = categoryMap,
                    )
                if (categoryInfo.first != category) return@mapNotNull null

                val subCategory = categoryInfo.second
                val mergedRelatedSpotTypes =
                    guideDetail
                        .relatedSpotTypes
                        .takeIf { it.isNotEmpty() }
                        ?: relatedSpots[guideDetailKey]

                DisposalItemGuide(
                    id = guideDetailKey,
                    name = guideDetailKey,
                    category = category,
                    subCategory = subCategory,
                    instructions = emptyList(),
                    steps = guideDetail.steps,
                    cautions = guideDetail.cautions,
                    subGuides = guideDetail.subGuides,
                    detailSections = guideDetail.sections,
                    tip = guideDetail.tip,
                    isRecyclable = DisposalRecyclability.fromCategory(category),
                    relatedSpotTypes = mergedRelatedSpotTypes,
                )
            }
    }

    override fun getCategories(): List<DisposalCategory> = DisposalCategory.entries.toList()

    private fun WasteDictionaryItem.dictionarySearchRank(
        normalizedQuery: String,
        resolvedQuery: String,
    ): Int? =
        when {
            name.equals(normalizedQuery, ignoreCase = true) || name.equals(
                resolvedQuery,
                ignoreCase = true
            ) -> 0

            name.startsWith(normalizedQuery, ignoreCase = true) || name.startsWith(
                resolvedQuery,
                ignoreCase = true
            ) -> 1

            name.contains(normalizedQuery, ignoreCase = true) || name.contains(
                resolvedQuery,
                ignoreCase = true
            ) -> 2

            similarItems.any {
                it.equals(normalizedQuery, ignoreCase = true) || it.equals(
                    resolvedQuery,
                    ignoreCase = true
                )
            } -> 3

            similarItems.any {
                it.startsWith(normalizedQuery, ignoreCase = true) || it.startsWith(
                    resolvedQuery,
                    ignoreCase = true
                )
            } -> 4

            similarItems.any {
                it.contains(normalizedQuery, ignoreCase = true) || it.contains(
                    resolvedQuery,
                    ignoreCase = true
                )
            } -> 5

            else -> null
        }

    private fun Int.isEligibleDictionaryRank(bestRank: Int?): Boolean =
        when (bestRank) {
            0 -> this == 0
            1, 2 -> this in 1..2
            3 -> this == 3
            4, 5 -> this in 4..5
            else -> false
        }

    private fun resolveCategory(
        guideDetailKey: String,
        guideDetail: ItemGuideDetail?,
        categoryMap: Map<String, Pair<DisposalCategory, DisposalSubCategory?>>,
    ): Pair<DisposalCategory, DisposalSubCategory?> =
        guideDetail
            ?.sourceCategory
            .toSourceCategoryInfo()
            ?: categoryMap[guideDetailKey]
            ?: (DisposalCategory.OTHER to null)
}
