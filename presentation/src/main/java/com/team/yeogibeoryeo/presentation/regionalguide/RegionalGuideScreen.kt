package com.team.yeogibeoryeo.presentation.regionalguide

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.yeogibeoryeo.presentation.R
import com.team.yeogibeoryeo.presentation.common.REGIONAL_GUIDE_LINKS_URL
import com.team.yeogibeoryeo.presentation.common.components.MessageSnackbar
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionSelectorSection
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalGuideAmbiguousResult
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalGuideCandidateResult
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalGuideCollectionTypeCandidateResult
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalGuideEmptyResult
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalGuidePublicNoticeCta
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalGuideSearchBar
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalGuideSummaryCard
import com.team.yeogibeoryeo.presentation.regionalguide.components.RegionalWasteScheduleCard
import com.team.yeogibeoryeo.presentation.regionalguide.components.toRegionalGuideSelectorText
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionSearchCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideCandidateUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalGuideUiModel
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleTime
import com.team.yeogibeoryeo.presentation.regionalguide.model.RegionalWasteScheduleUiModel
import kotlinx.coroutines.launch

@Composable
fun RegionalGuideRoute(
    modifier: Modifier = Modifier,
    initialKeyword: String? = null,
    initialAddress: String? = null,
    initialFavoriteTargetId: String? = null,
    onOpenExternalUrl: (String) -> Boolean = { true },
    viewModel: RegionalGuideViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val searchKeywordRegionNameParts by viewModel.searchKeywordRegionNameParts.collectAsStateWithLifecycle()
    val regionSelectorUiState by viewModel.regionSelectorUiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val favoriteUpdateFailedMessage = stringResource(R.string.favorite_update_failed_message)
    val currentFavoriteUpdateFailedMessage by rememberUpdatedState(favoriteUpdateFailedMessage)
    LaunchedEffect(initialKeyword, initialAddress, initialFavoriteTargetId) {
        if (!viewModel.consumeInitialRouteRequest()) return@LaunchedEffect

        when {
            !initialFavoriteTargetId.isNullOrBlank() -> viewModel.loadByFavoriteTargetId(initialFavoriteTargetId)
            !initialAddress.isNullOrBlank() -> viewModel.loadByAddress(initialAddress)
            !initialKeyword.isNullOrBlank() -> {
                viewModel.onSearchKeywordChanged(initialKeyword)
                viewModel.searchByKeyword(initialKeyword)
            }
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                RegionalGuideEvent.FavoriteUpdateFailed -> {
                    snackbarHostState.showSnackbar(currentFavoriteUpdateFailedMessage)
                }
            }
        }
    }

    RegionalGuideScreen(
        uiState = uiState,
        searchKeyword = searchKeyword,
        searchKeywordRegionNameParts = searchKeywordRegionNameParts,
        regionSelectorUiState = regionSelectorUiState,
        onSearchKeywordChange = viewModel::onSearchKeywordChanged,
        onSearchClick = viewModel::searchByKeyword,
        onRetryClick = viewModel::retryLastRequest,
        onEmptySearchActionClick = viewModel::prepareSearchAgain,
        onSidoSelected = viewModel::onSidoSelected,
        onSigunguSelected = viewModel::onSigunguSelected,
        onEupmyeondongSelected = viewModel::onEupmyeondongSelected,
        onRegionSelectionStarted = viewModel::onRegionSelectionStarted,
        onRegionSelectorDropdownExpanded = viewModel::onRegionSelectorDropdownExpanded,
        onRegionSelectorDropdownDismissed = viewModel::onRegionSelectorDropdownDismissed,
        onRegionSelectionSearchClick = viewModel::onRegionSelectionSearchClick,
        onCandidateClick = viewModel::onRegionCandidateSelected,
        onGuideCandidateClick = viewModel::onRegionalGuideCandidateSelected,
        onCandidateListScrollPositionChange = viewModel::onCandidateListScrollPositionChanged,
        onRestoreCandidates = viewModel::restoreCandidatesFromDetail,
        onFavoriteClick = viewModel::onFavoriteClick,
        onPublicNoticeClick = { onOpenExternalUrl(REGIONAL_GUIDE_LINKS_URL) },
        snackbarHostState = snackbarHostState,
        modifier = modifier.statusBarsPadding(),
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RegionalGuideScreen(
    uiState: RegionalGuideUiState,
    searchKeyword: String,
    regionSelectorUiState: RegionSelectorUiState,
    searchKeywordRegionNameParts: List<String>? = null,
    onSearchKeywordChange: (String) -> Unit,
    onSearchClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    onEmptySearchActionClick: () -> Unit,
    onSidoSelected: (String) -> Unit,
    onSigunguSelected: (String) -> Unit,
    onEupmyeondongSelected: (String) -> Unit,
    onRegionSelectionSearchClick: () -> Unit,
    onCandidateClick: (RegionSearchCandidateUiModel) -> Unit,
    onGuideCandidateClick: (RegionalGuideCandidateUiModel) -> Unit,
    modifier: Modifier = Modifier,
    onRegionSelectionStarted: () -> Unit = {},
    onCandidateListScrollPositionChange: (String, RegionalGuideCandidateListScrollPosition) -> Unit = { _, _ -> },
    onRegionSelectorDropdownExpanded: (RegionSelectorDropdown) -> Unit = {},
    onRegionSelectorDropdownDismissed: () -> Unit = {},
    onRestoreCandidates: () -> Boolean = { false },
    onFavoriteClick: () -> Unit = {},
    onPublicNoticeClick: () -> Boolean = { true },
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var isRegionSelectorExpanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val publicNoticeOpenFailedMessage = stringResource(R.string.item_guide_action_open_failed_message)
    val selectedRegionText = regionSelectorUiState.selectedRegionParts.toRegionalGuideSelectorText()
    val displayedSearchKeyword = searchKeywordRegionNameParts
        ?.toRegionalGuideSelectorText()
        ?: searchKeyword
    val ambiguousState = uiState as? RegionalGuideUiState.Ambiguous
    val guideCandidatesState = uiState as? RegionalGuideUiState.GuideCandidates
    val collectionTypeGuideCandidatesState = guideCandidatesState
        ?.takeIf { state -> state.shouldShowCollectionTypeSelectionPanel() }
    val listGuideCandidatesState = guideCandidatesState
        ?.takeUnless { state -> state.shouldShowCollectionTypeSelectionPanel() }
    val hasSearchCandidates = ambiguousState != null || listGuideCandidatesState != null
    val compactRegionText = when (uiState) {
        is RegionalGuideUiState.Success ->
            selectedRegionText ?: uiState.query

        is RegionalGuideUiState.Loading ->
            uiState.regionNameParts?.toRegionalGuideSelectorText()
                ?: selectedRegionText
                ?: uiState.query

        else -> uiState.queryOrNull()
    }
    val isRegionSelectorCompact =
        uiState !is RegionalGuideUiState.Idle &&
            uiState !is RegionalGuideUiState.Ambiguous &&
            uiState !is RegionalGuideUiState.GuideCandidates &&
            !isRegionSelectorExpanded &&
            compactRegionText != null

    fun clearSearchFocus() {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun collapseRegionSelector() {
        if (isRegionSelectorExpanded) {
            isRegionSelectorExpanded = false
        }
    }

    fun showPublicNoticeOpenFailedMessage() {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(publicNoticeOpenFailedMessage)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is RegionalGuideUiState.Loading) {
            collapseRegionSelector()
        }
    }

    fun handleEmptyAction(actionType: RegionalGuideEmptyActionType) {
        when (actionType) {
            RegionalGuideEmptyActionType.SEARCH_AGAIN -> {
                collapseRegionSelector()
                onRegionSelectorDropdownDismissed()
                onEmptySearchActionClick()
            }

            RegionalGuideEmptyActionType.SELECT_REGION -> {
                onRegionSelectionStarted()
                isRegionSelectorExpanded = true
                onRegionSelectorDropdownDismissed()
            }
        }
    }

    val collectionTypePanelScrollState = rememberScrollState()

    BackHandler(
        enabled = when (uiState) {
            is RegionalGuideUiState.Loading -> uiState.canRestoreCandidates
            is RegionalGuideUiState.Success -> uiState.canRestoreCandidates
            is RegionalGuideUiState.Error -> uiState.canRestoreCandidates
            is RegionalGuideUiState.Ambiguous,
            is RegionalGuideUiState.GuideCandidates -> true
            else -> false
        }
    ) {
        onRestoreCandidates()
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                MessageSnackbar(
                    message = snackbarData.visuals.message,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(SnackbarIconSize),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            val headerModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp)
                .then(
                    if (collectionTypeGuideCandidatesState != null) {
                        Modifier
                            .weight(1f)
                            .verticalScroll(collectionTypePanelScrollState)
                    } else {
                        Modifier
                    }
                )

            Column(
                modifier = headerModifier
            ) {
                Text(
                    text = stringResource(id = R.string.regional_guide_screen_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(id = R.string.regional_guide_screen_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                RegionalGuideSearchBar(
                    keyword = displayedSearchKeyword,
                    onKeywordChange = onSearchKeywordChange,
                    onSearchClick = { submittedKeyword ->
                        clearSearchFocus()
                        collapseRegionSelector()
                        onRegionSelectorDropdownDismissed()
                        onSearchClick(
                            searchKeywordRegionNameParts
                                ?.let { searchKeyword }
                                ?: submittedKeyword,
                        )
                    },
                    candidateContent = if (hasSearchCandidates) {
                        {
                            if (ambiguousState != null) {
                                val candidateListScrollKey = ambiguousState.candidateListScrollKey()

                                RegionalGuideAmbiguousResult(
                                    candidates = ambiguousState.candidates,
                                    scrollStateKey = candidateListScrollKey,
                                    initialScrollPosition =
                                        ambiguousState.candidateListScrollPosition,
                                    onScrollPositionChange = { position ->
                                        onCandidateListScrollPositionChange(
                                            candidateListScrollKey,
                                            position,
                                        )
                                    },
                                    onCandidateClick = { candidate ->
                                        clearSearchFocus()
                                        collapseRegionSelector()
                                        onRegionSelectorDropdownDismissed()
                                        onCandidateClick(candidate)
                                    },
                                )
                            }

                            if (listGuideCandidatesState != null) {
                                val candidateListScrollKey = listGuideCandidatesState.candidateListScrollKey()

                                RegionalGuideCandidateResult(
                                    candidates = listGuideCandidatesState.candidates,
                                    scrollStateKey = candidateListScrollKey,
                                    initialScrollPosition =
                                        listGuideCandidatesState.candidateListScrollPosition,
                                    onScrollPositionChange = { position ->
                                        onCandidateListScrollPositionChange(
                                            candidateListScrollKey,
                                            position,
                                        )
                                    },
                                    onCandidateClick = { candidate ->
                                        clearSearchFocus()
                                        collapseRegionSelector()
                                        onRegionSelectorDropdownDismissed()
                                        onGuideCandidateClick(candidate)
                                    },
                                )
                            }
                        }
                    } else {
                        null
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (collectionTypeGuideCandidatesState != null) {
                    val candidateMessageSpec =
                        collectionTypeGuideCandidatesState.collectionTypeSelectionMessageSpec()

                    RegionalGuideCollectionTypeCandidateResult(
                        message = candidateMessageSpec.title(),
                        messageDescription = candidateMessageSpec.description(),
                        sectionTitle = candidateMessageSpec.sectionTitle(),
                        selectedRegionText = selectedRegionText
                            ?: collectionTypeGuideCandidatesState
                                .selectedRegionParts()
                                ?.toRegionalGuideSelectorText(),
                        candidates = collectionTypeGuideCandidatesState.candidates,
                        onCandidateClick = { candidate ->
                            clearSearchFocus()
                            collapseRegionSelector()
                            onRegionSelectorDropdownDismissed()
                            onGuideCandidateClick(candidate)
                        },
                    )
                }

                if (collectionTypeGuideCandidatesState == null) {
                    RegionSelectorSection(
                        uiState = regionSelectorUiState,
                        compact = isRegionSelectorCompact,
                        compactRegionText = compactRegionText,
                        onSidoSelected = { sido ->
                            clearSearchFocus()
                            onSidoSelected(sido)
                        },
                        onSigunguSelected = { sigungu ->
                            clearSearchFocus()
                            onSigunguSelected(sigungu)
                        },
                        onEupmyeondongSelected = { eupmyeondong ->
                            clearSearchFocus()
                            onEupmyeondongSelected(eupmyeondong)
                        },
                        onDropdownExpanded = { dropdown ->
                            clearSearchFocus()
                            onRegionSelectorDropdownExpanded(dropdown)
                        },
                        onDropdownDismissed = onRegionSelectorDropdownDismissed,
                        onSearchClick = {
                            clearSearchFocus()
                            collapseRegionSelector()
                            onRegionSelectorDropdownDismissed()
                            onRegionSelectionSearchClick()
                        },
                        onChangeClick = {
                            clearSearchFocus()
                            onRegionSelectionStarted()
                            isRegionSelectorExpanded = true
                        },
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            if (collectionTypeGuideCandidatesState == null) {
                RegionalGuideContent(
                    uiState = uiState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    onRetryClick = onRetryClick,
                    onEmptyActionClick = ::handleEmptyAction,
                    onFavoriteClick = onFavoriteClick,
                    onPublicNoticeClick = {
                        if (!onPublicNoticeClick()) {
                            showPublicNoticeOpenFailedMessage()
                        }
                    },
                )
            }
        }
    }
}

private fun RegionalGuideUiState.queryOrNull(): String? =
    when (this) {
        RegionalGuideUiState.Idle -> null
        is RegionalGuideUiState.Loading -> query
        is RegionalGuideUiState.Success -> query
        is RegionalGuideUiState.Empty -> query
        is RegionalGuideUiState.Ambiguous -> query
        is RegionalGuideUiState.GuideCandidates -> query
        is RegionalGuideUiState.Error -> query
    }?.takeIf { query -> query.isNotBlank() }

private fun RegionalGuideUiState.GuideCandidates.selectedRegionParts(): List<String>? =
    candidates.firstNotNullOfOrNull { candidate ->
        listOfNotNull(
            candidate.sido.takeIfNotBlank(),
            candidate.sigungu.takeIfNotBlank(),
            candidate.eupmyeondong.takeIfNotBlank(),
        )
            .takeIf { parts -> parts.isNotEmpty() }
    }

private fun String?.takeIfNotBlank(): String? =
    this?.takeIf { value -> value.isNotBlank() }

@Composable
private fun RegionalGuideContent(
    uiState: RegionalGuideUiState,
    onRetryClick: () -> Unit,
    onEmptyActionClick: (RegionalGuideEmptyActionType) -> Unit,
    onFavoriteClick: () -> Unit,
    onPublicNoticeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        RegionalGuideUiState.Idle -> {
            Spacer(modifier = modifier)
        }

        is RegionalGuideUiState.Loading -> {
            RegionalGuideLoadingContent(
                query = uiState.regionNameParts
                    ?.toRegionalGuideSelectorText()
                    ?: uiState.query,
                modifier = modifier
            )
        }

        is RegionalGuideUiState.Success -> {
            RegionalGuideSuccessContent(
                guide = uiState.guide,
                isFavorite = uiState.isFavorite,
                onFavoriteClick = onFavoriteClick,
                onPublicNoticeClick = onPublicNoticeClick,
                modifier = modifier
            )
        }

        is RegionalGuideUiState.Empty -> {
            val action = uiState.action
            RegionalGuideEmptyResult(
                title = stringResource(id = uiState.titleResId),
                message = stringResource(id = uiState.messageResId),
                modifier = modifier,
                actionLabel = action?.let { stringResource(id = it.labelResId) },
                onActionClick = action?.let {
                    { onEmptyActionClick(action.type) }
                },
            )
        }

        is RegionalGuideUiState.Ambiguous -> {
            Spacer(modifier = modifier)
        }

        is RegionalGuideUiState.GuideCandidates -> {
            Spacer(modifier = modifier)
        }

        is RegionalGuideUiState.Error -> {
            RegionalGuideErrorContent(
                message = uiState.message.displayText(),
                onRetryClick = onRetryClick,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun RegionalGuideErrorMessage.displayText(): String =
    when (this) {
        is RegionalGuideErrorMessage.Dynamic -> value
        is RegionalGuideErrorMessage.Resource -> stringResource(id = resId)
    }

@Composable
private fun RegionalGuideLoadingContent(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator()

        Text(
            text = query.loadingMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun String.loadingMessage(): String =
    trim()
        .takeIf { it.isNotBlank() }
        ?.let { query ->
            stringResource(
                id = R.string.regional_guide_loading_region_message,
                query,
            )
        }
        ?: stringResource(id = R.string.regional_guide_loading_default_message)

@Composable
private fun RegionalGuideSuccessContent(
    guide: RegionalGuideUiModel,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onPublicNoticeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RegionalGuideSummaryCard(
                guide = guide,
                isFavorite = isFavorite,
                onFavoriteClick = onFavoriteClick,
            )
        }

        item {
            RegionalGuidePublicNoticeCta(onClick = onPublicNoticeClick)
        }

        item {
            Text(
                text = stringResource(id = R.string.regional_guide_schedule_section_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(items = guide.schedules.groupForDisplay()) { scheduleGroup ->
            RegionalWasteScheduleCard(
                schedule = scheduleGroup.schedule,
                disposalPlaces = scheduleGroup.disposalPlaces,
            )
        }
    }
}

private fun List<RegionalWasteScheduleUiModel>.groupForDisplay(): List<RegionalWasteScheduleDisplayGroup> =
    groupBy { schedule ->
        RegionalWasteScheduleDisplayKey(
            wasteTypeName = schedule.wasteTypeName,
            disposalDays = schedule.disposalDays,
            disposalTime = schedule.disposalTime,
            disposalMethod = schedule.disposalMethod,
        )
    }
        .values
        .map { schedules ->
            RegionalWasteScheduleDisplayGroup(
                schedule = schedules.first(),
                disposalPlaces = schedules.mapNotNull { schedule ->
                    schedule.disposalPlace.takeIfNotBlank()
                }.distinct(),
            )
        }

private data class RegionalWasteScheduleDisplayKey(
    val wasteTypeName: String,
    val disposalDays: String?,
    val disposalTime: RegionalWasteScheduleTime?,
    val disposalMethod: String?,
)

private data class RegionalWasteScheduleDisplayGroup(
    val schedule: RegionalWasteScheduleUiModel,
    val disposalPlaces: List<String>,
)

@Composable
private fun RegionalGuideErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.regional_guide_error_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            TextButton(
                onClick = onRetryClick
            ) {
                Text(text = stringResource(id = R.string.regional_guide_error_retry_action))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenIdlePreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.Idle,
            searchKeyword = "",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("서울특별시", "경기도", "인천광역시"),
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onEmptySearchActionClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
            onCandidateClick = {},
            onGuideCandidateClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenLoadingPreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.Loading(
                query = "영등포구"
            ),
            searchKeyword = "영등포구",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("서울특별시", "경기도", "인천광역시"),
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onEmptySearchActionClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
            onCandidateClick = {},
            onGuideCandidateClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenSuccessPreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.Success(
                query = "영등포구",
                guide = RegionalGuideUiModel(
                    regionName = "서울특별시 영등포구 문래동",
                    managementZoneName = "영등포구",
                    targetRegionName = "문래동",
                    disposalPlaceType = "문전수거",
                    disposalPlaceDescription = "집 앞 지정 장소에 배출",
                    schedules = listOf(
                        RegionalWasteScheduleUiModel(
                            wasteTypeName = "일반쓰레기",
                            disposalDays = "월, 수, 금",
                            disposalTime = RegionalWasteScheduleTime.Range("18:00", "24:00"),
                            disposalMethod = "종량제 봉투에 담아 배출",
                        ),
                        RegionalWasteScheduleUiModel(
                            wasteTypeName = "음식물쓰레기",
                            disposalDays = "화, 목, 일",
                            disposalTime = RegionalWasteScheduleTime.Range("18:00", "24:00"),
                            disposalMethod = "음식물 전용 용기에 담아 배출",
                        ),
                        RegionalWasteScheduleUiModel(
                            wasteTypeName = "재활용품",
                            disposalDays = "목",
                            disposalTime = RegionalWasteScheduleTime.Range("18:00", "24:00"),
                            disposalMethod = "품목별로 분리하여 배출",
                        ),
                    ),
                    uncollectedDays = "토요일",
                    departmentInfo = "청소행정과 02-0000-0000",
                ),
            ),
            searchKeyword = "영등포구",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("서울특별시", "경기도", "인천광역시"),
                sigunguOptions = listOf("구로구", "영등포구", "종로구"),
                selectedSido = "서울특별시",
                selectedSigungu = "영등포구",
                eupmyeondongOptions = listOf("문래동", "당산동", "여의동"),
                selectedEupmyeondong = "문래동",
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onEmptySearchActionClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
            onCandidateClick = {},
            onGuideCandidateClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenEmptyPreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.Empty(
                query = "없는 지역",
                titleResId = R.string.regional_guide_empty_info_not_found_title,
                messageResId = R.string.regional_guide_empty_info_not_found_message,
            ),
            searchKeyword = "없는 지역",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("서울특별시", "경기도", "인천광역시"),
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onEmptySearchActionClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
            onCandidateClick = {},
            onGuideCandidateClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenAmbiguousPreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.Ambiguous(
                query = "신흥동",
                candidates = listOf(
                    RegionSearchCandidateUiModel(
                        sido = "인천광역시",
                        sigungu = "중구",
                        eupmyeondong = "신흥동",
                    ),
                    RegionSearchCandidateUiModel(
                        sido = "대전광역시",
                        sigungu = "동구",
                        eupmyeondong = "신흥동",
                    ),
                )
            ),
            searchKeyword = "신흥동",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("서울특별시", "경기도", "인천광역시"),
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onEmptySearchActionClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
            onCandidateClick = {},
            onGuideCandidateClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenGuideCandidatesPreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.GuideCandidates(
                query = "울주군",
                reason = RegionalGuideCandidateReason.MULTIPLE_CANDIDATES,
                candidates = listOf(
                    RegionalGuideCandidateUiModel(
                        guide = previewRegionalGuide(
                            regionName = "울산광역시 울주군",
                            targetRegionName = "범서, 온양, 웅촌, 언양, 삼남, 상북, 온산, 청량, 서생"
                        ),
                        sido = "울산광역시",
                        sigungu = "울주군",
                        eupmyeondong = null
                    ),
                    RegionalGuideCandidateUiModel(
                        guide = previewRegionalGuide(
                            regionName = "울산광역시 울주군",
                            targetRegionName = "두동, 두서, 삼동"
                        ),
                        sido = "울산광역시",
                        sigungu = "울주군",
                        eupmyeondong = null
                    ),
                )
            ),
            searchKeyword = "울주군",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("서울특별시", "울산광역시", "경기도"),
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onEmptySearchActionClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
            onCandidateClick = {},
            onGuideCandidateClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenFallbackGuideCandidatesPreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.GuideCandidates(
                query = "사천면",
                reason = RegionalGuideCandidateReason.FALLBACK_BECAUSE_DIRECT_MATCH_NOT_FOUND,
                candidates = listOf(
                    RegionalGuideCandidateUiModel(
                        guide = previewRegionalGuide(
                            regionName = "강원특별자치도 강릉시",
                            targetRegionName = "없음"
                        ),
                        sido = "강원특별자치도",
                        sigungu = "강릉시",
                        eupmyeondong = "사천면"
                    ),
                    RegionalGuideCandidateUiModel(
                        guide = previewRegionalGuide(
                            regionName = "강원특별자치도 강릉시",
                            targetRegionName = "없음"
                        ).copy(disposalPlaceType = "거점수거"),
                        sido = "강원특별자치도",
                        sigungu = "강릉시",
                        eupmyeondong = "사천면"
                    ),
                )
            ),
            searchKeyword = "사천면",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("강원특별자치도"),
                sigunguOptions = listOf("강릉시"),
                eupmyeondongOptions = listOf("사천면"),
                selectedSido = "강원특별자치도",
                selectedSigungu = "강릉시",
                selectedEupmyeondong = "사천면",
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onEmptySearchActionClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
            onCandidateClick = {},
            onGuideCandidateClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegionalGuideScreenErrorPreview() {
    MaterialTheme {
        RegionalGuideScreen(
            uiState = RegionalGuideUiState.Error(
                query = "영등포구",
                message = RegionalGuideErrorMessage.Resource(
                    resId = R.string.regional_guide_error_keyword_search_message,
                ),
            ),
            searchKeyword = "영등포구",
            regionSelectorUiState = RegionSelectorUiState(
                sidoOptions = listOf("서울특별시", "경기도", "인천광역시"),
            ),
            onSearchKeywordChange = {},
            onSearchClick = {},
            onRetryClick = {},
            onEmptySearchActionClick = {},
            onSidoSelected = {},
            onSigunguSelected = {},
            onEupmyeondongSelected = {},
            onRegionSelectionSearchClick = {},
            onCandidateClick = {},
            onGuideCandidateClick = {},
        )
    }
}

private fun previewRegionalGuide(
    regionName: String,
    targetRegionName: String
): RegionalGuideUiModel =
    RegionalGuideUiModel(
        regionName = regionName,
        managementZoneName = regionName,
        targetRegionName = targetRegionName,
        disposalPlaceType = "문전수거",
        disposalPlaceDescription = "문전",
        schedules = emptyList(),
        uncollectedDays = "토, 일",
        departmentInfo = "환경자원과"
    )

private val SnackbarIconSize = 20.dp
