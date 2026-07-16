package com.cristobalcariqueo.defensadedeudores.ui.screens.track

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cristobalcariqueo.defensadedeudores.R
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import com.cristobalcariqueo.defensadedeudores.domain.model.Source
import com.cristobalcariqueo.defensadedeudores.ui.components.ColorDot
import com.cristobalcariqueo.defensadedeudores.ui.components.DonutChart
import com.cristobalcariqueo.defensadedeudores.ui.components.RegistryRow
import com.cristobalcariqueo.defensadedeudores.ui.format.formatClp
import com.cristobalcariqueo.defensadedeudores.ui.theme.swatchColor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Track screen (SCOPE.md screen 2): circular graphs, per-source quick-create,
 * recent + historical tables, search and AND-filters. Return-search modal is
 * task #8; tapping an amount opens the edit flow in task #9.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(
    trackId: String,
    onBack: () -> Unit,
    onOpenPeople: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenTrackConfig: () -> Unit,
    viewModel: TrackViewModel = koinViewModel(parameters = { parametersOf(trackId) }),
) {
    val track by viewModel.track.collectAsState()
    val shortcuts by viewModel.shortcuts.collectAsState()
    val allPeople by viewModel.allPeople.collectAsState()
    val allSources by viewModel.allSources.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val selectedDebtorId by viewModel.selectedDebtorId.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val recentRows by viewModel.recentRows.collectAsState()
    val recentSize by viewModel.recentSize.collectAsState()
    val historyVisible by viewModel.historyVisible.collectAsState()
    val historyRows by viewModel.historyRows.collectAsState()
    val historySize by viewModel.historySize.collectAsState()
    val historyPage by viewModel.historyPage.collectAsState()
    val historyCount by viewModel.historyCount.collectAsState()
    val debtorSlices by viewModel.debtorSlices.collectAsState()
    val sourceSlices by viewModel.sourceSlices.collectAsState()
    val outstandingTotal by viewModel.outstandingTotal.collectAsState()
    val returnSearch by viewModel.returnSearch.collectAsState()
    val suggestion by viewModel.suggestion.collectAsState()
    val editTarget by viewModel.editTarget.collectAsState()
    val fullEditTarget by viewModel.fullEditTarget.collectAsState()

    var filtersOpen by remember { mutableStateOf(false) }
    var detailedCreate by remember { mutableStateOf(false) }
    var quickCreateSource by remember { mutableStateOf<Source?>(null) }

    val returnBg = settings?.returnBgColor ?: "#FFF3CD"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(track?.name ?: stringResource(R.string.screen_track_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenTrackConfig) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.track_config_title),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(8.dp)) {
                    DonutChart(
                        title = stringResource(R.string.track_graph_by_debtor),
                        slices = debtorSlices,
                        modifier = Modifier.weight(1f),
                    )
                    DonutChart(
                        title = stringResource(R.string.track_graph_by_source),
                        slices = sourceSlices,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    stringResource(R.string.track_outstanding_total, formatClp(outstandingTotal)),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                QuickCreateSection(
                    shortcuts = shortcuts,
                    sources = sources,
                    selectedDebtorId = selectedDebtorId,
                    onSelectDebtor = viewModel::selectDebtor,
                    onSourceClick = { quickCreateSource = it },
                    onOpenPeople = onOpenPeople,
                    onOpenSources = onOpenSources,
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    CompactSearchField(
                        value = filter.query,
                        onValueChange = viewModel::setSearchQuery,
                        hint = stringResource(R.string.track_search_hint),
                        modifier = Modifier.weight(1f),
                    )
                    FilledIconToggleButton(
                        checked = filter.hasSheetFilters,
                        onCheckedChange = { filtersOpen = true },
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.track_filters),
                        )
                    }
                    IconButton(onClick = viewModel::openReturnSearch) {
                        Icon(
                            Icons.Default.CurrencyExchange,
                            contentDescription = stringResource(R.string.return_search_title),
                        )
                    }
                    IconButton(onClick = { detailedCreate = true }) {
                        Icon(
                            Icons.Default.PostAdd,
                            contentDescription = stringResource(R.string.detailed_create_title),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                TableHeader(
                    title = stringResource(R.string.track_recent_header),
                    size = recentSize,
                    options = RECENT_SIZES,
                    onSize = viewModel::setRecentSize,
                )
            }
            if (recentRows.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.track_table_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(recentRows, key = { it.registry.id }) { row ->
                    RegistryRow(
                        row,
                        returnBg,
                        onAmountClick = viewModel::openEdit,
                        onLongPress = viewModel::openFullEdit,
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        stringResource(R.string.track_history_show),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = historyVisible, onCheckedChange = { viewModel.toggleHistory() })
                }
            }
            if (historyVisible) {
                item {
                    TableHeader(
                        title = stringResource(R.string.track_history_header),
                        size = historySize,
                        options = HISTORY_SIZES,
                        onSize = viewModel::setHistorySize,
                    )
                }
                items(historyRows, key = { "h-${it.registry.id}" }) { row ->
                    RegistryRow(
                        row,
                        returnBg,
                        onAmountClick = viewModel::openEdit,
                        onLongPress = viewModel::openFullEdit,
                    )
                }
                item {
                    HistoryPager(
                        page = historyPage,
                        pageCount = pageCount(historyCount, historySize),
                        onPage = viewModel::setHistoryPage,
                    )
                }
            }
        }
    }

    quickCreateSource?.let { source ->
        val debtor = shortcuts.firstOrNull { it.id == selectedDebtorId }
        if (debtor != null) {
            QuickCreateDialog(
                debtorName = debtor.name,
                sourceName = source.name,
                onConfirm = { amount, note ->
                    viewModel.createNormal(debtor.id, source.id, amount, note)
                    quickCreateSource = null
                },
                onDismiss = { quickCreateSource = null },
            )
        }
    }

    editTarget?.let { row ->
        EditAmountDialog(
            row = row,
            onConfirm = viewModel::confirmEdit,
            onDismiss = viewModel::closeEdit,
        )
    }

    fullEditTarget?.let { row ->
        EditRegistryDialog(
            row = row,
            sources = allSources,
            onConfirm = viewModel::confirmFullEdit,
            onDismiss = viewModel::closeEdit,
        )
    }

    if (detailedCreate) {
        DetailedCreateDialog(
            people = allPeople,
            sources = sources,
            onConfirm = { personId, sourceId, amount, note, date ->
                viewModel.createNormal(personId, sourceId, amount, note, date)
                detailedCreate = false
            },
            onDismiss = { detailedCreate = false },
        )
    }

    returnSearch?.let { state ->
        ReturnSearchDialog(
            state = state,
            onSelectDebtor = viewModel::selectReturnDebtor,
            onAmountChange = viewModel::setReturnAmount,
            onToggle = viewModel::toggleReturnSelection,
            onSettle = viewModel::settleSelected,
            onCreateReturn = viewModel::createReturnFromSearch,
            onDismiss = viewModel::closeReturnSearch,
        )
    }

    suggestion?.let { current ->
        MatchSuggestionDialog(
            suggestion = current,
            returnBgColor = returnBg,
            onConfirm = viewModel::confirmSuggestion,
            onDismiss = viewModel::dismissSuggestion,
        )
    }

    if (filtersOpen) {
        FilterSheet(
            current = filter,
            people = allPeople,
            allSources = allSources,
            onApply = { newFilter ->
                viewModel.setFilter(newFilter)
                filtersOpen = false
            },
            onDismiss = { filtersOpen = false },
        )
    }
}

