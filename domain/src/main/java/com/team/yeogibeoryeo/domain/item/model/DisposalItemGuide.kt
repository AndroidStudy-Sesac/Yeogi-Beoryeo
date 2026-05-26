package com.team.yeogibeoryeo.domain.item.model

data class DisposalItemGuide(
    val id: String,
    val name: String,
    val category: DisposalCategory,
    val subCategory: DisposalSubCategory?,
    val instructions: List<DisposalInstruction>,
    val steps: List<String> = emptyList(),
    val features: List<String> = emptyList(),
    val cautions: List<String> = emptyList(),
    val subGuides: List<DisposalSubGuide> = emptyList(),
    val detailSections: List<DisposalGuideSection> = emptyList(),
    val tip: String?,
    val isRecyclable: Boolean,
    val relatedSpotTypes: List<RelatedSpotType>?,
)

data class DisposalSubGuide(
    val name: String,
    val summary: String,
)

data class DisposalGuideSection(
    val title: String,
    val lines: List<String>,
    val rows: List<DisposalGuideSectionRow> = emptyList(),
)

data class DisposalGuideSectionRow(
    val label: String,
    val value: String,
)
