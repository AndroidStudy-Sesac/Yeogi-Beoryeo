package com.team.yeogibeoryeo.presentation.common.text

private const val ZERO_WIDTH_SPACE = "\u200B"
private val koreanSyllable = Regex("([가-힣])")

fun String.withKoreanSyllableBreakOpportunities(): String =
    replace(koreanSyllable, "$1$ZERO_WIDTH_SPACE")
