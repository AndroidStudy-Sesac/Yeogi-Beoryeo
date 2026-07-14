package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.favorite.model.CollectionSpotFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteSnapshotRepository
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ToggleCollectionSpotFavoriteUseCase
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotSearchResult
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheClearResult
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotGeocodingRepository
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import com.team.yeogibeoryeo.domain.spot.repository.RecentCurrentLocationSpotCacheRepository
import com.team.yeogibeoryeo.domain.spot.usecase.CalculateDistanceMeterUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.ClearRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.FilterCollectionSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.GetFreshRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.NormalizeCollectionSpotSearchKeywordUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.ResolveMapRegionSearchCandidateUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SaveRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByKeywordUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByLocationUseCase
import com.team.yeogibeoryeo.domain.time.TimeProvider
import com.team.yeogibeoryeo.presentation.cache.RecentCurrentLocationCacheClearNotifier
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationProvider
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
import com.team.yeogibeoryeo.presentation.map.location.LocationPermissionChecker
import com.team.yeogibeoryeo.presentation.map.model.FavoriteSpotMapMoveRequest
import com.team.yeogibeoryeo.presentation.search.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Rule

abstract class CollectionSpotMapViewModelTestFixture {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
}

internal fun createViewModel(
    repository: FakeCollectionSpotRepository,
    currentLocationResult: CurrentLocationResult,
    hasFineLocationPermission: Boolean = true,
    recentCurrentLocationSpotCacheRepository: RecentCurrentLocationSpotCacheRepository =
        FakeRecentCurrentLocationSpotCacheRepository(),
    timeProvider: TimeProvider = FakeTimeProvider(),
    favoriteRepository: FakeFavoriteRepository = FakeFavoriteRepository(),
    regionOptionsRepository: FakeMapRegionOptionsRepository = FakeMapRegionOptionsRepository(),
    snapshotRepository: FakeCollectionSpotFavoriteSnapshotRepository =
        FakeCollectionSpotFavoriteSnapshotRepository(),
    collectionSpotFavoriteRepository: CollectionSpotFavoriteRepository? = null,
    recentCurrentLocationCacheClearNotifier: RecentCurrentLocationCacheClearNotifier =
        RecentCurrentLocationCacheClearNotifier(),
): CollectionSpotMapViewModel {
    return createViewModel(
        repository = repository,
        currentLocationProvider = FakeCurrentLocationProvider(currentLocationResult),
        hasFineLocationPermission = hasFineLocationPermission,
        recentCurrentLocationSpotCacheRepository = recentCurrentLocationSpotCacheRepository,
        timeProvider = timeProvider,
        favoriteRepository = favoriteRepository,
        regionOptionsRepository = regionOptionsRepository,
        snapshotRepository = snapshotRepository,
        collectionSpotFavoriteRepository = collectionSpotFavoriteRepository,
        recentCurrentLocationCacheClearNotifier = recentCurrentLocationCacheClearNotifier,
    )
}

