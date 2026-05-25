package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.presentation.common.design.AppAccentColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DisposalGuideMetadataChips(
    guide: DisposalItemGuide,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DisposalRouteChip(guide = guide)
        DisposalCategoryChip(category = guide.category)
        guide.subCategory?.let { DisposalSubCategoryChip(subCategory = it) }
    }
}

@Composable
fun DisposalRouteChip(
    guide: DisposalItemGuide,
    modifier: Modifier = Modifier,
) {
    val route = DisposalRouteStatus.from(guide) ?: return
    if (route.icon == null) {
        MetadataChip(
            text = route.label,
            modifier = modifier,
            containerColor = route.containerColor,
            contentColor = route.contentColor,
        )
        return
    }

    Box(
        modifier =
            modifier
                .semantics { contentDescription = route.label }
                .background(
                    color = route.containerColor,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = route.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = route.contentColor,
        )
    }
}

private enum class DisposalRouteStatus(
    val label: String,
    val icon: ImageVector?,
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
) {
    RECYCLABLE(
        "재활용 분리배출",
        Icons.Outlined.Recycling,
        AppAccentColors.SoftMint,
        AppAccentColors.Mint
    ),
    LARGE_WASTE("신고 후 배출", Icons.Outlined.Phone, AppAccentColors.SoftAmber, AppAccentColors.Amber),
    DEDICATED_COLLECTION("전용 수거", null, AppAccentColors.SoftSky, AppAccentColors.Sky),
    ;

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
