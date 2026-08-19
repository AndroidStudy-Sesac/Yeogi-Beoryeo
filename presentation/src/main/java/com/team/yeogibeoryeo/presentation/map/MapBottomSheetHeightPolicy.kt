package com.team.yeogibeoryeo.presentation.map

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.spot.model.MapRegionSearchCandidate
import com.team.yeogibeoryeo.presentation.map.components.MapResultBottomSheetPeekHeight
import com.team.yeogibeoryeo.presentation.map.components.MapSpotDetailBottomSheetPeekHeight

internal object MapBottomSheetHeightPolicy {
    fun maxExpandedHeight(
        mapUiMode: MapUiMode,
        hasRegionSelection: Boolean,
        hasStateMessageContent: Boolean,
        maxHeight: Dp,
        bottomContentPadding: Dp,
        regionCandidateCount: Int,
        regionDetailCandidate: MapRegionSearchCandidate?,
        canNavigateBackToRegionCandidates: Boolean,
        fontScale: Float,
    ): Dp? {
        return when {
            mapUiMode == MapUiMode.SpotDetail -> null
            hasRegionSelection -> regionSelectionContentFitHeight(
                maxHeight = maxHeight,
                bottomContentPadding = bottomContentPadding,
                candidateCount = regionCandidateCount,
                detailCandidate = regionDetailCandidate,
                canNavigateBackToRegionCandidates = canNavigateBackToRegionCandidates,
                fontScale = fontScale,
            )
            hasStateMessageContent -> stateMessageContentFitHeight(
                maxHeight = maxHeight,
                bottomContentPadding = bottomContentPadding,
                fontScale = fontScale,
            )
            else -> null
        }
    }

    fun mediumVisibleHeight(
        hasStateMessageContent: Boolean,
        maxHeight: Dp,
        bottomContentPadding: Dp,
        fontScale: Float,
    ): Dp {
        return if (hasStateMessageContent) {
            stateMessageContentFitHeight(
                maxHeight = maxHeight,
                bottomContentPadding = bottomContentPadding,
                fontScale = fontScale,
            )
        } else {
            MapSpotDetailBottomSheetPeekHeight
        }
    }

    private fun regionSelectionContentFitHeight(
        maxHeight: Dp,
        bottomContentPadding: Dp,
        candidateCount: Int,
        detailCandidate: MapRegionSearchCandidate?,
        canNavigateBackToRegionCandidates: Boolean,
        fontScale: Float,
    ): Dp {
        val heightScale = fontScale.coerceIn(1f, MAX_HEIGHT_FONT_SCALE)
        val contentHeight = if (detailCandidate == null) {
            HeaderEstimatedHeight +
                RegionSelectionDescriptionEstimatedHeight * heightScale +
                RegionSelectionRowEstimatedHeight * candidateCount.toFloat() * heightScale +
                bottomContentPadding +
                RegionSelectionBottomExtraPadding
        } else {
            val detailKeywordCount = detailCandidate.searchKeywords
                .filterNot { keyword -> keyword == detailCandidate.searchKeyword }
                .distinct()
                .size
            val backButtonHeight = if (canNavigateBackToRegionCandidates) {
                RegionDetailBackButtonEstimatedHeight
            } else {
                0.dp
            }

            HeaderEstimatedHeight +
                backButtonHeight * heightScale +
                RegionDetailDescriptionEstimatedHeight * heightScale +
                RegionSelectionRowEstimatedHeight * detailKeywordCount.toFloat() * heightScale +
                RegionDetailAllRowEstimatedHeight * heightScale +
                bottomContentPadding +
                RegionSelectionBottomExtraPadding
        }
        val maxContentFitHeight = maxHeight * REGION_SELECTION_MAX_EXPANDED_RATIO

        return contentHeight
            .coerceAtLeast(MapResultBottomSheetPeekHeight)
            .coerceAtMost(maxContentFitHeight)
    }

    private fun stateMessageContentFitHeight(
        maxHeight: Dp,
        bottomContentPadding: Dp,
        fontScale: Float,
    ): Dp {
        val fontScaleProgress = ((fontScale - 1f) / (MAX_HEIGHT_FONT_SCALE - 1f))
            .coerceIn(0f, 1f)
        val maxExpandedRatio = STATE_MESSAGE_BASE_MAX_EXPANDED_RATIO +
            (STATE_MESSAGE_LARGE_FONT_MAX_EXPANDED_RATIO - STATE_MESSAGE_BASE_MAX_EXPANDED_RATIO) *
            fontScaleProgress
        val contentHeight = StateMessageBaseExpandedHeight +
            StateMessageLargeFontExtraExpandedHeight * fontScaleProgress +
            bottomContentPadding

        return contentHeight
            .coerceAtLeast(MapResultBottomSheetPeekHeight)
            .coerceAtMost(maxHeight * maxExpandedRatio)
    }

    private const val REGION_SELECTION_MAX_EXPANDED_RATIO = 0.88f
    private val HeaderEstimatedHeight = 57.dp
    private val RegionSelectionDescriptionEstimatedHeight = 92.dp
    private val RegionDetailDescriptionEstimatedHeight = 150.dp
    private val RegionDetailBackButtonEstimatedHeight = 60.dp
    private val RegionSelectionRowEstimatedHeight = 68.dp
    private val RegionDetailAllRowEstimatedHeight = 92.dp
    private val RegionSelectionBottomExtraPadding = 24.dp
    private const val MAX_HEIGHT_FONT_SCALE = 2f
    private const val STATE_MESSAGE_BASE_MAX_EXPANDED_RATIO = 0.52f
    private const val STATE_MESSAGE_LARGE_FONT_MAX_EXPANDED_RATIO = 0.72f
    private val StateMessageBaseExpandedHeight = 360.dp
    private val StateMessageLargeFontExtraExpandedHeight = 160.dp
}
