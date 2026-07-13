package com.team.yeogibeoryeo.domain.spot.usecase

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry
import com.team.yeogibeoryeo.domain.spot.repository.RecentCurrentLocationSpotCacheRepository
import com.team.yeogibeoryeo.domain.time.TimeProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecentCurrentLocationSpotCacheUseCasesTest {

    @Test
    fun `GetFreshRecentCurrentLocationSpotsUseCase는 TTL 이내 캐시를 반환한다`() =
        runBlocking {
            val cachedSpots = listOf(sampleSpot("cache"))
            val repository = FakeRecentCurrentLocationSpotCacheRepository(
                entry = RecentCurrentLocationSpotCacheEntry(
                    spots = cachedSpots,
                    savedAtMillis = TEST_NOW_MILLIS - FIVE_MINUTES_MILLIS,
                ),
            )
            val useCase = GetFreshRecentCurrentLocationSpotsUseCase(
                repository = repository,
                timeProvider = FakeTimeProvider(TEST_NOW_MILLIS),
            )

            val result = useCase()

            assertEquals(cachedSpots, result?.spots)
        }

    @Test
    fun `GetFreshRecentCurrentLocationSpotsUseCase는 만료된 캐시를 반환하지 않는다`() =
        runBlocking {
            val repository = FakeRecentCurrentLocationSpotCacheRepository(
                entry = RecentCurrentLocationSpotCacheEntry(
                    spots = listOf(sampleSpot("expired")),
                    savedAtMillis = TEST_NOW_MILLIS - ELEVEN_MINUTES_MILLIS,
                ),
            )
            val useCase = GetFreshRecentCurrentLocationSpotsUseCase(
                repository = repository,
                timeProvider = FakeTimeProvider(TEST_NOW_MILLIS),
            )

            val result = useCase()

            assertNull(result)
            assertNull(repository.entry)
            assertEquals(1, repository.clearCallCount)
        }

    @Test
    fun `GetFreshRecentCurrentLocationSpotsUseCase는 미래 시각 캐시를 반환하지 않는다`() =
        runBlocking {
            val repository = FakeRecentCurrentLocationSpotCacheRepository(
                entry = RecentCurrentLocationSpotCacheEntry(
                    spots = listOf(sampleSpot("future")),
                    savedAtMillis = TEST_NOW_MILLIS + 1L,
                ),
            )
            val useCase = GetFreshRecentCurrentLocationSpotsUseCase(
                repository = repository,
                timeProvider = FakeTimeProvider(TEST_NOW_MILLIS),
            )

            val result = useCase()

            assertNull(result)
            assertNull(repository.entry)
            assertEquals(1, repository.clearCallCount)
        }

    @Test
    fun `SaveRecentCurrentLocationSpotsUseCase는 TimeProvider 기준 시각으로 캐시를 저장한다`() =
        runBlocking {
            val spots = listOf(sampleSpot("saved"))
            val repository = FakeRecentCurrentLocationSpotCacheRepository()
            val useCase = SaveRecentCurrentLocationSpotsUseCase(
                repository = repository,
                timeProvider = FakeTimeProvider(TEST_NOW_MILLIS),
            )

            useCase(spots)

            assertEquals(spots, repository.entry?.spots)
            assertEquals(TEST_NOW_MILLIS, repository.entry?.savedAtMillis)
        }

    @Test
    fun `ClearRecentCurrentLocationSpotsUseCase는 캐시를 삭제한다`() =
        runBlocking {
            val repository = FakeRecentCurrentLocationSpotCacheRepository(
                entry = RecentCurrentLocationSpotCacheEntry(
                    spots = listOf(sampleSpot("cached")),
                    savedAtMillis = TEST_NOW_MILLIS,
                ),
            )
            val useCase = ClearRecentCurrentLocationSpotsUseCase(repository)

            useCase()

            assertNull(repository.entry)
            assertEquals(1, repository.clearCallCount)
        }

    private fun sampleSpot(id: String): CollectionSpot {
        return CollectionSpot(
            id = id,
            name = "수거 장소 $id",
            type = CollectionSpotType.STANDARD_BAG_STORE,
            address = "서울특별시 영등포구",
            detailLocation = null,
            coordinate = null,
            distanceMeter = null,
            isBookmarked = false,
        )
    }

    private class FakeRecentCurrentLocationSpotCacheRepository(
        var entry: RecentCurrentLocationSpotCacheEntry? = null,
    ) : RecentCurrentLocationSpotCacheRepository {
        var clearCallCount = 0
            private set

        override suspend fun getRecentCurrentLocationSpots(): RecentCurrentLocationSpotCacheEntry? {
            return entry
        }

        override suspend fun saveRecentCurrentLocationSpots(entry: RecentCurrentLocationSpotCacheEntry) {
            this.entry = entry
        }

        override suspend fun clearRecentCurrentLocationSpots() {
            clearCallCount += 1
            entry = null
        }
    }

    private class FakeTimeProvider(
        private val nowMillis: Long,
    ) : TimeProvider {
        override fun currentTimeMillis(): Long = nowMillis
    }

    private companion object {
        const val TEST_NOW_MILLIS = 20 * 60 * 1_000L
        const val FIVE_MINUTES_MILLIS = 5 * 60 * 1_000L
        const val ELEVEN_MINUTES_MILLIS = 11 * 60 * 1_000L
    }
}
