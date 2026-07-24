package com.team.yeogibeoryeo.data.region.local

internal object RegionAssetContract {
    const val VERSION = "20260701"
    const val REGIONAL_GUIDE_AVAILABILITY_VERSION = "20260716"

    private const val ASSET_DIRECTORY = "region"

    private const val ADMINISTRATIVE_REGION_FILE_NAME =
        "administrative_regions.$VERSION.json"
    private const val LEGAL_ADMIN_MAPPING_FILE_NAME =
        "legal_to_admin_mappings.$VERSION.json"
    private const val REGIONAL_GUIDE_REGION_FILE_NAME =
        "regional_guide_regions.$VERSION.json"
    private const val REGIONAL_GUIDE_AVAILABILITY_FILE_NAME =
        "regional_guide_availability.$REGIONAL_GUIDE_AVAILABILITY_VERSION.json"

    const val ADMINISTRATIVE_REGION_ASSET_PATH =
        "$ASSET_DIRECTORY/$ADMINISTRATIVE_REGION_FILE_NAME"
    const val LEGAL_ADMIN_MAPPING_ASSET_PATH =
        "$ASSET_DIRECTORY/$LEGAL_ADMIN_MAPPING_FILE_NAME"
    const val REGIONAL_GUIDE_REGION_ASSET_PATH =
        "$ASSET_DIRECTORY/$REGIONAL_GUIDE_REGION_FILE_NAME"
    const val REGIONAL_GUIDE_AVAILABILITY_ASSET_PATH =
        "$ASSET_DIRECTORY/$REGIONAL_GUIDE_AVAILABILITY_FILE_NAME"
}
