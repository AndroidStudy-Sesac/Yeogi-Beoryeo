package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveRegionalGuideFavoriteSnapshotsUseCase
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.HomeRegionalGuideSummaryResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class ObserveHomeRegionalGuideSummaryUseCase
    @Inject
    constructor(
        private val observeFavoritesUseCase: ObserveFavoritesUseCase,
        private val observeRegionalGuideFavoriteSnapshotsUseCase: ObserveRegionalGuideFavoriteSnapshotsUseCase,
        private val getRegionalDisposalGuideUseCase: GetRegionalDisposalGuideUseCase,
        private val getTodayRegionalWasteSummaryUseCase: GetTodayRegionalWasteSummaryUseCase,
    ) {
        @OptIn(ExperimentalCoroutinesApi::class)
        operator fun invoke(): Flow<HomeRegionalGuideSummaryResult> =
            combine(
                observeFavoritesUseCase(FavoriteTargetType.REGIONAL_GUIDE),
                observeRegionalGuideFavoriteSnapshotsUseCase(),
            ) { favorites, snapshots ->
                favorites.latestRegionalGuideFavorite() to snapshots.associateBy { it.targetId }
            }
                .flatMapLatest { (favorite, snapshotsByTargetId) ->
                    flow {
                        if (favorite == null) {
                            emit(HomeRegionalGuideSummaryResult.NoFavorite)
                            return@flow
                        }

                        val snapshot = snapshotsByTargetId[favorite.targetId]
                        if (snapshot == null) {
                            emit(HomeRegionalGuideSummaryResult.FavoriteRestoreFailed(favorite.targetId))
                            return@flow
                        }

                        emit(HomeRegionalGuideSummaryResult.Loading)
                        emit(loadSummary(snapshot))
                    }
                }

        private suspend fun loadSummary(
            snapshot: RegionalGuideFavoriteSnapshot,
        ): HomeRegionalGuideSummaryResult {
            val regionName = snapshot.region.displayName()
            return when (
                val result =
                    getRegionalDisposalGuideUseCase(
                        region = snapshot.region,
                        preferredTargetRegionName = snapshot.targetRegionName,
                        preferredManagementZoneName = snapshot.managementZoneName,
                    )
            ) {
                is RegionalGuideLookupResult.Success -> {
                    val summary =
                        getTodayRegionalWasteSummaryUseCase(
                            targetId = snapshot.targetId,
                            regionName = regionName,
                            guide = result.guide,
                        )

                    if (summary == null) {
                        HomeRegionalGuideSummaryResult.NoTodaySchedule(
                            targetId = snapshot.targetId,
                            regionName = regionName,
                        )
                    } else {
                        HomeRegionalGuideSummaryResult.Success(summary)
                    }
                }

                is RegionalGuideLookupResult.Failure ->
                    HomeRegionalGuideSummaryResult.Failure(
                        targetId = snapshot.targetId,
                        regionName = regionName,
                        reason = result.reason,
                    )

                is RegionalGuideLookupResult.Candidates,
                RegionalGuideLookupResult.CandidateNotFound,
                RegionalGuideLookupResult.NotFound,
                ->
                    HomeRegionalGuideSummaryResult.FavoriteRestoreFailed(snapshot.targetId)
            }
        }

        private fun List<Favorite>.latestRegionalGuideFavorite(): Favorite? =
            maxByOrNull { favorite -> favorite.savedAtMillis }

        private fun Region.displayName(): String =
            listOfNotNull(
                sido?.trimToNull(),
                sigungu?.trimToNull(),
                eupmyeondong?.trimToNull(),
            ).joinToString(" > ")

        private fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotBlank() }
    }
