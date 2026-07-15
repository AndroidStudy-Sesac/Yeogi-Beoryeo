package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.RegionalGuideFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.usecase.GetRegionalGuideFavoriteSnapshotUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoriteUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ToggleRegionalGuideFavoriteUseCase
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.region.repository.RegionRepository
import com.team.yeogibeoryeo.domain.region.usecase.ClassifyRegionSearchInputUseCase
import com.team.yeogibeoryeo.domain.region.usecase.ExtractRegionFromAddressUseCase
import com.team.yeogibeoryeo.domain.region.usecase.FindAdminDongCandidatesForLegalDongUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetEupmyeondongOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetSidoOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.GetSigunguOptionsUseCase
import com.team.yeogibeoryeo.domain.region.usecase.NormalizeRegionForRegionalGuideUseCase
import com.team.yeogibeoryeo.domain.region.usecase.ResolveRegionFromKeywordUseCase
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.repository.HomeRegionalGuidePrimaryFavoriteRepository
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import com.team.yeogibeoryeo.domain.regionalguide.usecase.GetRegionalDisposalGuideUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.GetRegionalGuideEupmyeondongOptionsUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.NormalizeRegionalGuideDisplayRegionUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.NormalizeRegionalGuideQueryUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.ResolveRegionalGuideRegionFromKeywordUseCase
import com.team.yeogibeoryeo.domain.regionalguide.usecase.SelectRegionalGuideCandidateUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description


internal fun createViewModel(
    regionRepository: RegionRepository = FakeRegionRepository(),
    regionOptionsRepository: RegionOptionsRepository = FakeRegionOptionsRepository(),
    regionalGuideRepository: RegionalDisposalGuideRepository = FakeRegionalDisposalGuideRepository(),
    regionalGuideOptionRepository: RegionalDisposalGuideRepository = FakeRegionalDisposalGuideRepository(),
    favoriteRepository: FavoriteRepository = FakeFavoriteRepository(),
    regionalGuideSnapshotRepository: RegionalGuideFavoriteSnapshotRepository =
        FakeRegionalGuideFavoriteSnapshotRepository(),
    regionalGuideFavoriteRepository: RegionalGuideFavoriteRepository =
        FakeRegionalGuideFavoriteRepository(
            favoriteRepository = favoriteRepository,
            snapshotRepository = regionalGuideSnapshotRepository,
        ),
): RegionalGuideViewModel {
    return RegionalGuideViewModel(
        classifyRegionSearchInputUseCase = ClassifyRegionSearchInputUseCase(),
        resolveRegionFromKeywordUseCase = ResolveRegionalGuideRegionFromKeywordUseCase(
            resolveRegionFromKeywordUseCase = ResolveRegionFromKeywordUseCase(
                repository = regionRepository,
                regionOptionsRepository = regionOptionsRepository
            )
        ),
        extractRegionFromAddressUseCase = ExtractRegionFromAddressUseCase(regionRepository),
        getRegionalDisposalGuideUseCase = GetRegionalDisposalGuideUseCase(
            repository = regionalGuideRepository,
            normalizeRegionalGuideQueryUseCase = NormalizeRegionalGuideQueryUseCase(),
            selectRegionalGuideCandidateUseCase = SelectRegionalGuideCandidateUseCase(),
            findAdminDongCandidatesForLegalDongUseCase = FindAdminDongCandidatesForLegalDongUseCase(
                regionOptionsRepository
            )
        ),
        getSidoOptionsUseCase = GetSidoOptionsUseCase(regionOptionsRepository),
        getSigunguOptionsUseCase = GetSigunguOptionsUseCase(regionOptionsRepository),
        getRegionalGuideEupmyeondongOptionsUseCase = GetRegionalGuideEupmyeondongOptionsUseCase(
            getEupmyeondongOptionsUseCase = GetEupmyeondongOptionsUseCase(regionOptionsRepository),
            normalizeRegionalGuideQueryUseCase = NormalizeRegionalGuideQueryUseCase(),
            repository = regionalGuideOptionRepository,
        ),
        normalizeRegionalGuideDisplayRegionUseCase = NormalizeRegionalGuideDisplayRegionUseCase(
            NormalizeRegionForRegionalGuideUseCase(regionOptionsRepository)
        ),
        observeFavoriteUseCase = ObserveFavoriteUseCase(favoriteRepository),
        toggleRegionalGuideFavoriteUseCase =
            ToggleRegionalGuideFavoriteUseCase(
                repository = regionalGuideFavoriteRepository,
                homeRegionalGuidePrimaryFavoriteRepository =
                    FakeHomeRegionalGuidePrimaryFavoriteRepository(),
            ),
        getRegionalGuideFavoriteSnapshotUseCase =
            GetRegionalGuideFavoriteSnapshotUseCase(regionalGuideSnapshotRepository),
    )
}

