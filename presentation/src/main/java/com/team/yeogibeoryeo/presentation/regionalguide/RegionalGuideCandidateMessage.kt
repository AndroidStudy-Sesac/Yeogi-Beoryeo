package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.presentation.R

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

@Composable
internal fun RegionalGuideCandidateReason.candidateMessage(): String =
    stringResource(id = messageResId())
