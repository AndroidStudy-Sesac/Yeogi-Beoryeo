package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.model.DisposalSubCategory
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.components.DisposalGuideMetadataChips
import com.team.yeogibeoryeo.presentation.search.components.SectionCard
import com.team.yeogibeoryeo.presentation.search.components.SubGuideSection
import com.team.yeogibeoryeo.presentation.search.components.containerColor
import com.team.yeogibeoryeo.presentation.search.components.icon
import com.team.yeogibeoryeo.presentation.search.components.iconTint
import com.team.yeogibeoryeo.presentation.search.model.RepresentativeGuideCategory

@Composable
fun ItemGuideDetailScreen(
    guide: DisposalItemGuide,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backActionDescription = stringResource(R.string.back_action)
    val representativeCategory =
        RepresentativeGuideCategory.fromGuideName(guide.name)
            ?: RepresentativeGuideCategory.fromDisposalCategory(guide.category)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 24.dp,
                top = 16.dp,
                end = 24.dp,
                bottom = 0.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = backActionDescription,
                modifier = Modifier.semantics {
                    contentDescription = backActionDescription
                },
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        color = representativeCategory.containerColor(),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = representativeCategory.icon,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = representativeCategory.iconTint()
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = guide.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                DisposalGuideMetadataChips(guide = guide)
            }
        }

        if (guide.detailSections.isNotEmpty()) {
            guide.detailSections.forEach { section ->
                SectionCard(
                    title = section.title,
                    lines = section.lines,
                    rows = section.rows,
                )
            }
        } else {
            if (guide.steps.isNotEmpty()) {
                SectionCard(
                    title = stringResource(R.string.disposal_steps_title),
                    lines = guide.steps,
                    numbered = true,
                )
            }

            if (guide.cautions.isNotEmpty()) {
                SectionCard(
                    title = stringResource(R.string.cautions_title),
                    lines = guide.cautions,
                )
            }

            if (guide.subGuides.isNotEmpty()) {
                SubGuideSection(
                    title = stringResource(R.string.sub_guides_title),
                    subGuides = guide.subGuides,
                )
            }

            if (guide.features.isNotEmpty()) {
                SectionCard(
                    title = stringResource(R.string.features_title),
                    lines = guide.features,
                )
            }

            guide.tip?.let {
                SectionCard(
                    title = stringResource(R.string.tip_title),
                    lines = listOf(it),
                )
            }
        }

        SectionCard(
            title = stringResource(R.string.local_disposal_notice_title),
            lines = listOf(stringResource(R.string.local_disposal_notice)),
        )
    }
}