internal fun sampleGuide(
    sido: String,
    sigungu: String?,
    targetRegionName: String?
): RegionalDisposalGuide {
    return RegionalDisposalGuide(
        region = Region(
            sido = sido,
            sigungu = sigungu
        ),
        targetRegionName = targetRegionName,
        schedules = emptyList()
    )
}

internal class FakeRegionRepository(
    private val resolvedRegion: Region? = null,
    private val extractedRegion: Region? = null
) : RegionRepository {
    val extractedAddresses = mutableListOf<String>()
    val resolvedKeywords = mutableListOf<String>()

    override fun extractRegionFromAddress(address: String): Region? {
        extractedAddresses += address
        return extractedRegion
    }

    override suspend fun resolveRegionFromKeyword(keyword: String): Region? {
        resolvedKeywords += keyword
        return resolvedRegion
    }

    override suspend fun resolveRegionFromCoordinate(
        latitude: Double,
        longitude: Double
    ): Region? = null
}

internal class FakeRegionOptionsRepository(
    private val sidoOptions: List<String> = listOf("서울특별시", "경기도"),
    private val sigunguOptionsBySido: Map<String, List<String>> = emptyMap(),
    private val eupmyeondongOptionsByRegion: Map<String, Map<String, List<String>>> = emptyMap(),
    private val delayedSigunguOptionsBySido: Map<String, CompletableDeferred<List<String>>> = emptyMap(),
    private val keywordRegions: List<Region> = emptyList(),
    private val adminDongCandidates: List<Region> = emptyList(),
) : RegionOptionsRepository {

    override suspend fun getSidoOptions(): List<String> = sidoOptions

    override suspend fun getSigunguOptions(
        sido: String
    ): List<String> {
        return delayedSigunguOptionsBySido[sido]?.await()
            ?: sigunguOptionsBySido[sido].orEmpty()
    }

    override suspend fun getEupmyeondongOptions(
        sido: String,
        sigungu: String
    ): List<String> {
        return eupmyeondongOptionsByRegion[sido]
            ?.get(sigungu)
            .orEmpty()
    }

    override suspend fun findRegionsByEupmyeondongKeyword(
        keyword: String
    ): List<Region> {
        val exactMatches = keywordRegions.filter { region -> region.eupmyeondong == keyword }

        return exactMatches.ifEmpty {
            keywordRegions.filter { region ->
                region.eupmyeondong?.startsWith(keyword) == true
            }
        }
    }

    override suspend fun findRegionsBySigunguKeyword(
        keyword: String
    ): List<Region> {
        val exactMatches = keywordRegions.filter { region -> region.sigungu == keyword }

        val prefixMatches = exactMatches.ifEmpty {
            keywordRegions.filter { region ->
                region.sigungu?.startsWith(keyword) == true
            }
        }

        return prefixMatches.ifEmpty {
            keywordRegions.filter { region ->
                region.sigungu?.contains(keyword) == true
            }
        }
    }

    override suspend fun normalizeRegionForRegionalGuide(
        region: Region
    ): Region {
        val selectedSido = region.sido
        val selectedSigungu = region.sigungu
            ?.takeIf { sigungu -> sigungu.isNotBlank() }
            ?.substringBefore(" ")
            ?.let { sigungu ->
                if (sigungu.contains("시") && sigungu.contains("구")) {
                    sigungu.substringBefore("시") + "시"
                } else {
                    sigungu
                }
            }

        return region.copy(
            sido = selectedSido,
            sigungu = selectedSigungu
        )
    }

    override suspend fun findAdminDongCandidatesForLegalDong(
        region: Region
    ): List<Region> = adminDongCandidates
}

internal class FakeRegionalDisposalGuideRepository(
    private val candidates: List<RegionalDisposalGuide> = emptyList(),
    private val failure: Throwable? = null,
    private val throwable: Throwable? = null,
    private val delayMillis: Long = 0L,
) : RegionalDisposalGuideRepository {
    val queries = mutableListOf<RegionalGuideQuery>()

    override suspend fun getRegionalDisposalGuideCandidates(
        query: RegionalGuideQuery
    ): Result<List<RegionalDisposalGuide>> {
        queries += query
        if (delayMillis > 0) delay(delayMillis)
        throwable?.let { throw it }
        failure?.let { return Result.failure(it) }
        return Result.success(candidates)
    }
}

