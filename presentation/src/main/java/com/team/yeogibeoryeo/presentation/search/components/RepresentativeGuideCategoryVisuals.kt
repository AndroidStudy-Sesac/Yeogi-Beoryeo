package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.DryCleaning
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Liquor
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.PropaneTank
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

val RepresentativeGuideCategory.icon: ImageVector
    get() =
        when (this) {
            RepresentativeGuideCategory.CONSTRUCTION_WASTE -> Icons.Outlined.Construction
            RepresentativeGuideCategory.METAL -> Icons.Outlined.PropaneTank
            RepresentativeGuideCategory.LARGE_WASTE -> Icons.Outlined.Chair
            RepresentativeGuideCategory.COLORLESS_PET -> Icons.Outlined.LocalDrink
            RepresentativeGuideCategory.STYROFOAM -> Icons.Outlined.Inventory2
            RepresentativeGuideCategory.NON_COMBUSTIBLE -> Icons.Outlined.Diamond
            RepresentativeGuideCategory.VINYL -> Icons.Outlined.ShoppingBag
            RepresentativeGuideCategory.HAZARDOUS -> Icons.Outlined.WarningAmber
            RepresentativeGuideCategory.GLASS -> Icons.Outlined.Liquor
            RepresentativeGuideCategory.FOOD_WASTE -> Icons.Outlined.Fastfood
            RepresentativeGuideCategory.CLOTHING -> Icons.Outlined.DryCleaning
            RepresentativeGuideCategory.GENERAL -> Icons.Outlined.DeleteOutline
            RepresentativeGuideCategory.ELECTRONICS -> Icons.Outlined.Kitchen
            RepresentativeGuideCategory.BATTERY -> Icons.Outlined.BatteryFull
            RepresentativeGuideCategory.LIGHTING -> Icons.Outlined.Lightbulb
            RepresentativeGuideCategory.PAPER -> Icons.Outlined.Description
            RepresentativeGuideCategory.PAPER_PACK -> Icons.Outlined.FreeBreakfast
            RepresentativeGuideCategory.PLASTIC -> Icons.Outlined.Inventory2
            RepresentativeGuideCategory.OTHER -> Icons.Outlined.Grass
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
