package com.team.yeogibeoryeo.presentation.map

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.repository.CollectionSpotFavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.repository.FavoriteRepository
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ToggleCollectionSpotFavoriteUseCase
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import com.team.yeogibeoryeo.domain.spot.repository.RecentCurrentLocationSpotCacheRepository
import com.team.yeogibeoryeo.domain.spot.usecase.CalculateDistanceMeterUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.FilterCollectionSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.GetFreshRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SaveRecentCurrentLocationSpotsUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByKeywordUseCase
import com.team.yeogibeoryeo.domain.spot.usecase.SearchCollectionSpotsByLocationUseCase
import com.team.yeogibeoryeo.domain.time.TimeProvider
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationProvider
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationResult
import com.team.yeogibeoryeo.presentation.map.location.LocationPermissionChecker
import com.team.yeogibeoryeo.presentation.search.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class CollectionSpotMapInitialEntryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `초기 타입으로 지도 진입 시 해당 타입을 선택하고 현재 위치 검색 결과를 필터링한다`() =
        runTest {
            val currentCoordinate = Coordinate(latitude = 37.5666102, longitude = 126.9783881)
            val medicineSpot = sampleSpot("medicine", CollectionSpotType.MEDICINE_DROP_BOX)
            val batterySpot = sampleSpot("battery", CollectionSpotType.BATTERY_BIN)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(medicineSpot, batterySpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(currentCoordinate),
                hasFineLocationPermission = true,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(CollectionSpotType.MEDICINE_DROP_BOX)

            assertEquals(setOf(CollectionSpotType.MEDICINE_DROP_BOX), viewModel.uiState.value.selectedTypes)
            assertEquals(listOf(medicineSpot).withDistanceFrom(currentCoordinate), viewModel.uiState.value.spots)
            assertEquals(1, repository.locationSearchCallCount)
            assertEquals(currentCoordinate, repository.lastLocationCoordinate)
            assertEquals(MapSearchMode.CURRENT_LOCATION, viewModel.uiState.value.searchMode)
        }

    @Test
    fun `초기 타입으로 지도 진입 시 위치 권한이 없으면 타입만 선택하고 자동 권한 요청 상태를 만들지 않는다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = false,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(CollectionSpotType.FLUORESCENT_LAMP_BIN)

            assertEquals(setOf(CollectionSpotType.FLUORESCENT_LAMP_BIN), viewModel.uiState.value.selectedTypes)
            assertEquals(0, repository.locationSearchCallCount)
            assertFalse(viewModel.uiState.value.hasSearched)
            assertNull(viewModel.uiState.value.locationNotice)
            assertNull(viewModel.uiState.value.locationNoticeMessage)
            assertNull(viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `초기 타입 지도 진입은 자동 검색은 한 번만 실행하고 타입 선택은 새 진입 값을 반영한다`() =
        runTest {
            val medicineSpot = sampleSpot("medicine", CollectionSpotType.MEDICINE_DROP_BOX)
            val repository = FakeCollectionSpotRepository(locationSpots = listOf(medicineSpot))
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = true,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(CollectionSpotType.MEDICINE_DROP_BOX)
            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(CollectionSpotType.BATTERY_BIN)

            assertEquals(setOf(CollectionSpotType.BATTERY_BIN), viewModel.uiState.value.selectedTypes)
            assertEquals(emptyList<CollectionSpot>(), viewModel.uiState.value.spots)
            assertEquals(1, repository.locationSearchCallCount)
        }

    @Test
    fun `초기 타입으로 다시 지도 진입하면 기존 검색 상태가 있어도 타입 선택을 반영한다`() =
        runTest {
            val medicineSpot = sampleSpot("medicine", CollectionSpotType.MEDICINE_DROP_BOX)
            val batterySpot = sampleSpot("battery", CollectionSpotType.BATTERY_BIN)
            val repository = FakeCollectionSpotRepository(
                locationSpots = listOf(medicineSpot, batterySpot),
            )
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = true,
            )

            viewModel.searchByCurrentLocationOnMapEntryIfPermitted()
            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(CollectionSpotType.BATTERY_BIN)

            assertEquals(setOf(CollectionSpotType.BATTERY_BIN), viewModel.uiState.value.selectedTypes)
            assertEquals(
                listOf(batterySpot).withDistanceFrom(Coordinate(latitude = 37.5666102, longitude = 126.9783881)),
                viewModel.uiState.value.spots,
            )
            assertEquals(1, repository.locationSearchCallCount)
        }

    @Test
    fun `검색어가 입력된 상태에서 초기 타입으로 지도 진입하면 자동 검색 없이 타입 선택만 반영한다`() =
        runTest {
            val repository = FakeCollectionSpotRepository()
            val viewModel = createViewModel(
                repository = repository,
                currentLocationResult = CurrentLocationResult.Found(
                    Coordinate(latitude = 37.5666102, longitude = 126.9783881),
                ),
                hasFineLocationPermission = true,
            )

            viewModel.onSearchKeywordChanged("문래동")
            viewModel.searchByCurrentLocationOnMapEntryIfPermitted(CollectionSpotType.CLOTHING_BIN)

            assertEquals(setOf(CollectionSpotType.CLOTHING_BIN), viewModel.uiState.value.selectedTypes)
            assertEquals("문래동", viewModel.uiState.value.searchKeyword)
            assertEquals(0, repository.locationSearchCallCount)
            assertFalse(viewModel.uiState.value.hasSearched)
        }

    private fun createViewModel(
        repository: FakeCollectionSpotRepository,
        currentLocationResult: CurrentLocationResult,
        hasFineLocationPermission: Boolean = true,
    ): CollectionSpotMapViewModel {
        val cacheRepository = FakeRecentCurrentLocationSpotCacheRepository()
        val favoriteRepository = FakeFavoriteRepository()
        return CollectionSpotMapViewModel(
            searchCollectionSpotsByKeywordUseCase = SearchCollectionSpotsByKeywordUseCase(repository),
            searchCollectionSpotsByLocationUseCase = SearchCollectionSpotsByLocationUseCase(repository),
            filterCollectionSpotsUseCase = FilterCollectionSpotsUseCase(),
            currentLocationProvider = FakeCurrentLocationProvider(currentLocationResult),
            locationPermissionChecker = FakeLocationPermissionChecker(hasFineLocationPermission),
            getFreshRecentCurrentLocationSpotsUseCase = GetFreshRecentCurrentLocationSpotsUseCase(
                repository = cacheRepository,
                timeProvider = FakeTimeProvider(),
            ),
            saveRecentCurrentLocationSpotsUseCase = SaveRecentCurrentLocationSpotsUseCase(
                repository = cacheRepository,
                timeProvider = FakeTimeProvider(),
            ),
            observeFavoritesUseCase = ObserveFavoritesUseCase(favoriteRepository),
            toggleCollectionSpotFavoriteUseCase = ToggleCollectionSpotFavoriteUseCase(
                collectionSpotFavoriteRepository = FakeCollectionSpotFavoriteRepository(favoriteRepository),
            ),
            calculateDistanceMeterUseCase = CalculateDistanceMeterUseCase(),
        )
    }

    private fun sampleSpot(
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

    private fun List<CollectionSpot>.withDistanceFrom(
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

    private class FakeCurrentLocationProvider(
        private val result: CurrentLocationResult,
    ) : CurrentLocationProvider {
        override suspend fun getCurrentLocation(): CurrentLocationResult = result
    }

    private class FakeLocationPermissionChecker(
        private val hasFineLocationPermission: Boolean,
    ) : LocationPermissionChecker {
        override fun hasFineLocationPermission(): Boolean = hasFineLocationPermission
    }

    private class FakeTimeProvider : TimeProvider {
        override fun currentTimeMillis(): Long = TEST_NOW_MILLIS
    }

    private class FakeRecentCurrentLocationSpotCacheRepository :
        RecentCurrentLocationSpotCacheRepository {
        override suspend fun getRecentCurrentLocationSpots(): RecentCurrentLocationSpotCacheEntry? = null

        override suspend fun saveRecentCurrentLocationSpots(entry: RecentCurrentLocationSpotCacheEntry) = Unit

        override suspend fun clearRecentCurrentLocationSpots() = Unit
    }

    private class FakeCollectionSpotRepository(
        private val locationSpots: List<CollectionSpot> = emptyList(),
    ) : CollectionSpotRepository {
        var locationSearchCallCount = 0
            private set
        var lastLocationCoordinate: Coordinate? = null
            private set

        override suspend fun searchByKeyword(
            keyword: String,
            types: Set<CollectionSpotType>,
        ): List<CollectionSpot> = emptyList()

        override suspend fun searchByLocation(
            coordinate: Coordinate,
            radiusMeter: Int,
            types: Set<CollectionSpotType>,
        ): List<CollectionSpot> {
            locationSearchCallCount += 1
            lastLocationCoordinate = coordinate
            return locationSpots
        }

        override suspend fun geocodeSpot(spot: CollectionSpot): CollectionSpot = spot
    }

    private class FakeFavoriteRepository : FavoriteRepository {
        private val favorites = MutableStateFlow(emptyList<Favorite>())

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
        ): Boolean = false

        override suspend fun toggleFavorite(favorite: Favorite): Boolean = false

        override suspend fun addFavorite(favorite: Favorite) = Unit

        override suspend fun removeFavorite(
            type: FavoriteTargetType,
            targetId: String,
        ) = Unit
    }

    private class FakeCollectionSpotFavoriteRepository(
        private val favoriteRepository: FakeFavoriteRepository,
    ) : CollectionSpotFavoriteRepository {
        override suspend fun toggleFavorite(spot: CollectionSpot): Boolean =
            favoriteRepository.toggleFavorite(
                Favorite(
                    type = FavoriteTargetType.COLLECTION_SPOT,
                    targetId = spot.id,
                    savedAtMillis = TEST_NOW_MILLIS,
                ),
            )

        override suspend fun removeFavorite(targetId: String) {
            favoriteRepository.removeFavorite(FavoriteTargetType.COLLECTION_SPOT, targetId)
        }
    }

    private companion object {
        const val TEST_NOW_MILLIS = 20 * 60 * 1_000L
    }
}