internal class FakeFavoriteRepository(
    initialFavorites: List<Favorite> = emptyList(),
) : FavoriteRepository {
    private val favorites = MutableStateFlow(initialFavorites)

    override fun observeFavorites(): Flow<List<Favorite>> = favorites

    override fun observeFavorite(
        type: FavoriteTargetType,
        targetId: String,
    ): Flow<Boolean> =
        favorites.map { items ->
            items.any { favorite -> favorite.type == type && favorite.targetId == targetId }
        }

    override suspend fun isFavorite(
        type: FavoriteTargetType,
        targetId: String,
    ): Boolean =
        favorites.value.any { favorite -> favorite.type == type && favorite.targetId == targetId }

    override suspend fun toggleFavorite(favorite: Favorite): Boolean {
        return if (isFavorite(favorite.type, favorite.targetId)) {
            removeFavorite(favorite.type, favorite.targetId)
            false
        } else {
            addFavorite(favorite)
            true
        }
    }

    override suspend fun addFavorite(favorite: Favorite) {
        favorites.value =
            favorites.value
                .filterNot { it.type == favorite.type && it.targetId == favorite.targetId } + favorite
    }

    override suspend fun removeFavorite(
        type: FavoriteTargetType,
        targetId: String,
    ) {
        favorites.value =
            favorites.value.filterNot { favorite ->
                favorite.type == type && favorite.targetId == targetId
            }
    }
}

internal class FakeRegionalGuideFavoriteSnapshotRepository(
    snapshots: List<RegionalGuideFavoriteSnapshot> = emptyList(),
) : RegionalGuideFavoriteSnapshotRepository {
    private val snapshots = MutableStateFlow(snapshots)

    override fun observeSnapshots(): Flow<List<RegionalGuideFavoriteSnapshot>> = snapshots

    override suspend fun getSnapshot(targetId: String): RegionalGuideFavoriteSnapshot? =
        snapshots.value.firstOrNull { snapshot -> snapshot.targetId == targetId }

    override suspend fun upsertSnapshot(snapshot: RegionalGuideFavoriteSnapshot) {
        snapshots.value =
            snapshots.value
                .filterNot { it.targetId == snapshot.targetId } + snapshot
    }

    override suspend fun deleteSnapshot(targetId: String) {
        snapshots.value = snapshots.value.filterNot { snapshot -> snapshot.targetId == targetId }
    }
}

internal class FakeRegionalGuideFavoriteRepository(
    private val favoriteRepository: FavoriteRepository,
    private val snapshotRepository: RegionalGuideFavoriteSnapshotRepository,
) : RegionalGuideFavoriteRepository {
    override suspend fun toggleFavorite(snapshot: RegionalGuideFavoriteSnapshot): Boolean {
        val favorite =
            Favorite(
                type = FavoriteTargetType.REGIONAL_GUIDE,
                targetId = snapshot.targetId,
                savedAtMillis = 1L,
            )
        val compatibleTargetIds = snapshot.compatibleTargetIds

        return if (
            compatibleTargetIds.any { targetId ->
                favoriteRepository.isFavorite(favorite.type, targetId)
            }
        ) {
            compatibleTargetIds.forEach { targetId ->
                favoriteRepository.removeFavorite(favorite.type, targetId)
                snapshotRepository.deleteSnapshot(targetId)
            }
            false
        } else {
            favoriteRepository.addFavorite(favorite)
            snapshotRepository.upsertSnapshot(snapshot)
            true
        }
    }

    override suspend fun removeFavorite(targetId: String) {
        val key = RegionalGuideFavoriteKey.decodeOrNull(targetId)
        val compatibleTargetIds =
            listOfNotNull(
                targetId,
                key?.copy(managementZoneName = null)?.encodeLegacy()
            ).distinct()

        compatibleTargetIds.forEach { compatibleTargetId ->
            favoriteRepository.removeFavorite(FavoriteTargetType.REGIONAL_GUIDE, compatibleTargetId)
            snapshotRepository.deleteSnapshot(compatibleTargetId)
        }
    }
}

internal class FakeHomeRegionalGuidePrimaryFavoriteRepository(
    initialTargetId: String? = null,
) : HomeRegionalGuidePrimaryFavoriteRepository {
    private val primaryTargetId = MutableStateFlow(initialTargetId)
    private val lastSelectedTargetId = MutableStateFlow<String?>(null)

    override fun observePrimaryFavoriteTargetId(): Flow<String?> = primaryTargetId

    override fun observeLastSelectedFavoriteTargetId(): Flow<String?> = lastSelectedTargetId

    override suspend fun setPrimaryFavoriteTargetId(targetId: String) {
        primaryTargetId.value = targetId
    }

    override suspend fun clearPrimaryFavoriteTargetId() {
        primaryTargetId.value = null
    }

    override suspend fun clearPrimaryFavoriteTargetIdIfMatches(targetId: String) {
        if (primaryTargetId.value == targetId) {
            primaryTargetId.value = null
        }
    }

    override suspend fun setLastSelectedFavoriteTargetId(targetId: String) {
        lastSelectedTargetId.value = targetId
    }

    override suspend fun clearLastSelectedFavoriteTargetId() {
        lastSelectedTargetId.value = null
    }

    override suspend fun clearLastSelectedFavoriteTargetIdIfMatches(targetId: String) {
        if (lastSelectedTargetId.value == targetId) {
            lastSelectedTargetId.value = null
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class RegionalGuideMainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

