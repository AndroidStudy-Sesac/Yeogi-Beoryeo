package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.domain.item.model.DisposalGuideSectionRow
import com.team.yeogibeoryeo.presentation.common.text.KoreanLineBreakText
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults
import com.team.yeogibeoryeo.presentation.search.itemGuideDetailTextStyles

/**
 * 부가적인 정보를 담는 카드 컴포넌트입니다.
 * 테마의 SurfaceVariant 컬러를 사용하여 배경과 구분합니다.
 */
@Composable
fun SectionCard(
    title: String,
    lines: List<String>,
    modifier: Modifier = Modifier,
    numbered: Boolean = false,
    rows: List<DisposalGuideSectionRow> = emptyList(),
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val size = ItemSearchLayoutDefaults.size

    ItemGuideSectionCard(
        title = title,
        modifier = modifier,
    ) {
        if (lines.isNotEmpty()) {
            SectionLines(
                lines = lines,
                numbered = numbered,
            )
        }

        if (rows.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                rows.forEach { row ->
                    SectionRow(
                        label = row.label,
                        value = row.value,
                        labelModifier = Modifier.widthIn(
                            min = size.infoLabelMinWidth,
                            max = size.infoLabelMaxWidth,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLines(
    lines: List<String>,
    numbered: Boolean,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val textStyles = itemGuideDetailTextStyles()

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        lines.forEachIndexed { index, line ->
            val text = if (numbered) "${index + 1}. $line" else line
            KoreanLineBreakText(
                text = text,
                style = textStyles.body.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun SectionRow(
    label: String,
    value: String,
    labelModifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val textStyles = itemGuideDetailTextStyles()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        KoreanLineBreakText(
            text = label,
            modifier = labelModifier,
            style = textStyles.body.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
        )
        KoreanLineBreakText(
            text = value,
            modifier = Modifier
                .weight(1f),
            style = textStyles.body.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}
