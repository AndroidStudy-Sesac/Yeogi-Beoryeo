package com.team.yeogibeoryeo.presentation.search.components

import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

internal val RepresentativeGuideCategory.quickCategoryLabel: String
    get() = when (this) {
        RepresentativeGuideCategory.FOOD_WASTE -> "음식물류\n폐기물"
        RepresentativeGuideCategory.GENERAL -> "일반종량제\n폐기물"
        RepresentativeGuideCategory.NON_COMBUSTIBLE -> "불연성종량제 폐기물"
        RepresentativeGuideCategory.CONSTRUCTION_WASTE -> "공사장\n생활폐기물"
        RepresentativeGuideCategory.HAZARDOUS -> "생활계\n유해폐기물"
        else -> displayName
    }
