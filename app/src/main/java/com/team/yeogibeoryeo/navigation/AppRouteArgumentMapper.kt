package com.team.yeogibeoryeo.navigation

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.presentation.search.model.ItemUsefulGuideType
import com.team.yeogibeoryeo.presentation.settings.SettingsDetailType

internal fun CollectionSpotType.toRouteType(): CollectionSpotRouteType =
    when (this) {
        CollectionSpotType.SMALL_E_WASTE_BIN -> CollectionSpotRouteType.SMALL_E_WASTE_BIN
        CollectionSpotType.BATTERY_BIN -> CollectionSpotRouteType.BATTERY_BIN
        CollectionSpotType.PHONE_DROP_OFF -> CollectionSpotRouteType.PHONE_DROP_OFF
        CollectionSpotType.RECYCLING_CENTER -> CollectionSpotRouteType.RECYCLING_CENTER
        CollectionSpotType.STANDARD_BAG_STORE -> CollectionSpotRouteType.STANDARD_BAG_STORE
        CollectionSpotType.MEDICINE_DROP_BOX -> CollectionSpotRouteType.MEDICINE_DROP_BOX
        CollectionSpotType.FLUORESCENT_LAMP_BIN -> CollectionSpotRouteType.FLUORESCENT_LAMP_BIN
        CollectionSpotType.CLOTHING_BIN -> CollectionSpotRouteType.CLOTHING_BIN
        CollectionSpotType.ICE_PACK_BIN -> CollectionSpotRouteType.ICE_PACK_BIN
        CollectionSpotType.WASTE_COOKING_OIL_BIN -> CollectionSpotRouteType.WASTE_COOKING_OIL_BIN
        CollectionSpotType.HAZARDOUS_WASTE_BIN -> CollectionSpotRouteType.HAZARDOUS_WASTE_BIN
        CollectionSpotType.OTHER -> CollectionSpotRouteType.OTHER
    }

internal fun CollectionSpotRouteType.toCollectionSpotType(): CollectionSpotType =
    when (this) {
        CollectionSpotRouteType.SMALL_E_WASTE_BIN -> CollectionSpotType.SMALL_E_WASTE_BIN
        CollectionSpotRouteType.BATTERY_BIN -> CollectionSpotType.BATTERY_BIN
        CollectionSpotRouteType.PHONE_DROP_OFF -> CollectionSpotType.PHONE_DROP_OFF
        CollectionSpotRouteType.RECYCLING_CENTER -> CollectionSpotType.RECYCLING_CENTER
        CollectionSpotRouteType.STANDARD_BAG_STORE -> CollectionSpotType.STANDARD_BAG_STORE
        CollectionSpotRouteType.MEDICINE_DROP_BOX -> CollectionSpotType.MEDICINE_DROP_BOX
        CollectionSpotRouteType.FLUORESCENT_LAMP_BIN -> CollectionSpotType.FLUORESCENT_LAMP_BIN
        CollectionSpotRouteType.CLOTHING_BIN -> CollectionSpotType.CLOTHING_BIN
        CollectionSpotRouteType.ICE_PACK_BIN -> CollectionSpotType.ICE_PACK_BIN
        CollectionSpotRouteType.WASTE_COOKING_OIL_BIN -> CollectionSpotType.WASTE_COOKING_OIL_BIN
        CollectionSpotRouteType.HAZARDOUS_WASTE_BIN -> CollectionSpotType.HAZARDOUS_WASTE_BIN
        CollectionSpotRouteType.OTHER -> CollectionSpotType.OTHER
    }

internal fun ItemUsefulGuideType.toRouteType(): ItemUsefulGuideRouteType =
    when (this) {
        ItemUsefulGuideType.SMALL_E_WASTE -> ItemUsefulGuideRouteType.SMALL_E_WASTE
        ItemUsefulGuideType.REGIONAL_GUIDE -> ItemUsefulGuideRouteType.REGIONAL_GUIDE
        ItemUsefulGuideType.REPRESENTATIVE_CATEGORY -> ItemUsefulGuideRouteType.REPRESENTATIVE_CATEGORY
        ItemUsefulGuideType.ITEM_DICTIONARY -> ItemUsefulGuideRouteType.ITEM_DICTIONARY
    }

internal fun ItemUsefulGuideRouteType.toItemUsefulGuideType(): ItemUsefulGuideType =
    when (this) {
        ItemUsefulGuideRouteType.SMALL_E_WASTE -> ItemUsefulGuideType.SMALL_E_WASTE
        ItemUsefulGuideRouteType.REGIONAL_GUIDE -> ItemUsefulGuideType.REGIONAL_GUIDE
        ItemUsefulGuideRouteType.REPRESENTATIVE_CATEGORY -> ItemUsefulGuideType.REPRESENTATIVE_CATEGORY
        ItemUsefulGuideRouteType.ITEM_DICTIONARY -> ItemUsefulGuideType.ITEM_DICTIONARY
    }

internal fun SettingsDetailType.toRouteType(): SettingsDetailRouteType =
    when (this) {
        SettingsDetailType.Notice -> SettingsDetailRouteType.Notice
        SettingsDetailType.Contact -> SettingsDetailRouteType.Contact
        SettingsDetailType.AppInfo -> SettingsDetailRouteType.AppInfo
        SettingsDetailType.LocationPermission -> SettingsDetailRouteType.LocationPermission
        SettingsDetailType.Terms -> SettingsDetailRouteType.Terms
        SettingsDetailType.PrivacyPolicy -> SettingsDetailRouteType.PrivacyPolicy
        SettingsDetailType.Sources -> SettingsDetailRouteType.Sources
        SettingsDetailType.Cache -> SettingsDetailRouteType.Cache
    }

internal fun SettingsDetailRouteType.toScreenType(): SettingsDetailType =
    when (this) {
        SettingsDetailRouteType.Notice -> SettingsDetailType.Notice
        SettingsDetailRouteType.Contact -> SettingsDetailType.Contact
        SettingsDetailRouteType.AppInfo -> SettingsDetailType.AppInfo
        SettingsDetailRouteType.LocationPermission -> SettingsDetailType.LocationPermission
        SettingsDetailRouteType.Terms -> SettingsDetailType.Terms
        SettingsDetailRouteType.PrivacyPolicy -> SettingsDetailType.PrivacyPolicy
        SettingsDetailRouteType.Sources -> SettingsDetailType.Sources
        SettingsDetailRouteType.Cache -> SettingsDetailType.Cache
    }
