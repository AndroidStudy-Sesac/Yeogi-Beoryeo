package com.team.yeogibeoryeo.data.regionalguide.repository

import com.team.yeogibeoryeo.data.regionalguide.mapper.RegionalGuideMapper
import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideDataSource
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideQuery
import com.team.yeogibeoryeo.domain.regionalguide.repository.RegionalDisposalGuideRepository
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * [RegionalDisposalGuideRepository]의 Data 계층 구현체.
 * RemoteDataSource를 통해 데이터를 패치하고 DTO를 domain 후보 목록으로 변환합니다.
 */
class RegionalDisposalGuideRepositoryImpl @Inject constructor(
    private val remoteDataSource: RegionalGuideDataSource
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
    ): Result<List<RegionalGuideItemDto>> = coroutineScope {
        val request = cacheMutex.withLock {
            recentCandidatesCache
                ?.takeIf { cache -> cache.sigunguQuery == sigunguQuery }
                ?.let { cache -> return@coroutineScope Result.success(cache.items) }

            inFlightRequests[sigunguQuery]
                ?: async(start = LAZY) {
                    fetchAndCacheRegionalGuideItems(sigunguQuery)
                }.also { request ->
                    inFlightRequests[sigunguQuery] = request
                }
        }

        request.await()
    }

    private suspend fun fetchAndCacheRegionalGuideItems(
        sigunguQuery: String,
    ): Result<List<RegionalGuideItemDto>> = try {
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
        cacheMutex.withLock {
            inFlightRequests -= sigunguQuery
        }
    }

    private data class CachedRegionalGuideItems(
        val sigunguQuery: String,
        val items: List<RegionalGuideItemDto>,
    )
}
