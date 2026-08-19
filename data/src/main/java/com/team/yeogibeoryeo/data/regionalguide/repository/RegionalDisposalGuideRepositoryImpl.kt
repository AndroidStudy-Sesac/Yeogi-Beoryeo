package com.team.yeogibeoryeo.data.regionalguide.repository

import com.team.yeogibeoryeo.data.regionalguide.di.RegionalGuideFetchScope
import com.team.yeogibeoryeo.data.regionalguide.mapper.RegionalGuideMapper
import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideDataSource
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [RegionalDisposalGuideRepository]의 Data 계층 구현체.
 * RemoteDataSource를 통해 데이터를 패치하고 DTO를 domain 후보 목록으로 변환합니다.
 */
class RegionalDisposalGuideRepositoryImpl @Inject constructor(
    private val remoteDataSource: RegionalGuideDataSource,
    @param:RegionalGuideFetchScope private val fetchScope: CoroutineScope,
) : RegionalDisposalGuideRepository {

    private val cacheMutex = Mutex()
    private var recentCandidatesCache: CachedRegionalGuideItems? = null
    private val inFlightRequests = mutableMapOf<String, Deferred<Result<List<RegionalGuideItemDto>>>>()

    override suspend fun getRegionalDisposalGuideCandidates(
        query: RegionalGuideQuery
    ): Result<List<RegionalDisposalGuide>> {
        return fetchRegionalGuideItems(query.sigunguQuery)
            .map { dtoList ->
                dtoList.map { dto ->
                    RegionalGuideMapper.mapToDomain(
                        baseRegion = query.displayRegion.copy(
                            sido = null,
                            sigungu = null
                        ),
                        dto = dto
                    )
                }
            }
    }

    private suspend fun fetchRegionalGuideItems(
        sigunguQuery: String,
    ): Result<List<RegionalGuideItemDto>> {
        val request = cacheMutex.withLock {
            recentCandidatesCache
                ?.takeIf { cache -> cache.sigunguQuery == sigunguQuery }
                ?.let { cache -> return Result.success(cache.items) }

            inFlightRequests[sigunguQuery]
                ?: createInFlightRequest(sigunguQuery).also { request ->
                    inFlightRequests[sigunguQuery] = request
                }
        }

        request.start()
        return request.await()
    }

    private fun createInFlightRequest(
        sigunguQuery: String,
    ): Deferred<Result<List<RegionalGuideItemDto>>> = fetchScope.async(start = LAZY) {
        fetchAndCacheRegionalGuideItems(sigunguQuery)
    }

    private suspend fun fetchAndCacheRegionalGuideItems(
        sigunguQuery: String,
    ): Result<List<RegionalGuideItemDto>> {
        val requestJob = currentCoroutineContext()[Job]

        return try {
            remoteDataSource.fetchRegionalGuides(sigunguQuery)
                .onSuccess { result ->
                    if (!result.isPartial) {
                        cacheMutex.withLock {
                            recentCandidatesCache = CachedRegionalGuideItems(
                                sigunguQuery = sigunguQuery,
                                items = result.items,
                            )
                        }
                    }
                }
                .map { result -> result.items }
        } finally {
            withContext(NonCancellable) {
                cacheMutex.withLock {
                    if (inFlightRequests[sigunguQuery] === requestJob) {
                        inFlightRequests -= sigunguQuery
                    }
                }
            }
        }
    }

    private data class CachedRegionalGuideItems(
        val sigunguQuery: String,
        val items: List<RegionalGuideItemDto>,
    )
}
