package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.presentation.R

internal data class RegionalGuideCandidateMessageSpec(
    @param:StringRes val titleResId: Int,
    val titleArgs: List<String> = emptyList(),
    @param:StringRes val descriptionResId: Int? = null,
    val descriptionArgs: List<String> = emptyList(),
    @param:StringRes val sectionTitleResId: Int? = null,
)

@StringRes
internal fun RegionalGuideCandidateReason.messageResId(): Int =
    when (this) {
        RegionalGuideCandidateReason.MULTIPLE_CANDIDATES ->
            R.string.regional_guide_candidate_multiple_message

        RegionalGuideCandidateReason.MULTIPLE_EXACT_MATCHES ->
            R.string.regional_guide_candidate_multiple_exact_message

        RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND ->
            R.string.regional_guide_candidate_fallback_message

        RegionalGuideCandidateReason.FAVORITE_RESTORE_AMBIGUOUS ->
            R.string.regional_guide_candidate_favorite_restore_ambiguous_message
    }

internal fun RegionalGuideUiState.GuideCandidates.candidateMessageSpec(): RegionalGuideCandidateMessageSpec =
    when (reason) {
        RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND -> {
            val selectedEupmyeondong = candidates.firstNotNullOfOrNull { candidate ->
                candidate.eupmyeondong?.takeIf { value -> value.isNotBlank() }
            }
            val selectedSigungu = candidates.firstNotNullOfOrNull { candidate ->
                candidate.sigungu?.takeIf { value -> value.isNotBlank() }
            }

            RegionalGuideCandidateMessageSpec(
                titleResId = R.string.regional_guide_candidate_fallback_panel_title,
                descriptionResId = if (selectedEupmyeondong != null && selectedSigungu != null) {
                    R.string.regional_guide_candidate_fallback_panel_description
                } else {
                    R.string.regional_guide_candidate_fallback_panel_description_without_region
                },
                descriptionArgs = if (selectedEupmyeondong != null && selectedSigungu != null) {
                    listOf(selectedEupmyeondong, selectedSigungu)
                } else {
                    emptyList()
                },
                sectionTitleResId = R.string.regional_guide_candidate_fallback_section_title,
            )
        }

        else -> RegionalGuideCandidateMessageSpec(titleResId = reason.messageResId())
    }

internal fun RegionalGuideUiState.GuideCandidates.collectionTypeSelectionMessageSpec():
    RegionalGuideCandidateMessageSpec =
    if (reason == RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND) {
        candidateMessageSpec()
    } else {
        val selectedSigungu = candidates.firstNotNullOfOrNull { candidate ->
            candidate.sigungu?.takeIf { value -> value.isNotBlank() }
        }

        RegionalGuideCandidateMessageSpec(
            titleResId = R.string.regional_guide_candidate_collection_type_panel_title,
            descriptionResId = if (selectedSigungu != null) {
                R.string.regional_guide_candidate_collection_type_panel_description
            } else {
                R.string.regional_guide_candidate_collection_type_panel_description_without_region
            },
            descriptionArgs = listOfNotNull(selectedSigungu),
            sectionTitleResId = R.string.regional_guide_candidate_fallback_section_title,
        )
    }

@Composable
internal fun RegionalGuideCandidateMessageSpec.title(): String =
    stringResource(id = titleResId, *titleArgs.toTypedArray())

@Composable
internal fun RegionalGuideCandidateMessageSpec.description(): String? =
    descriptionResId?.let { resId ->
        stringResource(id = resId, *descriptionArgs.toTypedArray())
    }

@Composable
internal fun RegionalGuideCandidateMessageSpec.sectionTitle(): String? =
    sectionTitleResId?.let { resId -> stringResource(id = resId) }
