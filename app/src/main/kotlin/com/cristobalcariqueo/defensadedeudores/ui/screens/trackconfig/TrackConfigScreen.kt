package com.cristobalcariqueo.defensadedeudores.ui.screens.trackconfig

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cristobalcariqueo.defensadedeudores.R
import com.cristobalcariqueo.defensadedeudores.ui.components.ColorDot
import com.cristobalcariqueo.defensadedeudores.ui.components.NameEditDialog
import com.cristobalcariqueo.defensadedeudores.ui.theme.swatchColor
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Per-track settings (v0.2.0): rename (pencil beside the name), delete, and
 * draft membership editing persisted by Apply. Warns when leaving with zero
 * quick-create debtors.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TrackConfigScreen(
    trackId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: TrackConfigViewModel = koinViewModel(parameters = { parametersOf(trackId) }),
) {
    val track by viewModel.track.collectAsState()
    val allPeople by viewModel.allPeople.collectAsState()
    val allSources by viewModel.allSources.collectAsState()
    val draftDebtors by viewModel.draftDebtors.collectAsState()
    val draftSources by viewModel.draftSources.collectAsState()
    val hasChanges by viewModel.hasChanges.collectAsState()
    val removalBlock by viewModel.removalBlock.collectAsState()

    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var leavingWithoutDebtors by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val requestExit = {
        if (draftDebtors.isEmpty()) leavingWithoutDebtors = true else onBack()
    }
    BackHandler { requestExit() }

    val inUseMsg = stringResource(R.string.track_config_in_use)
    LaunchedEffect(removalBlock) {
        if (removalBlock != null) {
            snackbar.showSnackbar(inUseMsg)
            viewModel.clearRemovalBlock()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.track_config_title)) },
                navigationIcon = {
                    IconButton(onClick = requestExit) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    track?.name.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { renaming = true }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.action_rename),
                    )
                }
            }

            MembershipSection(
                title = stringResource(R.string.track_config_debtors),
                hint = null,
                items = allPeople.map { Triple(it.id, it.name, it.color) },
                selected = draftDebtors,
                dark = dark,
                onToggle = viewModel::toggleDebtor,
                onClear = viewModel::clearDebtors,
            )

            MembershipSection(
                title = stringResource(R.string.track_config_sources),
                hint = stringResource(R.string.track_config_sources_hint),
                items = allSources.map { Triple(it.id, it.name, it.color) },
                selected = draftSources,
                dark = dark,
                onToggle = viewModel::toggleSource,
                onClear = viewModel::clearSources,
            )

            Button(
                onClick = viewModel::apply,
                enabled = hasChanges,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.filter_apply))
            }

            OutlinedButton(
                onClick = { deleting = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.track_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (renaming) {
        NameEditDialog(
            title = stringResource(R.string.action_rename),
            initialName = track?.name.orEmpty(),
            onConfirm = { name, _ ->
                viewModel.rename(name)
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
                TextButton(onClick = { viewModel.delete(onDeleted) }) {
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

    if (leavingWithoutDebtors) {
        AlertDialog(
            onDismissRequest = { leavingWithoutDebtors = false },
            title = { Text(stringResource(R.string.track_config_title)) },
            text = { Text(stringResource(R.string.track_config_no_debtors)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        leavingWithoutDebtors = false
                        onBack()
                    },
                ) { Text(stringResource(R.string.action_leave)) }
            },
            dismissButton = {
                TextButton(onClick = { leavingWithoutDebtors = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MembershipSection(
    title: String,
    hint: String?,
    items: List<Triple<String, String, String?>>,
    selected: Set<String>,
    dark: Boolean,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear, enabled = selected.isNotEmpty()) {
                Text(stringResource(R.string.filter_clear))
            }
        }
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { (id, name, color) ->
                FilterChip(
                    selected = id in selected,
                    onClick = { onToggle(id) },
                    leadingIcon = { ColorDot(swatchColor(color, dark, fallbackSeed = id)) },
                    label = { Text(name, maxLines = 1) },
                )
            }
        }
    }
}
