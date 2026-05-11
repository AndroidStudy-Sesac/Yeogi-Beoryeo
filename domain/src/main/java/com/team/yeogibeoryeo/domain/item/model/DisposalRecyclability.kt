package com.team.yeogibeoryeo.domain.item.model

object DisposalRecyclability {
    private val RECYCLABLE_METHODS = setOf("재활용폐기물", "보증금 환급", "역회수")

    private val RECYCLABLE_BY_CATEGORY =
        setOf(
            DisposalCategory.PAPER,
            DisposalCategory.GLASS,
            DisposalCategory.METAL,
            DisposalCategory.PLASTIC,
            DisposalCategory.STYROFOAM,
            DisposalCategory.VINYL,
            DisposalCategory.ELECTRONICS,
            DisposalCategory.CLOTHING,
        )

    fun fromMethods(methods: List<String>): Boolean = methods.any { it in RECYCLABLE_METHODS }

    fun fromCategory(category: DisposalCategory): Boolean = category in RECYCLABLE_BY_CATEGORY
}
