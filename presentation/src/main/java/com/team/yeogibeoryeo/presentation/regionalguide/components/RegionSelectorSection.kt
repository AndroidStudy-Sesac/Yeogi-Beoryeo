package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.regionalguide.RegionSelectorDropdown
import com.team.yeogibeoryeo.presentation.regionalguide.RegionSelectorEupmyeondongSelectionStatus
import com.team.yeogibeoryeo.presentation.regionalguide.RegionSelectorUiState

@Composable
fun RegionSelectorSection(
    uiState: RegionSelectorUiState,
    onSidoSelected: (String) -> Unit,
    onSigunguSelected: (String) -> Unit,
    onEupmyeondongSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    compactLandscape: Boolean = false,
    compactRegionText: String? = null,
    onDropdownExpanded: (RegionSelectorDropdown) -> Unit = {},
    onDropdownDismissed: () -> Unit = {},
    onChangeClick: () -> Unit = {},
) {
    val selectedRegionText = uiState.selectedRegionParts.toRegionalGuideSelectorText()
    val dropdownMaxHeight = if (compactLandscape) {
        CompactLandscapeDropdownMaxHeight
    } else {
        DefaultDropdownMaxHeight
    }

    if (compact && (selectedRegionText ?: compactRegionText) != null) {
        RegionSelectorCompactCard(
            selectedRegionText = selectedRegionText ?: compactRegionText.orEmpty(),
            onChangeClick = onChangeClick,
            modifier = modifier,
        )
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(if (compactLandscape) 16.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactLandscape) 10.dp else 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RegionDropdownChip(
                    label = uiState.selectedSido
                        ?: stringResource(id = R.string.regional_guide_region_selector_sido_label),
                    options = uiState.sidoOptions,
                    expanded = uiState.expandedDropdown == RegionSelectorDropdown.SIDO,
                    onExpanded = { onDropdownExpanded(RegionSelectorDropdown.SIDO) },
                    onDismissed = onDropdownDismissed,
                    onOptionSelected = onSidoSelected,
                    dropdownMaxHeight = dropdownMaxHeight,
                    modifier = Modifier.weight(1f),
                )

                RegionDropdownChip(
                    label = uiState.selectedSigungu
                        ?: stringResource(id = R.string.regional_guide_region_selector_sigungu_label),
                    options = uiState.sigunguOptions,
                    enabled = uiState.isSigunguSelectionEnabled,
                    expanded = uiState.expandedDropdown == RegionSelectorDropdown.SIGUNGU,
                    onExpanded = { onDropdownExpanded(RegionSelectorDropdown.SIGUNGU) },
                    onDismissed = onDropdownDismissed,
                    onOptionSelected = onSigunguSelected,
                    dropdownMaxHeight = dropdownMaxHeight,
                    modifier = Modifier.weight(1f),
                )
            }

            if (compactLandscape) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RegionDropdownChip(
                        label = uiState.eupmyeondongSelectionStatus.label(),
                        options = uiState.eupmyeondongOptions,
                        enabled = uiState.isEupmyeondongSelectionEnabled,
                        expanded = uiState.expandedDropdown == RegionSelectorDropdown.EUPMYEONDONG,
                        onExpanded = { onDropdownExpanded(RegionSelectorDropdown.EUPMYEONDONG) },
                        onDismissed = onDropdownDismissed,
                        onOptionSelected = onEupmyeondongSelected,
                        dropdownMaxHeight = dropdownMaxHeight,
                        modifier = Modifier.weight(2f),
                    )

                    RegionSelectorSearchButton(
                        onClick = onSearchClick,
                        enabled = uiState.canSearchSelectedRegion,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                RegionDropdownChip(
                    label = uiState.eupmyeondongSelectionStatus.label(),
                    options = uiState.eupmyeondongOptions,
                    enabled = uiState.isEupmyeondongSelectionEnabled,
                    expanded = uiState.expandedDropdown == RegionSelectorDropdown.EUPMYEONDONG,
                    onExpanded = { onDropdownExpanded(RegionSelectorDropdown.EUPMYEONDONG) },
                    onDismissed = onDropdownDismissed,
                    onOptionSelected = onEupmyeondongSelected,
                    dropdownMaxHeight = dropdownMaxHeight,
                    modifier = Modifier.fillMaxWidth(),
                )

                selectedRegionText?.let { selectedRegionText ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Text(
                            text = selectedRegionText,
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 12.dp,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                RegionSelectorSearchButton(
                    onClick = onSearchClick,
                    enabled = uiState.canSearchSelectedRegion,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RegionSelectorSearchButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(
            text = stringResource(id = R.string.regional_guide_region_selector_search_action),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun RegionSelectorCompactCard(
    selectedRegionText: String,
    onChangeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = selectedRegionText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )

            TextButton(
                onClick = onChangeClick,
            ) {
                Text(
                    text = stringResource(id = R.string.regional_guide_region_selector_change_action),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RegionSelectorEupmyeondongSelectionStatus.label(): String =
    when (this) {
        is RegionSelectorEupmyeondongSelectionStatus.Selected -> name
        RegionSelectorEupmyeondongSelectionStatus.Loading ->
            stringResource(id = R.string.regional_guide_region_selector_eupmyeondong_loading_label)

        RegionSelectorEupmyeondongSelectionStatus.Unavailable ->
            stringResource(id = R.string.regional_guide_region_selector_eupmyeondong_unavailable_label)

        RegionSelectorEupmyeondongSelectionStatus.Default ->
            stringResource(id = R.string.regional_guide_region_selector_eupmyeondong_label)
    }

@Composable
private fun RegionDropdownChip(
    label: String,
    options: List<String>,
    expanded: Boolean,
    onExpanded: () -> Unit,
    onDismissed: () -> Unit,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dropdownMaxHeight: Dp = DefaultDropdownMaxHeight,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val dropdownWidth = maxWidth

        Column {
            FilterChip(
                modifier = Modifier.fillMaxWidth(),
                selected = enabled && label in options,
                enabled = enabled,
                onClick = {
                    if (options.isNotEmpty()) {
                        onExpanded()
                    }
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismissed,
                modifier = Modifier
                    .width(dropdownWidth)
                    .heightIn(max = dropdownMaxHeight),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                        },
                    )
                }
            }
        }
    }
}

private val CompactLandscapeDropdownMaxHeight = 180.dp
private val DefaultDropdownMaxHeight = 280.dp

@Preview(showBackground = true)
@Composable
private fun RegionSelectorSectionPreview() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
        ) {
            RegionSelectorSection(
                uiState = RegionSelectorUiState(
                    sidoOptions = listOf(
                        "서울특별시",
                        "경기도",
                        "인천광역시",
                    ),
                ),
                onSidoSelected = {},
                onSigunguSelected = {},
                onEupmyeondongSelected = {},
                onSearchClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionSelectorSectionSelectedPreview() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
        ) {
            RegionSelectorSection(
                uiState = RegionSelectorUiState(
                    sidoOptions = listOf(
                        "서울특별시",
                        "경기도",
                        "인천광역시",
                    ),
                    sigunguOptions = listOf(
                        "구로구",
                        "영등포구",
                        "종로구",
                    ),
                    eupmyeondongOptions = listOf(
                        "문래동",
                        "당산동",
                        "여의동",
                    ),
                    selectedSido = "서울특별시",
                    selectedSigungu = "영등포구",
                    selectedEupmyeondong = "문래동",
                ),
                onSidoSelected = {},
                onSigunguSelected = {},
                onEupmyeondongSelected = {},
                onSearchClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionSelectorSectionCompactPreview() {
    MaterialTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
        ) {
            RegionSelectorSection(
                uiState = RegionSelectorUiState(
                    selectedSido = "서울특별시",
                    selectedSigungu = "영등포구",
                    selectedEupmyeondong = "문래동",
                ),
                compact = true,
                onSidoSelected = {},
                onSigunguSelected = {},
                onEupmyeondongSelected = {},
                onSearchClick = {},
                onChangeClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