private val RECENT_SIZES = listOf(20, 30, 40, 50, 60, 70)
private val HISTORY_SIZES = listOf(50, 75, 100, 125, 150)

private fun pageCount(count: Int, size: Int): Int =
    if (count <= 0 || size <= 0) 1 else (count + size - 1) / size

@Composable
private fun QuickCreateSection(
    shortcuts: List<Person>,
    sources: List<Source>,
    selectedDebtorId: String?,
    onSelectDebtor: (String) -> Unit,
    onSourceClick: (Source) -> Unit,
    onOpenPeople: () -> Unit,
    onOpenSources: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            stringResource(R.string.track_quick_create),
            style = MaterialTheme.typography.titleSmall,
        )
        if (shortcuts.isEmpty()) {
            EmptyStateAction(
                message = stringResource(R.string.track_no_shortcuts_warning),
                button = stringResource(R.string.track_go_to_people),
                onClick = onOpenPeople,
            )
            return@Column
        }
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
        ) {
            shortcuts.forEach { person ->
                FilterChip(
                    selected = person.id == selectedDebtorId,
                    onClick = { onSelectDebtor(person.id) },
                    leadingIcon = {
                        ColorDot(swatchColor(person.color, dark, fallbackSeed = person.id))
                    },
                    label = { Text(person.name, maxLines = 1) },
                )
            }
        }
        if (sources.isEmpty()) {
            EmptyStateAction(
                message = stringResource(R.string.track_no_sources_warning),
                button = stringResource(R.string.track_go_to_sources),
                onClick = onOpenSources,
            )
        } else {
            // Up to three wrapped rows of source buttons; overflow scrolls sideways.
            val rows = minOf(sources.size, SOURCE_GRID_MAX_ROWS)
            LazyHorizontalGrid(
                rows = GridCells.Fixed(rows),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height((rows * SOURCE_GRID_ROW_HEIGHT + (rows - 1) * 6).dp)
                    .padding(vertical = 4.dp),
            ) {
                gridItems(sources, key = { it.id }) { source ->
                    OutlinedButton(onClick = { onSourceClick(source) }) {
                        ColorDot(swatchColor(source.color, dark, fallbackSeed = source.id))
                        Spacer(Modifier.size(6.dp))
                        Text(source.name, maxLines = 1)
                    }
                }
            }
        }
    }
}

private const val SOURCE_GRID_MAX_ROWS = 3
private const val SOURCE_GRID_ROW_HEIGHT = 48

@Composable
private fun EmptyStateAction(message: String, button: String, onClick: () -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Button(onClick = onClick, modifier = Modifier.padding(top = 4.dp)) { Text(button) }
    }
}

@Composable
private fun TableHeader(title: String, size: Int, options: List<Int>, onSize: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        TextButton(onClick = { open = true }) {
            Text(stringResource(R.string.track_rows_per_page, size))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toString()) },
                    onClick = {
                        onSize(option)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun HistoryPager(page: Int, pageCount: Int, onPage: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
        IconButton(onClick = { onPage(page - 1) }, enabled = page > 0) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cd_prev_page),
            )
        }
        Text(
            stringResource(R.string.track_page_indicator, page + 1, pageCount),
            style = MaterialTheme.typography.bodyMedium,
        )
        IconButton(onClick = { onPage(page + 1) }, enabled = page < pageCount - 1) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cd_next_page),
            )
        }
    }
}

/** Search box slimmer than the stock text field -- the row packs three action buttons beside it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyMedium
            .copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = true,
        interactionSource = interactionSource,
        modifier = modifier.height(SEARCH_FIELD_HEIGHT.dp),
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            placeholder = {
                Text(hint, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                )
            },
        )
    }
}

private const val SEARCH_FIELD_HEIGHT = 44
