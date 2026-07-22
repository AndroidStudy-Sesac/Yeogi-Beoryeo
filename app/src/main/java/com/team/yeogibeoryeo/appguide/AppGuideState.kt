package com.team.yeogibeoryeo.appguide

enum class AppGuideStep {
    ITEM_SEARCH,
    QUICK_CATEGORY,
    MAP,
    REGIONAL_GUIDE,
    FAVORITES,
    ;

    val number: Int
        get() = ordinal + 1

    val hasPrevious: Boolean
        get() = ordinal > 0

    val isLast: Boolean
        get() = this == entries.last()

    fun previous(): AppGuideStep = entries[(ordinal - 1).coerceAtLeast(0)]

    fun nextOrNull(): AppGuideStep? = entries.getOrNull(ordinal + 1)
}

data class AppGuideUiState(
    val isReady: Boolean = false,
    val isVisible: Boolean = false,
    val currentStep: AppGuideStep = AppGuideStep.ITEM_SEARCH,
)

internal const val CURRENT_APP_GUIDE_VERSION = 1
