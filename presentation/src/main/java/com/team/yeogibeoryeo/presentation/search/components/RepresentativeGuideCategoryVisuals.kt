package com.team.yeogibeoryeo.presentation.search.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@get:DrawableRes
val RepresentativeGuideCategory.iconResId: Int
    get() =
        when (this) {
            RepresentativeGuideCategory.PAPER -> R.drawable.ic_disposal_category_paper
            RepresentativeGuideCategory.PAPER_PACK -> R.drawable.ic_disposal_category_paper_pack
            RepresentativeGuideCategory.COLORLESS_PET -> R.drawable.ic_disposal_category_colorless_pet
            RepresentativeGuideCategory.PLASTIC -> R.drawable.ic_disposal_category_plastic
            RepresentativeGuideCategory.VINYL -> R.drawable.ic_disposal_category_vinyl
            RepresentativeGuideCategory.STYROFOAM -> R.drawable.ic_disposal_category_styrofoam
            RepresentativeGuideCategory.GLASS -> R.drawable.ic_disposal_category_glass
            RepresentativeGuideCategory.METAL -> R.drawable.ic_disposal_category_metal
            RepresentativeGuideCategory.CLOTHING -> R.drawable.ic_disposal_category_clothing
            RepresentativeGuideCategory.BATTERY -> R.drawable.ic_disposal_category_battery
            RepresentativeGuideCategory.LIGHTING -> R.drawable.ic_disposal_category_lighting
            RepresentativeGuideCategory.ELECTRONICS -> R.drawable.ic_disposal_category_electronics
            RepresentativeGuideCategory.FOOD_WASTE -> R.drawable.ic_disposal_category_food_waste
            RepresentativeGuideCategory.GENERAL -> R.drawable.ic_disposal_category_general
            RepresentativeGuideCategory.NON_COMBUSTIBLE -> R.drawable.ic_disposal_category_non_combustible
            RepresentativeGuideCategory.LARGE_WASTE -> R.drawable.ic_disposal_category_large_waste
            RepresentativeGuideCategory.CONSTRUCTION_WASTE -> R.drawable.ic_disposal_category_construction_waste
            RepresentativeGuideCategory.HAZARDOUS -> R.drawable.ic_disposal_category_hazardous
            RepresentativeGuideCategory.OTHER -> R.drawable.ic_disposal_category_other
        }

@Composable
fun RepresentativeGuideCategory.containerColor(): Color =
    when (this) {
        RepresentativeGuideCategory.CONSTRUCTION_WASTE -> MaterialTheme.colorScheme.errorContainer
        RepresentativeGuideCategory.METAL -> MaterialTheme.colorScheme.primaryContainer
        RepresentativeGuideCategory.LARGE_WASTE -> MaterialTheme.colorScheme.tertiaryContainer
        RepresentativeGuideCategory.COLORLESS_PET -> MaterialTheme.colorScheme.primaryContainer
        RepresentativeGuideCategory.STYROFOAM -> MaterialTheme.colorScheme.secondaryContainer
        RepresentativeGuideCategory.NON_COMBUSTIBLE -> MaterialTheme.colorScheme.surfaceVariant
        RepresentativeGuideCategory.VINYL -> MaterialTheme.colorScheme.tertiaryContainer
        RepresentativeGuideCategory.HAZARDOUS -> MaterialTheme.colorScheme.errorContainer
        RepresentativeGuideCategory.GLASS -> MaterialTheme.colorScheme.primaryContainer
        RepresentativeGuideCategory.FOOD_WASTE -> MaterialTheme.colorScheme.secondaryContainer
        RepresentativeGuideCategory.CLOTHING -> MaterialTheme.colorScheme.tertiaryContainer
        RepresentativeGuideCategory.GENERAL -> MaterialTheme.colorScheme.surfaceVariant
        RepresentativeGuideCategory.ELECTRONICS -> MaterialTheme.colorScheme.primaryContainer
        RepresentativeGuideCategory.BATTERY -> MaterialTheme.colorScheme.secondaryContainer
        RepresentativeGuideCategory.LIGHTING -> MaterialTheme.colorScheme.tertiaryContainer
        RepresentativeGuideCategory.PAPER -> MaterialTheme.colorScheme.secondaryContainer
        RepresentativeGuideCategory.PAPER_PACK -> MaterialTheme.colorScheme.secondaryContainer
        RepresentativeGuideCategory.PLASTIC -> MaterialTheme.colorScheme.primaryContainer
        RepresentativeGuideCategory.OTHER -> MaterialTheme.colorScheme.surfaceVariant
    }

@Composable
fun RepresentativeGuideCategory.iconTint(): Color =
    when (this) {
        RepresentativeGuideCategory.CONSTRUCTION_WASTE -> MaterialTheme.colorScheme.onErrorContainer
        RepresentativeGuideCategory.METAL -> MaterialTheme.colorScheme.onPrimaryContainer
        RepresentativeGuideCategory.LARGE_WASTE -> MaterialTheme.colorScheme.onTertiaryContainer
        RepresentativeGuideCategory.COLORLESS_PET -> MaterialTheme.colorScheme.onPrimaryContainer
        RepresentativeGuideCategory.STYROFOAM -> MaterialTheme.colorScheme.onSecondaryContainer
        RepresentativeGuideCategory.NON_COMBUSTIBLE -> MaterialTheme.colorScheme.onSurfaceVariant
        RepresentativeGuideCategory.VINYL -> MaterialTheme.colorScheme.onTertiaryContainer
        RepresentativeGuideCategory.HAZARDOUS -> MaterialTheme.colorScheme.onErrorContainer
        RepresentativeGuideCategory.GLASS -> MaterialTheme.colorScheme.onPrimaryContainer
        RepresentativeGuideCategory.FOOD_WASTE -> MaterialTheme.colorScheme.onSecondaryContainer
        RepresentativeGuideCategory.CLOTHING -> MaterialTheme.colorScheme.onTertiaryContainer
        RepresentativeGuideCategory.GENERAL -> MaterialTheme.colorScheme.onSurfaceVariant
        RepresentativeGuideCategory.ELECTRONICS -> MaterialTheme.colorScheme.onPrimaryContainer
        RepresentativeGuideCategory.BATTERY -> MaterialTheme.colorScheme.onSecondaryContainer
        RepresentativeGuideCategory.LIGHTING -> MaterialTheme.colorScheme.onTertiaryContainer
        RepresentativeGuideCategory.PAPER -> MaterialTheme.colorScheme.onSecondaryContainer
        RepresentativeGuideCategory.PAPER_PACK -> MaterialTheme.colorScheme.onSecondaryContainer
        RepresentativeGuideCategory.PLASTIC -> MaterialTheme.colorScheme.onPrimaryContainer
        RepresentativeGuideCategory.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
    }
