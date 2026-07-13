package com.cristobalcariqueo.defensadedeudores.ui.screens.track

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cristobalcariqueo.defensadedeudores.R
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import com.cristobalcariqueo.defensadedeudores.domain.model.Source
import com.cristobalcariqueo.defensadedeudores.ui.components.DonutChart
import com.cristobalcariqueo.defensadedeudores.ui.components.NameEditDialog
import com.cristobalcariqueo.defensadedeudores.ui.components.RegistryRow
import com.cristobalcariqueo.defensadedeudores.ui.format.formatClp
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
    viewModel: TrackViewModel = koinViewModel(parameters = { parametersOf(trackId) }),
) {
    val track by viewModel.track.collectAsState()
    val shortcuts by viewModel.shortcuts.collectAsState()
    val allPeople by viewModel.allPeople.collectAsState()
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

    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var filtersOpen by remember { mutableStateOf(false) }
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
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.track_menu),
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_rename)) },
                            onClick = {
                                menuOpen = false
                                renaming = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.track_delete)) },
                            onClick = {
                                menuOpen = false
                                deleting = true
                            },
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
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    OutlinedTextField(
                        value = filter.query,
                        onValueChange = viewModel::setSearchQuery,
                        label = { Text(stringResource(R.string.track_search_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { filtersOpen = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.track_filters),
                        )
                    }
                }
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
                    RegistryRow(row, returnBg) { /* edit flow -- task #9 */ }
                }
            }

            item {
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
                    RegistryRow(row, returnBg) { /* edit flow -- task #9 */ }
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

    if (renaming) {
        NameEditDialog(
            title = stringResource(R.string.action_rename),
            initialName = track?.name.orEmpty(),
            onConfirm = { name ->
                viewModel.renameTrack(name)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }

    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.track_delete)) },
            text = { Text(stringResource(R.string.track_delete_confirm, track?.name.orEmpty())) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTrack(onBack) }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
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

    if (filtersOpen) {
        FilterSheet(
            current = filter,
            people = allPeople,
            allSources = sources,
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
                    label = { Text(person.name) },
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
            ) {
                sources.forEach { source ->
                    OutlinedButton(onClick = { onSourceClick(source) }) { Text(source.name) }
                }
            }
        }
    }
}

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
