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
import com.team.yeogibeoryeo.presentation.common.design.AppAccentColors
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
        RepresentativeGuideCategory.CONSTRUCTION_WASTE -> AppAccentColors.SoftBrick
        RepresentativeGuideCategory.METAL -> AppAccentColors.SoftBlue
        RepresentativeGuideCategory.LARGE_WASTE -> AppAccentColors.SoftBrown
        RepresentativeGuideCategory.COLORLESS_PET -> AppAccentColors.SoftSky
        RepresentativeGuideCategory.STYROFOAM -> AppAccentColors.SoftLime
        RepresentativeGuideCategory.NON_COMBUSTIBLE -> AppAccentColors.SoftGrayStrong
        RepresentativeGuideCategory.VINYL -> AppAccentColors.SoftMagenta
        RepresentativeGuideCategory.HAZARDOUS -> AppAccentColors.SoftRed
        RepresentativeGuideCategory.GLASS -> AppAccentColors.SoftCyan
        RepresentativeGuideCategory.FOOD_WASTE -> AppAccentColors.SoftMint
        RepresentativeGuideCategory.CLOTHING -> AppAccentColors.SoftRose
        RepresentativeGuideCategory.GENERAL -> MaterialTheme.colorScheme.surfaceVariant
        RepresentativeGuideCategory.ELECTRONICS -> AppAccentColors.SoftIndigo
        RepresentativeGuideCategory.BATTERY -> AppAccentColors.SoftTeal
        RepresentativeGuideCategory.LIGHTING -> AppAccentColors.SoftAmber
        RepresentativeGuideCategory.PAPER -> AppAccentColors.SoftOrange
        RepresentativeGuideCategory.PAPER_PACK -> AppAccentColors.SoftCoral
        RepresentativeGuideCategory.PLASTIC -> AppAccentColors.SoftViolet
        RepresentativeGuideCategory.OTHER -> AppAccentColors.SoftOlive
    }

@Composable
fun RepresentativeGuideCategory.iconTint(): Color =
    when (this) {
        RepresentativeGuideCategory.CONSTRUCTION_WASTE -> AppAccentColors.Brick
        RepresentativeGuideCategory.METAL -> AppAccentColors.PointBlue
        RepresentativeGuideCategory.LARGE_WASTE -> AppAccentColors.Brown
        RepresentativeGuideCategory.COLORLESS_PET -> AppAccentColors.Sky
        RepresentativeGuideCategory.STYROFOAM -> AppAccentColors.Lime
        RepresentativeGuideCategory.NON_COMBUSTIBLE -> AppAccentColors.Gray
        RepresentativeGuideCategory.VINYL -> AppAccentColors.Magenta
        RepresentativeGuideCategory.HAZARDOUS -> AppAccentColors.Red
        RepresentativeGuideCategory.GLASS -> AppAccentColors.MainCyan
        RepresentativeGuideCategory.FOOD_WASTE -> AppAccentColors.Mint
        RepresentativeGuideCategory.CLOTHING -> AppAccentColors.Rose
        RepresentativeGuideCategory.GENERAL -> AppAccentColors.DarkSlate
        RepresentativeGuideCategory.ELECTRONICS -> AppAccentColors.Indigo
        RepresentativeGuideCategory.BATTERY -> AppAccentColors.Teal
        RepresentativeGuideCategory.LIGHTING -> AppAccentColors.Amber
        RepresentativeGuideCategory.PAPER -> AppAccentColors.Orange
        RepresentativeGuideCategory.PAPER_PACK -> AppAccentColors.Coral
        RepresentativeGuideCategory.PLASTIC -> AppAccentColors.Violet
        RepresentativeGuideCategory.OTHER -> AppAccentColors.Olive
    }