internal fun createViewModel(
    repository: FakeCollectionSpotRepository,
    currentLocationProvider: CurrentLocationProvider,
    hasFineLocationPermission: Boolean = true,
    recentCurrentLocationSpotCacheRepository: RecentCurrentLocationSpotCacheRepository =
        FakeRecentCurrentLocationSpotCacheRepository(),
    timeProvider: TimeProvider = FakeTimeProvider(),
    favoriteRepository: FakeFavoriteRepository = FakeFavoriteRepository(),
    regionOptionsRepository: FakeMapRegionOptionsRepository = FakeMapRegionOptionsRepository(),
    snapshotRepository: FakeCollectionSpotFavoriteSnapshotRepository =
        FakeCollectionSpotFavoriteSnapshotRepository(),
    collectionSpotFavoriteRepository: CollectionSpotFavoriteRepository? = null,
    recentCurrentLocationCacheClearNotifier: RecentCurrentLocationCacheClearNotifier =
        RecentCurrentLocationCacheClearNotifier(),
): CollectionSpotMapViewModel {
    val normalizeKeywordUseCase = NormalizeCollectionSpotSearchKeywordUseCase()

    return CollectionSpotMapViewModel(
        resolveMapRegionSearchCandidateUseCase = ResolveMapRegionSearchCandidateUseCase(
            regionOptionsRepository = regionOptionsRepository,
            normalizeKeywordUseCase = normalizeKeywordUseCase,
        ),
        searchCollectionSpotsByKeywordUseCase = SearchCollectionSpotsByKeywordUseCase(
            repository = repository,
            geocodingRepository = repository,
            normalizeKeywordUseCase = normalizeKeywordUseCase,
        ),
        searchCollectionSpotsByLocationUseCase = SearchCollectionSpotsByLocationUseCase(
            repository = repository,
            geocodingRepository = repository,
        ),
        filterCollectionSpotsUseCase = FilterCollectionSpotsUseCase(),
        currentLocationProvider = currentLocationProvider,
        locationPermissionChecker = FakeLocationPermissionChecker(hasFineLocationPermission),
        getFreshRecentCurrentLocationSpotsUseCase = GetFreshRecentCurrentLocationSpotsUseCase(
            repository = recentCurrentLocationSpotCacheRepository,
            timeProvider = timeProvider,
        ),
        saveRecentCurrentLocationSpotsUseCase = SaveRecentCurrentLocationSpotsUseCase(
            repository = recentCurrentLocationSpotCacheRepository,
            timeProvider = timeProvider,
        ),
        clearRecentCurrentLocationSpotsUseCase = ClearRecentCurrentLocationSpotsUseCase(
            repository = recentCurrentLocationSpotCacheRepository,
        ),
        observeFavoritesUseCase = ObserveFavoritesUseCase(favoriteRepository),
        toggleCollectionSpotFavoriteUseCase = ToggleCollectionSpotFavoriteUseCase(
            collectionSpotFavoriteRepository =
                collectionSpotFavoriteRepository
                    ?: FakeCollectionSpotFavoriteRepository(
                        favoriteRepository = favoriteRepository,
                        snapshotRepository = snapshotRepository,
                    ),
        ),
        calculateDistanceMeterUseCase = CalculateDistanceMeterUseCase(),
        recentCurrentLocationCacheClearNotifier = recentCurrentLocationCacheClearNotifier,
    )
}

internal fun freshCacheEntry(
    spots: List<CollectionSpot>,
): RecentCurrentLocationSpotCacheEntry {
    return RecentCurrentLocationSpotCacheEntry(
        spots = spots,
        savedAtMillis = TEST_NOW_MILLIS,
    )
}

internal fun sampleSpot(
    id: String,
    type: CollectionSpotType,
): CollectionSpot {
    return CollectionSpot(
        id = id,
        name = "수거 장소 $id",
        type = type,
        address = "서울시 중구",
        detailLocation = null,
        coordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881),
    )
}

internal fun List<CollectionSpot>.withDistanceFrom(
    coordinate: Coordinate,
): List<CollectionSpot> {
    val calculateDistanceMeterUseCase = CalculateDistanceMeterUseCase()

    return map { spot ->
        val spotCoordinate = spot.coordinate ?: return@map spot

        spot.copy(
            distanceMeter = calculateDistanceMeterUseCase(
                from = coordinate,
                to = spotCoordinate,
            ),
        )
    }
}

internal fun sampleFavoriteSpotMapMoveRequest(
    requestId: String = "request-1",
): FavoriteSpotMapMoveRequest {
    return FavoriteSpotMapMoveRequest(
        requestId = requestId,
        targetId = "favorite-spot",
        name = "폐건전지 수거함",
        type = CollectionSpotType.BATTERY_BIN,
        address = "서울특별시 영등포구 문래동",
        detailLocation = "주민센터 앞",
        coordinate = Coordinate(latitude = 37.5, longitude = 126.9),
    )
}

