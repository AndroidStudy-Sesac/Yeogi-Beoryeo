package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveFavoritesUseCase
import com.team.yeogibeoryeo.domain.favorite.usecase.ObserveRegionalGuideFavoriteSnapshotsUseCase
import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.HomeRegionalGuideSummaryBuildResult
import com.team.yeogibeoryeo.domain.regionalguide.model.HomeRegionalGuideSummaryResult
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideLookupResult
import com.team.yeogibeoryeo.domain.regionalguide.repository.HomeRegionalGuidePrimaryFavoriteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.runningFold
import javax.inject.Inject

class ObserveHomeRegionalGuideSummaryUseCase
    @Inject
    constructor(
        private val observeFavoritesUseCase: ObserveFavoritesUseCase,
        private val observeRegionalGuideFavoriteSnapshotsUseCase: ObserveRegionalGuideFavoriteSnapshotsUseCase,
        private val getRegionalDisposalGuideUseCase: GetRegionalDisposalGuideUseCase,
        private val buildHomeRegionalGuideSummaryUseCase: BuildHomeRegionalGuideSummaryUseCase,
        private val selectHomeRegionalGuidePrimaryFavoriteUseCase:
            SelectHomeRegionalGuidePrimaryFavoriteUseCase,
        private val observeHomeRegionalGuidePrimaryFavoriteTargetIdUseCase:
            ObserveHomeRegionalGuidePrimaryFavoriteTargetIdUseCase,
        private val observeHomeRegionalGuideLastSelectedFavoriteTargetIdUseCase:
            ObserveHomeRegionalGuideLastSelectedFavoriteTargetIdUseCase,
        private val homeRegionalGuidePrimaryFavoriteRepository:
            HomeRegionalGuidePrimaryFavoriteRepository,
    ) {
        @OptIn(ExperimentalCoroutinesApi::class)
        operator fun invoke(): Flow<HomeRegionalGuideSummaryResult> =
            combine(
                observeFavoritesUseCase(FavoriteTargetType.REGIONAL_GUIDE),
                observeRegionalGuideFavoriteSnapshotsUseCase(),
                observeHomeRegionalGuidePrimaryFavoriteTargetIdUseCase(),
                observeHomeRegionalGuideLastSelectedFavoriteTargetIdUseCase(),
            ) { favorites, snapshots, pinnedTargetId, lastSelectedTargetId ->
                HomeRegionalGuidePrimaryFavoriteInput(
                    favorites = favorites,
                    snapshots = snapshots,
                    pinnedTargetId = pinnedTargetId,
                    lastSelectedTargetId = lastSelectedTargetId,
                )
            }
                .runningFold(HomeRegionalGuidePrimaryFavoriteSelection()) { previous, input ->
                    val primaryFavorite =
                        selectHomeRegionalGuidePrimaryFavoriteUseCase(
                            favorites = input.favorites,
                            snapshots = input.snapshots,
                            pinnedTargetId = input.pinnedTargetId,
                            previousTargetId = input.lastSelectedTargetId ?: previous.targetId,
                        )
                    val snapshot =
                        input.snapshots.firstOrNull { snapshot ->
                            snapshot.targetId == primaryFavorite?.targetId
                        }
                    val selectedTargetId = primaryFavorite?.targetId
                    when {
                        selectedTargetId == null && input.lastSelectedTargetId != null ->
                            homeRegionalGuidePrimaryFavoriteRepository
                                .clearLastSelectedFavoriteTargetId()

                        selectedTargetId != null && selectedTargetId != input.lastSelectedTargetId ->
                            homeRegionalGuidePrimaryFavoriteRepository
                                .setLastSelectedFavoriteTargetId(selectedTargetId)
                    }

                    HomeRegionalGuidePrimaryFavoriteSelection(
                        favorite = primaryFavorite,
                        snapshot = snapshot,
                    )
                }
                .drop(1)
                .distinctUntilChanged()
                .flatMapLatest { (favorite, snapshot) ->
                    flow {
                        if (favorite == null) {
                            emit(HomeRegionalGuideSummaryResult.NoFavorite)
                            return@flow
                        }

                        if (snapshot == null) {
                            emit(HomeRegionalGuideSummaryResult.FavoriteRestoreFailed(favorite.targetId))
                            return@flow
                        }

                        emit(
                            HomeRegionalGuideSummaryResult.Loading(
                                targetId = snapshot.targetId,
                                regionName = snapshot.region.displayName(),
                            ),
                        )
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
                        favoriteKey = snapshot.key,
                    )
            ) {
                is RegionalGuideLookupResult.Success -> {
                    when (
                        val summaryResult = buildHomeRegionalGuideSummaryUseCase(
                            targetId = snapshot.targetId,
                            regionName = regionName,
                            guide = result.guide,
                        )
                    ) {
                        is HomeRegionalGuideSummaryBuildResult.Summary ->
                            HomeRegionalGuideSummaryResult.Success(summaryResult.summary)

                        HomeRegionalGuideSummaryBuildResult.NoRepresentativeSchedule ->
                            HomeRegionalGuideSummaryResult.NoRepresentativeSchedule(
                                targetId = snapshot.targetId,
                                regionName = regionName,
                            )

                        HomeRegionalGuideSummaryBuildResult.RepresentativeScheduleNeedsConfirmation ->
                            HomeRegionalGuideSummaryResult.RepresentativeScheduleNeedsConfirmation(
                                targetId = snapshot.targetId,
                                regionName = regionName,
                            )
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

        private fun Region.displayName(): String =
            listOfNotNull(
                sido?.trimToNull(),
                sigungu?.trimToNull(),
                eupmyeondong?.trimToNull(),
            ).joinToString(" > ")

        private fun String?.trimToNull(): String? = this?.trim()?.takeIf { it.isNotBlank() }

        private data class HomeRegionalGuidePrimaryFavoriteInput(
            val favorites: List<Favorite>,
            val snapshots: List<RegionalGuideFavoriteSnapshot>,
            val pinnedTargetId: String?,
            val lastSelectedTargetId: String?,
        )

        private data class HomeRegionalGuidePrimaryFavoriteSelection(
            val favorite: Favorite? = null,
            val snapshot: RegionalGuideFavoriteSnapshot? = null,
        ) {
            val targetId: String?
                get() = favorite?.targetId
        }
    }
