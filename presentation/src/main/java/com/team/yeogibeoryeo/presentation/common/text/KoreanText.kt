package com.team.yeogibeoryeo.presentation.common.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

private const val ZERO_WIDTH_SPACE = "\u200B"
private val koreanSyllable = Regex("([가-힣])")
private val materialAbbreviationReadings =
    mapOf(
        "EPP" to "이피피",
        "EPS" to "이피에스",
        "EPE" to "이피이",
        "EPR" to "이피알",
        "PP" to "피피",
        "PE" to "피이",
        "PS" to "피에스",
    )
private val materialAbbreviation = Regex(
    pattern = materialAbbreviationReadings.keys.joinToString(
        separator = "|",
        prefix = "(?<![A-Za-z])(",
        postfix = ")(?![A-Za-z])",
    ),
    option = RegexOption.IGNORE_CASE,
)

fun String.withKoreanLineBreakOpportunities(): String =
    replace(koreanSyllable, "$1$ZERO_WIDTH_SPACE")

fun String.toTalkBackReadableText(): String =
    materialAbbreviation.replace(this) { match ->
        materialAbbreviationReadings.getValue(match.value.uppercase())
    }

fun Modifier.koreanLineBreakSemantics(originalText: String): Modifier =
    semantics {
        text = AnnotatedString(originalText.toTalkBackReadableText())
    }

@Composable
fun KoreanLineBreakText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text.withKoreanLineBreakOpportunities(),
        modifier = modifier.koreanLineBreakSemantics(text),
        color = color,
        style = style,
        fontWeight = fontWeight,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
    )
}
