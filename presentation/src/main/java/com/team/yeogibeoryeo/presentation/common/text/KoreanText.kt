package com.team.yeogibeoryeo.presentation.common.text

import androidx.compose.ui.text.style.LineBreak

private const val ZERO_WIDTH_SPACE = "\u200B"
private val koreanSyllable = Regex("([가-힣])")

val koreanTextLineBreak =
    LineBreak(
        strategy = LineBreak.Strategy.HighQuality,
        strictness = LineBreak.Strictness.Loose,
        wordBreak = LineBreak.WordBreak.Unspecified,
    )

fun String.withKoreanSyllableBreakOpportunities(): String =
    replace(koreanSyllable, "$1$ZERO_WIDTH_SPACE")
