package com.team.yeogibeoryeo.data.item.repository

import com.team.yeogibeoryeo.data.core.di.IoDispatcher
import com.team.yeogibeoryeo.data.item.local.ItemCategoryLocalSource
import com.team.yeogibeoryeo.data.item.local.ItemGuideDetail
import com.team.yeogibeoryeo.data.item.local.WasteDictionaryItem
import com.team.yeogibeoryeo.data.item.mapper.toDomain as dictionaryToDomain
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalRecyclability
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DisposalItemGuideRepositoryImpl
@Inject
constructor(
    private val localDataSource: ItemCategoryLocalSource,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DisposalItemGuideRepository {
    override suspend fun searchItemGuides(query: String): List<DisposalItemGuide> =
        withContext(ioDispatcher) {
            val searchQuery = query.toSearchKey()
            if (searchQuery.isBlank()) return@withContext emptyList()

            val dictionaryItems = localDataSource.getWasteDictionaryItems()
            val directMatches = dictionaryItems.searchBy(searchQuery)
            if (directMatches.isNotEmpty()) return@withContext directMatches

            val resolvedQuery = localDataSource.getSynonyms()[searchQuery]?.toSearchKey() ?: searchQuery
            if (resolvedQuery == searchQuery) return@withContext emptyList()

            dictionaryItems.searchBy(resolvedQuery)
        }

    private fun List<WasteDictionaryItem>.searchBy(query: String): List<DisposalItemGuide> {
        val rankedDictionaryMatches =
            mapNotNull { item ->
                val rank = item.dictionarySearchRank(query) ?: return@mapNotNull null
                item to rank
            }
        val bestDictionaryRank = rankedDictionaryMatches.minOfOrNull { it.second }

        return rankedDictionaryMatches
            .filter { (_, rank) -> rank.isEligibleDictionaryRank(bestDictionaryRank) }
            .sortedWith(
                compareBy(
                    { (_, rank) -> rank },
                    { (item, _) -> item.name },
                ),
            )
            .map { (item, _) -> item.dictionaryToDomain() }
            .distinctBy { it.name }
    }

    override suspend fun getCategoryGuides(category: DisposalCategory): List<DisposalItemGuide> =
        withContext(ioDispatcher) {
            val guideDetails = localDataSource.getGuideDetails()

            guideDetails
                .mapNotNull { (guideDetailKey, guideDetail) ->
                    val sourceCategory = resolveCategory(guideDetail)
                    if (sourceCategory != category) return@mapNotNull null

                    guideDetail.toDomain(
                        guideDetailKey = guideDetailKey,
                        category = sourceCategory,
                    )
                }
        }

    override suspend fun getItemGuide(guideId: String): DisposalItemGuide? =
        withContext(ioDispatcher) {
            val guideDetail = localDataSource.getGuideDetails()[guideId]

            if (guideDetail != null) {
                guideDetail.toDomain(
                    guideDetailKey = guideId,
                    category = resolveCategory(guideDetail),
                )
            } else {
                val searchResults = searchItemGuides(guideId)
                searchResults.firstOrNull { it.id == guideId || it.name == guideId }
                    ?: searchResults.firstOrNull()
            }
        }

    override fun getCategories(): List<DisposalCategory> = DisposalCategory.entries.toList()

    private fun WasteDictionaryItem.dictionarySearchRank(query: String): Int? {
        val nameSearchKey = name.toSearchKey()
        val similarItemSearchKeys = similarItems.map { it.toSearchKey() }

        return when {
            nameSearchKey.equals(query, ignoreCase = true) -> 0

            nameSearchKey.startsWith(query, ignoreCase = true) -> 1

            nameSearchKey.contains(query, ignoreCase = true) -> 2

            similarItemSearchKeys.any { it.equals(query, ignoreCase = true) } -> 3

            similarItemSearchKeys.any { it.startsWith(query, ignoreCase = true) } -> 4

            similarItemSearchKeys.any { it.contains(query, ignoreCase = true) } -> 5

            else -> null
        }
    }

    private fun Int.isEligibleDictionaryRank(bestRank: Int?): Boolean =
        when (bestRank) {
            0 -> this == 0
            1, 2 -> this in 1..2
            3 -> this == 3
            4, 5 -> this in 4..5
            else -> false
        }

    private fun String.toSearchKey(): String = filterNot { it.isWhitespace() }

    private fun resolveCategory(
        guideDetail: ItemGuideDetail?,
    ): DisposalCategory =
        guideDetail
            ?.sourceCategory
            ?.let(DisposalCategory::fromDisplayName)
            ?: DisposalCategory.OTHER

    private fun ItemGuideDetail.toDomain(
        guideDetailKey: String,
        category: DisposalCategory,
    ): DisposalItemGuide {
        return DisposalItemGuide(
            id = guideDetailKey,
            name = guideDetailKey,
            category = category,
            subCategory = null,
            instructions = emptyList(),
            steps = steps,
            cautions = cautions,
            subGuides = subGuides,
            detailSections = sections,
            tip = tip,
            isRecyclable = DisposalRecyclability.fromCategory(category),
            relatedSpotTypes = relatedSpotTypes.takeIf { it.isNotEmpty() },
        )
    }
}