internal fun CollectionSpot.toFavoriteSnapshot(): CollectionSpotFavoriteSnapshot =
    CollectionSpotFavoriteSnapshot(
        targetId = id,
        name = name,
        type = type,
        address = address,
        detailLocation = detailLocation,
        coordinate = coordinate,
    )

internal class FakeCurrentLocationProvider(
    internal val resultProvider: suspend () -> CurrentLocationResult,
) : CurrentLocationProvider {

    constructor(result: CurrentLocationResult) : this({ result })

    override suspend fun getCurrentLocation(): CurrentLocationResult = resultProvider()
}

internal class FakeLocationPermissionChecker(
    internal val hasFineLocationPermission: Boolean,
) : LocationPermissionChecker {
    override fun hasFineLocationPermission(): Boolean = hasFineLocationPermission
}

internal class FakeTimeProvider(
    internal val nowMillis: Long = TEST_NOW_MILLIS,
) : TimeProvider {
    override fun currentTimeMillis(): Long = nowMillis
}

internal class FakeRecentCurrentLocationSpotCacheRepository(
    var entry: RecentCurrentLocationSpotCacheEntry? = null,
    private val getCompletion: kotlinx.coroutines.CompletableDeferred<Unit>? = null,
) : RecentCurrentLocationSpotCacheRepository {
    var getCallCount = 0
        private set
    var saveCallCount = 0
        private set
    var clearCallCount = 0
        private set

    override suspend fun getRecentCurrentLocationSpots(): RecentCurrentLocationSpotCacheEntry? {
        getCallCount += 1
        getCompletion?.await()
        return entry
    }

    override suspend fun saveRecentCurrentLocationSpots(entry: RecentCurrentLocationSpotCacheEntry) {
        saveCallCount += 1
        this.entry = entry
    }

    override suspend fun clearRecentCurrentLocationSpots(): RecentCurrentLocationSpotCacheClearResult {
        clearCallCount += 1
        val hadCache = entry != null
        entry = null

        return if (hadCache) {
            RecentCurrentLocationSpotCacheClearResult.Deleted
        } else {
            RecentCurrentLocationSpotCacheClearResult.NoCache
        }
    }
}

internal class FakeCollectionSpotRepository(
    internal val keywordSpots: List<CollectionSpot> = emptyList(),
    internal val locationSpots: List<CollectionSpot> = emptyList(),
    internal val isKeywordSearchPartial: Boolean = false,
    internal val keywordSearchThrowable: Throwable? = null,
    internal val locationSearchThrowable: Throwable? = null,
    internal val locationSearchResultProvider: (suspend () -> List<CollectionSpot>)? = null,
) : CollectionSpotRepository, CollectionSpotGeocodingRepository {
    val keywords = mutableListOf<String>()
    var locationSearchCallCount = 0
    var lastLocationCoordinate: Coordinate? = null
    var lastRadiusMeter: Int? = null

    override suspend fun searchRawByKeyword(
        keyword: String,
        types: Set<CollectionSpotType>,
    ): CollectionSpotSearchResult {
        keywords += keyword
        keywordSearchThrowable?.let { throw it }
        return CollectionSpotSearchResult(
            spots = keywordSpots,
            isPartial = isKeywordSearchPartial,
        )
    }

    override suspend fun searchRawByLocation(
        coordinate: Coordinate,
        radiusMeter: Int,
        types: Set<CollectionSpotType>,
    ): List<CollectionSpot> {
        locationSearchCallCount += 1
        lastLocationCoordinate = coordinate
        lastRadiusMeter = radiusMeter
        locationSearchThrowable?.let { throw it }
        locationSearchResultProvider?.let { provider -> return provider() }
        return locationSpots
    }

    override suspend fun geocodeSpot(spot: CollectionSpot): CollectionSpot = spot
}

