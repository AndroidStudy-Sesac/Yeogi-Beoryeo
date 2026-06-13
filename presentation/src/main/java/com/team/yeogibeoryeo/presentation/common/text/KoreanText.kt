package com.team.yeogibeoryeo.presentation.common.text

import androidx.compose.ui.text.style.LineBreak

val koreanTextLineBreak =
    LineBreak(
        strategy = LineBreak.Strategy.HighQuality,
        strictness = LineBreak.Strictness.Loose,
        wordBreak = LineBreak.WordBreak.Unspecified,
    )
