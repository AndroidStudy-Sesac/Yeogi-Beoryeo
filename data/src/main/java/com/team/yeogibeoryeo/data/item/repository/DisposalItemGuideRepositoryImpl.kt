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
import info.debatty.java.stringsimilarity.Levenshtein
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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

    override suspend fun suggestSearchQueries(query: String): List<String> =
        withContext(ioDispatcher) {
            val searchKey = query.toSearchKey()
            if (searchKey.length < 3) return@withContext emptyList()

            val items = localDataSource.getWasteDictionaryItems()
            val synonyms = localDataSource.getSynonyms()
            fun hasResults(term: String): Boolean {
                val key = term.toSearchKey()
                return items.any { it.dictionarySearchRank(key) != null } ||
                    synonyms[key]?.toSearchKey()?.let { resolved ->
                        items.any { it.dictionarySearchRank(resolved) != null }
                    } == true
            }
            if (hasResults(searchKey)) return@withContext emptyList()

            val distance = Levenshtein()
            val context = currentCoroutineContext()
            val normalizedQuery = searchKey.lowercase()
            (items.asSequence().flatMap { sequenceOf(it.name) + it.searchTerms.asSequence() } +
                synonyms.keys.asSequence() + synonyms.values.asSequence())
                .map { it.trim() }
                .distinctBy { it.toSearchKey().lowercase() }
                .filter { candidate ->
                    context.ensureActive()
                    val key = candidate.toSearchKey().lowercase()
                    key.length >= 3 && kotlin.math.abs(key.length - normalizedQuery.length) <= 1 &&
                        // 제한값 이상은 제한값으로 반환되므로 허용 거리 1보다 큰 값을 사용합니다.
                        distance.distance(normalizedQuery, key, 2) <= 1
                }
                .sortedBy { it.toSearchKey().lowercase() }
                .filter(::hasResults)
                .take(3)
                .toList()
        }

    private fun List<WasteDictionaryItem>.searchBy(query: String): List<DisposalItemGuide> {
        val rankedDictionaryMatches =
            mapNotNull { item ->
                val rank = item.dictionarySearchRank(query) ?: return@mapNotNull null
                item to rank
            }
        val bestDictionaryRank =
            rankedDictionaryMatches.minOfOrNull { it.second }
                ?: return emptyList()

        return rankedDictionaryMatches
            .filter { (_, rank) -> rank.isEligibleDictionaryRank(bestDictionaryRank) }
            .sortedWith(
                compareBy(
                    { (_, rank) -> rank },
                    { (item, _) -> !item.matchesExactSearchTerm(query) },
                    { (item, _) -> item.name },
                ),
            )
            .map { (item, _) -> item.dictionaryToDomain() }
            .distinctBy { it.id }
    }

    override suspend fun getCategoryGuides(category: DisposalCategory): List<DisposalItemGuide> =
        withContext(ioDispatcher) {
            val guideDetails = localDataSource.getGuideDetails()

            guideDetails
                .mapNotNull { (guideDetailKey, guideDetail) ->
                    val sourceCategory = resolveCategory(guideDetail)
                    if (sourceCategory != category) return@mapNotNull null

                    guideDetail.toDomain(
                        guideName = guideDetailKey,
                        category = sourceCategory,
                    )
                }
        }

    override suspend fun getItemGuide(guideId: String): DisposalItemGuide? =
        withContext(ioDispatcher) {
            val guideDetailEntry =
                localDataSource.getGuideDetails().entries.firstOrNull { (name, detail) ->
                    guideId == detail.id || guideId == name || guideId in detail.legacyNames
                }
            if (guideDetailEntry != null) {
                val (guideName, guideDetail) = guideDetailEntry
                return@withContext guideDetail.toDomain(
                    guideName = guideName,
                    category = resolveCategory(guideDetail),
                )
            }

            localDataSource.getWasteDictionaryItems()
                .firstOrNull { item ->
                    guideId == item.id || guideId == item.name || guideId in item.legacyNames
                }
                ?.dictionaryToDomain()
        }

    override fun getCategories(): List<DisposalCategory> = DisposalCategory.entries.toList()

    private fun WasteDictionaryItem.dictionarySearchRank(query: String): Int? {
        val nameSearchKey = name.toSearchKey()
        val similarItemSearchKeys = similarItems.map { it.toSearchKey() }

        return when {
            nameSearchKey.equals(query, ignoreCase = true) -> 0

            matchesExactSearchTerm(query) -> 1

            nameSearchKey.startsWith(query, ignoreCase = true) -> 1

            nameSearchKey.contains(query, ignoreCase = true) -> 2

            similarItemSearchKeys.any { it.equals(query, ignoreCase = true) } -> 3

            similarItemSearchKeys.any { it.startsWith(query, ignoreCase = true) } -> 4

            similarItemSearchKeys.any { it.contains(query, ignoreCase = true) } -> 5

            else -> null
        }
    }

    private fun WasteDictionaryItem.matchesExactSearchTerm(query: String): Boolean =
        searchTerms.any { term -> term.toSearchKey().equals(query, ignoreCase = true) }

    private fun Int.isEligibleDictionaryRank(bestRank: Int): Boolean =
        when (bestRank) {
            0 -> this == 0
            1, 2 -> this in 1..2
            3 -> this == 3
            else -> this in 4..5
        }

    private fun String.toSearchKey(): String = filterNot { it.isWhitespace() || it in "()（）" }

    private fun resolveCategory(
        guideDetail: ItemGuideDetail?,
    ): DisposalCategory =
        guideDetail
            ?.sourceCategory
            ?.let(DisposalCategory::fromDisplayName)
            ?: DisposalCategory.OTHER

    private fun ItemGuideDetail.toDomain(
        guideName: String,
        category: DisposalCategory,
    ): DisposalItemGuide {
        return DisposalItemGuide(
            id = id,
            name = guideName,
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
