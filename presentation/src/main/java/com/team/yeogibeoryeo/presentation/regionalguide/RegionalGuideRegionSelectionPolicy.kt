package com.team.yeogibeoryeo.presentation.regionalguide

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalDisposalGuide
import com.team.yeogibeoryeo.domain.regionalguide.model.RegionalGuideEupmyeondongNamePolicy

/**
 * 지역 가이드 조회 결과를 지역 선택 UI에 반영할 때 필요한 순수 판단을 모은 정책이다.
 *
 * 조회에는 정규화된 원본 지역을 사용하고, 선택 UI에는 실제 선택지에 존재하는 읍면동만
 * 반영한다. 이 구분으로 선택지 준비가 비동기로 완료되어도 조회 조건이 바뀌지 않는다.
 */
internal object RegionalGuideRegionSelectionPolicy {

    fun prepare(
        lookupRegion: Region,
        eupmyeondongOptions: List<String>?,
    ): PreparedRegionalGuideRegionSelection {
        val selectedEupmyeondong = lookupRegion.eupmyeondong
        val canSelectEupmyeondong =
            selectedEupmyeondong != null &&
                !eupmyeondongOptions.isNullOrEmpty() &&
                eupmyeondongOptions.any { option ->
                    RegionalGuideEupmyeondongNamePolicy.isSameName(
                        first = selectedEupmyeondong,
                        second = option,
                    )
                }

        val selectorRegion =
            if (
                selectedEupmyeondong != null &&
                    !eupmyeondongOptions.isNullOrEmpty() &&
                    !canSelectEupmyeondong
            ) {
                lookupRegion.copy(eupmyeondong = null)
            } else {
                lookupRegion
            }

        return PreparedRegionalGuideRegionSelection(
            lookupRegion = lookupRegion,
            selectorRegion = selectorRegion,
            removedEupmyeondong = selectedEupmyeondong.takeIf {
                selectorRegion.eupmyeondong != selectedEupmyeondong
            },
        )
    }

    fun guideWithSelectableEupmyeondong(guide: RegionalDisposalGuide): RegionalDisposalGuide {
        val selectableEupmyeondong = guide.managementZoneName.toSelectableEupmyeondongNameOrNull()
            ?: return guide

        if (guide.region.eupmyeondong == selectableEupmyeondong) return guide

        return guide.copy(
            region = guide.region.copy(eupmyeondong = selectableEupmyeondong),
        )
    }

    fun synchronizeSelectedEupmyeondong(
        state: RegionSelectorUiState,
        guideRegion: Region,
    ): RegionSelectorUiState {
        val selectedEupmyeondong = guideRegion.eupmyeondong
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: return state

        val isSameSelectedRegion =
            state.selectedSido == guideRegion.sido && state.selectedSigungu == guideRegion.sigungu
        val hasSelectableEupmyeondong = state.eupmyeondongOptions.any { option ->
            RegionalGuideEupmyeondongNamePolicy.isSameName(
                first = selectedEupmyeondong,
                second = option,
            )
        }

        return if (isSameSelectedRegion && hasSelectableEupmyeondong) {
            state.copy(selectedEupmyeondong = selectedEupmyeondong)
        } else {
            state
        }
    }

    private fun String?.toSelectableEupmyeondongNameOrNull(): String? {
        val value = this
            ?.trim()
            ?.takeIf { text -> text.isNotBlank() && text != NO_REGION_NAME }
            ?: return null

        return value.takeIf { text ->
            text.endsWith(EUP_SUFFIX) ||
                text.endsWith(MYEON_SUFFIX) ||
                text.endsWith(DONG_SUFFIX)
        }
    }

    private const val NO_REGION_NAME = "없음"
    private const val EUP_SUFFIX = "읍"
    private const val MYEON_SUFFIX = "면"
    private const val DONG_SUFFIX = "동"
}

internal data class PreparedRegionalGuideRegionSelection(
    val lookupRegion: Region,
    val selectorRegion: Region,
    val removedEupmyeondong: String?,
)
