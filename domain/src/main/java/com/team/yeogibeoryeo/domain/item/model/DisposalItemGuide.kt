package com.team.yeogibeoryeo.domain.item.model

data class DisposalItemGuide(
    val id: String,
    val name: String,
    val category: DisposalCategory,
    val subCategory: DisposalSubCategory?,
    val instructions: List<DisposalInstruction>,
    val steps: List<String> = emptyList(),
    val cautions: List<String> = emptyList(),
    val tip: String?,
    val isRecyclable: Boolean,
    val relatedSpotTypes: List<RelatedSpotType>?,
)
