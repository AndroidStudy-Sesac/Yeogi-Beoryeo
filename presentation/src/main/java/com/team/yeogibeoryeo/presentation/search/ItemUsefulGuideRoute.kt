package com.team.yeogibeoryeo.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.common.R as CommonR
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.search.components.ItemGuideActionButton
import com.team.yeogibeoryeo.presentation.search.components.SectionCard
import com.team.yeogibeoryeo.presentation.search.model.ItemUsefulGuideType
import com.team.yeogibeoryeo.presentation.search.model.toUsefulGuideContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemUsefulGuideRoute(
    guideType: ItemUsefulGuideType,
    onBackClick: () -> Unit,
    onSmallEWasteClick: (CollectionSpotType) -> Unit,
    onFreePickupGuideClick: () -> Unit,
    onOfficialSiteClick: (String) -> Unit,
    onRegionalGuideClick: () -> Unit,
    onItemSearchClick: () -> Unit,
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val content = guideType.toUsefulGuideContent()
    val spacing = ItemSearchLayoutDefaults.spacing
    val listState = rememberLazyListState()
    val detailTitle =
        if (guideType == ItemUsefulGuideType.SMALL_E_WASTE) {
            stringResource(R.string.item_useful_guide_small_e_waste_method_title)
        } else {
            stringResource(R.string.item_useful_guide_detail_section_title)
        }
    val detailLines =
        when (guideType) {
            ItemUsefulGuideType.SMALL_E_WASTE ->
                listOf(
                    stringResource(R.string.item_useful_guide_small_e_waste_method_line_bin),
                    stringResource(R.string.item_useful_guide_small_e_waste_method_line_location),
                    stringResource(R.string.item_useful_guide_small_e_waste_method_line_pickup),
                )

            else -> listOf(stringResource(content.detailResId))
        }
    val cautionLines =
        when (guideType) {
            ItemUsefulGuideType.SMALL_E_WASTE ->
                listOf(
                    stringResource(R.string.item_useful_guide_small_e_waste_caution_line_large),
                    stringResource(R.string.item_useful_guide_small_e_waste_caution_line_battery),
                    stringResource(R.string.item_useful_guide_small_e_waste_caution_line_place),
                )

            else -> listOf(stringResource(content.cautionResId))
        }

    BottomBarVisibilityOnScrollEffect(
        listState = listState,
        onBottomBarVisibilityChanged = onBottomBarVisibilityChanged,
    )

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(content.titleResId),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_action),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = spacing.xl),
            contentPadding = PaddingValues(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = stringResource(content.descriptionResId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SectionCard(
                    title = detailTitle,
                    lines = detailLines,
                )
            }

            item {
                SectionCard(
                    title = stringResource(R.string.cautions_title),
                    lines = cautionLines,
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    ItemGuideActionButton(
                        label = stringResource(content.ctaResId),
                        iconResId = CommonR.drawable.ic_action_search,
                        onClick = {
                            when (guideType) {
                                ItemUsefulGuideType.SMALL_E_WASTE ->
                                    onSmallEWasteClick(CollectionSpotType.SMALL_E_WASTE_BIN)

                                ItemUsefulGuideType.REGIONAL_GUIDE -> onRegionalGuideClick()
                                ItemUsefulGuideType.REPRESENTATIVE_CATEGORY -> onItemSearchClick()
                                ItemUsefulGuideType.ITEM_DICTIONARY -> onItemSearchClick()
                            }
                        },
                        prominent = true,
                    )

                    if (guideType == ItemUsefulGuideType.SMALL_E_WASTE) {
                        ItemGuideActionButton(
                            label = stringResource(R.string.item_guide_action_free_pickup),
                            iconResId = R.drawable.ic_disposal_route_report,
                            onClick = onFreePickupGuideClick,
                            prominent = false,
                        )
                    }
                }
            }

            if (content.relatedSites.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = stringResource(R.string.item_useful_guide_related_sites_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        content.relatedSites.forEach { site ->
                            ItemGuideActionButton(
                                label = stringResource(site.labelResId),
                                iconResId = CommonR.drawable.ic_action_search,
                                onClick = { onOfficialSiteClick(site.url) },
                                prominent = false,
                            )
                        }
                    }
                }
            }
        }
    }
}
