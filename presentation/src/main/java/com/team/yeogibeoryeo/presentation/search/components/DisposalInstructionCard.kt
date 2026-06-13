package com.team.yeogibeoryeo.presentation.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.text.withKoreanLineBreakOpportunities
import com.team.yeogibeoryeo.presentation.search.ItemSearchLayoutDefaults
import com.team.yeogibeoryeo.presentation.search.itemGuideDetailTextStyles

/**
 * '버리는 방법'을 강조하여 보여주는 카드 컴포넌트입니다.
 * 테마의 PrimaryContainer 컬러를 사용하여 강조 효과를 줍니다.
 */
@Composable
fun DisposalInstructionCard(
    instructions: List<DisposalInstruction>,
    modifier: Modifier = Modifier,
) {
    val spacing = ItemSearchLayoutDefaults.spacing
    val textStyles = itemGuideDetailTextStyles()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    ) {
        Column(
            modifier = Modifier.padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.disposal_method_title),
                style = textStyles.sectionTitle.copy(
                    fontWeight = FontWeight.Bold,
                ),
            )
            instructions.forEach { instruction ->
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    Text(
                        text = instruction.method.withKoreanLineBreakOpportunities(),
                        style = textStyles.emphasizedBody.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    instruction.tip?.let {
                        Text(
                            text = it.withKoreanLineBreakOpportunities(),
                            style = textStyles.supportingBody.copy(
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.84f),
                            ),
                        )
                    }
                }
            }
        }
    }
}
