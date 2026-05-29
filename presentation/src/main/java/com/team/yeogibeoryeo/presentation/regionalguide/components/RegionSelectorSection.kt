package com.team.yeogibeoryeo.presentation.regionalguide.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.team.yeogibeoryeo.presentation.regionalguide.RegionSelectorUiState

@Composable
fun RegionSelectorSection(
    uiState: RegionSelectorUiState,
    compact: Boolean = false,
    compactRegionText: String? = uiState.selectedRegionText,
    onSidoSelected: (String) -> Unit,
    onSigunguSelected: (String) -> Unit,
    onEupmyeondongSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onChangeClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (compact && compactRegionText != null) {
        RegionSelectorCompactCard(
            selectedRegionText = compactRegionText,
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
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RegionDropdownChip(
                    label = uiState.selectedSido ?: "시도 선택",
                    options = uiState.sidoOptions,
                    onOptionSelected = onSidoSelected,
                    modifier = Modifier.weight(1f),
                )

                RegionDropdownChip(
                    label = uiState.selectedSigungu ?: "시군구 선택",
                    options = uiState.sigunguOptions,
                    enabled = uiState.isSigunguSelectionEnabled,
                    onOptionSelected = onSigunguSelected,
                    modifier = Modifier.weight(1f),
                )
            }

            RegionDropdownChip(
                label = uiState.selectedEupmyeondong ?: "읍면동 선택",
                options = uiState.eupmyeondongOptions,
                enabled = uiState.isEupmyeondongSelectionEnabled,
                onOptionSelected = onEupmyeondongSelected,
                modifier = Modifier.fillMaxWidth(),
            )

            uiState.selectedRegionText?.let { selectedRegionText ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        text = "$selectedRegionText",
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

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSearchClick,
                enabled = uiState.canSearchSelectedRegion,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = "조회",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
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
                    text = "변경",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RegionDropdownChip(
    label: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
    ) {
        FilterChip(
            modifier = Modifier.fillMaxWidth(),
            selected = enabled && label in options,
            enabled = enabled,
            onClick = {
                if (options.isNotEmpty()) {
                    expanded = true
                }
            },
            label = {
                Text(
                    text = label,
                    maxLines = 1,
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
            onDismissRequest = {
                expanded = false
            },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(text = option)
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

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
