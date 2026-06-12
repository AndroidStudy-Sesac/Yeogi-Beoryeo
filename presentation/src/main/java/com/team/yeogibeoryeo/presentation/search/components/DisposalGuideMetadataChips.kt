package com.team.yeogibeoryeo.presentation.search.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisposalGuideMetadataChips(
    guide: DisposalItemGuide,
    modifier: Modifier = Modifier,
    showCategoryChip: Boolean = true,
) {
    val spacing = ItemSearchLayoutDefaults.spacing

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        DisposalRouteChip(guide = guide)
        if (showCategoryChip) {
            DisposalCategoryChip(category = guide.category)
        }
        guide.subCategory?.let { DisposalSubCategoryChip(subCategory = it) }
    }
}

@Composable
fun DisposalRouteChip(
    guide: DisposalItemGuide,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size
    val route = DisposalRouteStatus.from(guide) ?: return
    if (route.iconResId == null) {
        MetadataChip(
            text = route.label,
            modifier = modifier,
            containerColor = route.containerColor(),
            contentColor = route.contentColor(),
        )
        return
    }

    Box(
        modifier =
            modifier
                .semantics { contentDescription = route.label }
                .background(
                    color = route.containerColor(),
                    shape = MaterialTheme.shapes.small,
                )
                .padding(spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = route.iconResId),
            contentDescription = null,
            modifier = Modifier.size(size.iconSmall),
            tint = route.contentColor(),
        )
    }
}

private enum class DisposalRouteStatus(
    val label: String,
    @param:DrawableRes val iconResId: Int?,
) {
    RECYCLABLE(
        "재활용 분리배출",
        CommonR.drawable.ic_symbol_recycle,
    ),
    LARGE_WASTE("신고 후 배출", R.drawable.ic_disposal_route_report),
    DEDICATED_COLLECTION("전용 수거", null),
    ;

    @Composable
    fun containerColor(): Color =
        when (this) {
            RECYCLABLE -> MaterialTheme.colorScheme.secondaryContainer
            LARGE_WASTE -> MaterialTheme.colorScheme.tertiaryContainer
            DEDICATED_COLLECTION -> MaterialTheme.colorScheme.primaryContainer
        }

    @Composable
    fun contentColor(): Color =
        when (this) {
            RECYCLABLE -> MaterialTheme.colorScheme.onSecondaryContainer
            LARGE_WASTE -> MaterialTheme.colorScheme.onTertiaryContainer
            DEDICATED_COLLECTION -> MaterialTheme.colorScheme.onPrimaryContainer
        }

    companion object {
        fun from(guide: DisposalItemGuide): DisposalRouteStatus? =
            when (guide.category) {
                DisposalCategory.PAPER,
                DisposalCategory.PAPER_PACK,
                DisposalCategory.COLORLESS_PET,
                DisposalCategory.GLASS,
                DisposalCategory.METAL,
                DisposalCategory.PLASTIC,
                DisposalCategory.STYROFOAM,
                DisposalCategory.VINYL,
                DisposalCategory.CLOTHING,
                DisposalCategory.ELECTRONICS,
                DisposalCategory.BATTERY,
                DisposalCategory.LIGHTING,
                    -> RECYCLABLE

                DisposalCategory.HAZARDOUS -> DEDICATED_COLLECTION

                DisposalCategory.LARGE_WASTE -> LARGE_WASTE

                DisposalCategory.FOOD_WASTE -> null
                DisposalCategory.NON_COMBUSTIBLE -> null
                DisposalCategory.CONSTRUCTION_WASTE -> null
                DisposalCategory.GENERAL -> null
                DisposalCategory.OTHER -> null
            }
    }
}
