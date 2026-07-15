package com.cristobalcariqueo.defensadedeudores.ui.screens.trackconfig

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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

/** Per-track settings: rename/delete + debtor and related-source membership (v1.1). */
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
    val shortcutIds by viewModel.shortcutIds.collectAsState()
    val relatedSourceIds by viewModel.relatedSourceIds.collectAsState()
    val removalBlock by viewModel.removalBlock.collectAsState()

    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val inUseMsg = stringResource(R.string.track_config_in_use)
    val lastDebtorMsg = stringResource(R.string.track_config_last_debtor)
    LaunchedEffect(removalBlock) {
        val block = removalBlock ?: return@LaunchedEffect
        snackbar.showSnackbar(
            when (block) {
                RemovalBlock.IN_USE -> inUseMsg
                RemovalBlock.LAST_DEBTOR -> lastDebtorMsg
            },
        )
        viewModel.clearRemovalBlock()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.track_config_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            Text(track?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.track_config_debtors),
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    allPeople.forEach { person ->
                        FilterChip(
                            selected = person.id in shortcutIds,
                            onClick = { viewModel.toggleDebtor(person.id) },
                            leadingIcon = {
                                ColorDot(swatchColor(person.color, dark, fallbackSeed = person.id))
                            },
                            label = { Text(person.name, maxLines = 1) },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.track_config_sources),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    stringResource(R.string.track_config_sources_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    allSources.forEach { source ->
                        FilterChip(
                            selected = source.id in relatedSourceIds,
                            onClick = { viewModel.toggleSource(source.id) },
                            leadingIcon = {
                                ColorDot(swatchColor(source.color, dark, fallbackSeed = source.id))
                            },
                            label = { Text(source.name, maxLines = 1) },
                        )
                    }
                }
            }

            OutlinedButton(onClick = { renaming = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_rename))
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
}
