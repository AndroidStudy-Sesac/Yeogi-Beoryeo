package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.team.yeogibeoryeo.presentation.R

@Composable
internal fun List<String>.toRegionalGuideSelectorText(): String? =
    when (size) {
        0 -> null
        1 -> first()
        2 -> stringResource(
            id = R.string.regional_guide_region_two_name_format,
            this[0],
            this[1],
        )

        3 -> stringResource(
            id = R.string.regional_guide_region_three_name_format,
            this[0],
            this[1],
            this[2],
        )

        else -> null
    }

@Composable
internal fun List<String>.toRegionalGuideSummaryText(): String? =
    when (size) {
        0 -> null
        1 -> first()
        2 -> stringResource(
            id = R.string.regional_guide_summary_two_name_format,
            this[0],
            this[1],
        )

        3 -> stringResource(
            id = R.string.regional_guide_summary_three_name_format,
            this[0],
            this[1],
            this[2],
        )

        else -> null
    }
