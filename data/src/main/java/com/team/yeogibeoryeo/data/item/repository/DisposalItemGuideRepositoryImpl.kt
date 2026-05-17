package com.team.yeogibeoryeo.data.item.repository

import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import com.team.yeogibeoryeo.data.item.local.ItemCategoryLocalSource
import com.team.yeogibeoryeo.data.item.mapper.toDomain
import com.team.yeogibeoryeo.data.item.mapper.toDomain as dictionaryToDomain
import com.team.yeogibeoryeo.data.item.remote.datasource.ItemRemoteDataSource
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalRecyclability
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
        val dictionaryItems =
            localDataSource
                .getWasteDictionaryItems()
                .filter {
                    it.name.contains(normalizedQuery, ignoreCase = true) ||
                        it.name.contains(resolvedQuery, ignoreCase = true) ||
                        it.similarItems.any { similarItem ->
                            similarItem.contains(normalizedQuery, ignoreCase = true) ||
                                similarItem.contains(resolvedQuery, ignoreCase = true)
                        }
                }.map { it.dictionaryToDomain() }
                .distinctBy { it.name }

        if (dictionaryItems.isNotEmpty()) return dictionaryItems

        val remoteItems =
            remoteDataSource
                .searchItems(
                    serviceKey = publicDataKeyProvider.publicDataServiceKey,
                    itemNm = resolvedQuery,
                ).map { it.toDomain(categoryMap, relatedSpots, guideDetails, guideDetailAliases) }
                .distinctBy { it.name }

        if (remoteItems.isNotEmpty()) return remoteItems

        return localDataSource
            .getLocalItems()
            .filter {
                it.name.contains(normalizedQuery, ignoreCase = true) ||
                    it.name.contains(resolvedQuery, ignoreCase = true)
            }
    }

    override suspend fun getCategoryGuides(category: DisposalCategory): List<DisposalItemGuide> {
        val categoryMap = localDataSource.getCategoryMap()
        val relatedSpots = localDataSource.getRelatedSpots()
        val guideDetails = localDataSource.getGuideDetails()
        val guideDetailAliases = localDataSource.getGuideDetailAliases()
        val localItems = localDataSource.getLocalItems()

        val seedGuides =
            categoryMap
                .filter { (_, value) -> value.first == category }
                .map { (itemNm, value) ->
                    val subCategory = value.second
                    val guideDetailKey = guideDetailAliases[itemNm] ?: itemNm
                    val guideDetail = guideDetails[itemNm] ?: guideDetails[guideDetailKey]
                    val mergedRelatedSpotTypes =
                        guideDetail
                            ?.relatedSpotTypes
                            ?.takeIf { it.isNotEmpty() }
                            ?: relatedSpots[itemNm]

                    DisposalItemGuide(
                        id = itemNm,
                        name = itemNm,
                        category = category,
                        subCategory = subCategory,
                        instructions = emptyList(),
                        steps = guideDetail?.steps.orEmpty(),
                        cautions = guideDetail?.cautions.orEmpty(),
                        tip = guideDetail?.tip,
                        isRecyclable = DisposalRecyclability.fromCategory(category),
                        relatedSpotTypes = mergedRelatedSpotTypes,
                    )
                }

        val localCategoryItems = localItems.filter { it.category == category }

        return (seedGuides + localCategoryItems).distinctBy { it.name }
    }

    override fun getCategories(): List<DisposalCategory> = DisposalCategory.entries.toList()
}
