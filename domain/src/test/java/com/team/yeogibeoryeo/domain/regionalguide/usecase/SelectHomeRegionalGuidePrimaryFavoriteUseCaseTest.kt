package com.team.yeogibeoryeo.domain.regionalguide.usecase

import com.team.yeogibeoryeo.domain.favorite.model.Favorite
import com.team.yeogibeoryeo.domain.favorite.model.FavoriteTargetType
import com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteSnapshot
import com.team.yeogibeoryeo.domain.region.model.Region
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectHomeRegionalGuidePrimaryFavoriteUseCaseTest {
    private val useCase = SelectHomeRegionalGuidePrimaryFavoriteUseCase()

    @Test
    fun `기존 대표 지역이 즐겨찾기에 남아 있으면 대표 지역을 유지한다`() {
        val favorites =
            listOf(
                favorite(targetId = "old", savedAtMillis = 1L),
                favorite(targetId = "latest", savedAtMillis = 2L),
            )

        val result =
            useCase(
                favorites = favorites,
                snapshots = snapshots("old", "latest"),
                pinnedTargetId = null,
                previousTargetId = "old",
            )

        assertEquals("old", result?.targetId)
    }

    @Test
    fun `기존 대표 지역이 삭제되면 남은 즐겨찾기 중 최신 저장 항목을 선택한다`() {
        val favorites =
            listOf(
                favorite(targetId = "older", savedAtMillis = 1L),
                favorite(targetId = "newer", savedAtMillis = 2L),
            )

        val result =
            useCase(
                favorites = favorites,
                snapshots = snapshots("older", "newer"),
                pinnedTargetId = null,
                previousTargetId = "deleted",
            )

        assertEquals("newer", result?.targetId)
    }

    @Test
    fun `대표가 아닌 즐겨찾기가 추가되어도 기존 대표 지역을 유지한다`() {
        val favorites =
            listOf(
                favorite(targetId = "primary", savedAtMillis = 1L),
                favorite(targetId = "added", savedAtMillis = 3L),
                favorite(targetId = "other", savedAtMillis = 2L),
            )

        val result =
            useCase(
                favorites = favorites,
                snapshots = snapshots("primary", "added", "other"),
                pinnedTargetId = null,
                previousTargetId = "primary",
            )

        assertEquals("primary", result?.targetId)
    }

    @Test
    fun `마지막 즐겨찾기가 삭제되면 대표 지역을 선택하지 않는다`() {
        val result =
            useCase(
                favorites = emptyList(),
                snapshots = emptyList(),
                pinnedTargetId = null,
                previousTargetId = "deleted",
            )

        assertNull(result)
    }

    @Test
    fun `즐겨찾기 순서가 달라도 대표 선택 결과가 동일하다`() {
        val firstOrder =
            listOf(
                favorite(targetId = "b", savedAtMillis = 3L),
                favorite(targetId = "a", savedAtMillis = 3L),
                favorite(targetId = "c", savedAtMillis = 1L),
            )
        val secondOrder = firstOrder.reversed()

        val firstResult =
            useCase(
                favorites = firstOrder,
                snapshots = snapshots("a", "b", "c"),
                pinnedTargetId = null,
                previousTargetId = null,
            )
        val secondResult =
            useCase(
                favorites = secondOrder,
                snapshots = snapshots("a", "b", "c"),
                pinnedTargetId = null,
                previousTargetId = null,
            )

        assertEquals("a", firstResult?.targetId)
        assertEquals(firstResult, secondResult)
    }

    @Test
    fun `저장된 대표 지역의 스냅샷이 유효하지 않으면 복원 가능한 즐겨찾기에서 새 대표 지역을 선택한다`() {
        val favorites =
            listOf(
                favorite(targetId = "missing-snapshot", savedAtMillis = 3L),
                favorite(targetId = "restorable", savedAtMillis = 2L),
            )

        val result =
            useCase(
                favorites = favorites,
                snapshots = snapshots("restorable"),
                pinnedTargetId = null,
                previousTargetId = "missing-snapshot",
            )

        assertEquals("restorable", result?.targetId)
    }

    @Test
    fun `복원 가능한 스냅샷이 없으면 실패 상태를 만들 수 있도록 즐겨찾기를 반환한다`() {
        val result =
            useCase(
                favorites = listOf(favorite(targetId = "missing-snapshot", savedAtMillis = 1L)),
                snapshots = emptyList(),
                pinnedTargetId = null,
                previousTargetId = null,
            )

        assertEquals("missing-snapshot", result?.targetId)
    }

    @Test
    fun `직접 고정한 지역이 유효하면 기존 대표 지역보다 우선한다`() {
        val favorites =
            listOf(
                favorite(targetId = "previous", savedAtMillis = 1L),
                favorite(targetId = "pinned", savedAtMillis = 2L),
            )

        val result =
            useCase(
                favorites = favorites,
                snapshots = snapshots("previous", "pinned"),
                pinnedTargetId = "pinned",
                previousTargetId = "previous",
            )

        assertEquals("pinned", result?.targetId)
    }

    @Test
    fun `직접 고정한 지역이 유효하지 않으면 기존 대표 지역을 유지한다`() {
        val favorites =
            listOf(
                favorite(targetId = "previous", savedAtMillis = 1L),
                favorite(targetId = "other", savedAtMillis = 2L),
            )

        val result =
            useCase(
                favorites = favorites,
                snapshots = snapshots("previous", "other"),
                pinnedTargetId = "deleted",
                previousTargetId = "previous",
            )

        assertEquals("previous", result?.targetId)
    }

    private fun snapshots(vararg targetIds: String): List<RegionalGuideFavoriteSnapshot> =
        targetIds.map { targetId ->
            RegionalGuideFavoriteSnapshot(
                targetId = targetId,
                region = Region(sido = "Sido", sigungu = "Sigungu", eupmyeondong = targetId),
                targetRegionName = null,
                managementZoneName = null,
            )
        }

    private fun favorite(
        targetId: String,
        savedAtMillis: Long,
    ): Favorite =
        Favorite(
            type = FavoriteTargetType.REGIONAL_GUIDE,
            targetId = targetId,
            savedAtMillis = savedAtMillis,
        )
}