internal class FakeMapRegionOptionsRepository(
    private val eupmyeondongCandidates: Map<String, List<Region>> = emptyMap(),
    private val legalDongKeywords: Map<String, List<String>> = emptyMap(),
    private val eupmyeondongCandidateProvider: (suspend (String) -> List<Region>)? = null,
) : RegionOptionsRepository {
    val eupmyeondongKeywords = mutableListOf<String>()

    override suspend fun getSidoOptions(): List<String> = emptyList()

    override suspend fun getSigunguOptions(sido: String): List<String> = emptyList()

    override suspend fun getEupmyeondongOptions(
        sido: String,
        sigungu: String,
    ): List<String> = emptyList()

    override suspend fun findRegionsByEupmyeondongKeyword(keyword: String): List<Region> {
        eupmyeondongKeywords += keyword
        eupmyeondongCandidateProvider?.let { provider -> return provider(keyword) }
        return eupmyeondongCandidates[keyword].orEmpty()
    }

    override suspend fun findLegalDongKeywordsByRegion(
        region: Region,
        keyword: String,
    ): List<String> {
        return legalDongKeywords[
            listOf(
                region.sido.orEmpty(),
                region.sigungu.orEmpty(),
                region.eupmyeondong.orEmpty(),
                keyword,
            ).joinToString("|")
        ].orEmpty()
    }

    override suspend fun findRegionsBySigunguKeyword(keyword: String): List<Region> = emptyList()

    override suspend fun normalizeRegionForRegionalGuide(region: Region): Region = region

    override suspend fun findAdminDongCandidatesForLegalDong(region: Region): List<Region> = emptyList()
}

internal fun collectionSpotFavorite(targetId: String): Favorite =
    Favorite(
        type = FavoriteTargetType.COLLECTION_SPOT,
        targetId = targetId,
        savedAtMillis = 1L,
    )

internal class FakeFavoriteRepository(
    initialFavorites: List<Favorite> = emptyList(),
) : FavoriteRepository {
    internal val favorites = MutableStateFlow(initialFavorites)

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
            favorites.value.filterNot { it.type == type && it.targetId == targetId }
    }
}

internal class FakeCollectionSpotFavoriteSnapshotRepository(
    initialSnapshots: List<CollectionSpotFavoriteSnapshot> = emptyList(),
) : CollectionSpotFavoriteSnapshotRepository {
    val snapshots = MutableStateFlow(initialSnapshots)

    override fun observeSnapshots(): Flow<List<CollectionSpotFavoriteSnapshot>> = snapshots

    override suspend fun getSnapshot(targetId: String): CollectionSpotFavoriteSnapshot? =
        snapshots.value.firstOrNull { snapshot -> snapshot.targetId == targetId }

    override suspend fun upsertSnapshot(snapshot: CollectionSpotFavoriteSnapshot) {
        snapshots.value =
            snapshots.value
                .filterNot { it.targetId == snapshot.targetId } + snapshot
    }

    override suspend fun deleteSnapshot(targetId: String) {
        snapshots.value = snapshots.value.filterNot { it.targetId == targetId }
    }
}

internal class FakeCollectionSpotFavoriteRepository(
    internal val favoriteRepository: FakeFavoriteRepository,
    internal val snapshotRepository: FakeCollectionSpotFavoriteSnapshotRepository,
) : CollectionSpotFavoriteRepository {
    override suspend fun toggleFavorite(spot: CollectionSpot): Boolean {
        val isFavorite =
            favoriteRepository.toggleFavorite(
                Favorite(
                    type = FavoriteTargetType.COLLECTION_SPOT,
                    targetId = spot.id,
                    savedAtMillis = TEST_NOW_MILLIS,
                ),
            )

        if (isFavorite) {
            snapshotRepository.upsertSnapshot(spot.toFavoriteSnapshot())
        } else {
            snapshotRepository.deleteSnapshot(spot.id)
        }

        return isFavorite
    }

    override suspend fun removeFavorite(targetId: String) {
        favoriteRepository.removeFavorite(FavoriteTargetType.COLLECTION_SPOT, targetId)
        snapshotRepository.deleteSnapshot(targetId)
    }
}

internal const val TEST_NOW_MILLIS = 20 * 60 * 1_000L
