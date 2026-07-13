package com.cristobalcariqueo.defensadedeudores.ui.screens.conflicts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.cristobalcariqueo.defensadedeudores.data.local.entity.SyncConflictEntity
import org.koin.androidx.compose.koinViewModel

/**
 * Sync conflict resolution (DATA_MODEL.md "Offline & sync"): per row, keep
 * remote or local wholesale, or open the field-by-field merge dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictsScreen(
    onBack: () -> Unit,
    viewModel: ConflictsViewModel = koinViewModel(),
) {
    val conflicts by viewModel.conflicts.collectAsState()
    var merging by remember { mutableStateOf<SyncConflictEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.conflicts_title)) },
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
    ) { padding ->
        if (conflicts.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.conflicts_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                items(conflicts, key = { it.id }) { conflict ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                stringResource(R.string.conflicts_row, conflict.tableName),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                conflict.rowId,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.resolveLocal(conflict.id) },
                                    modifier = Modifier.weight(1f),
                                ) { Text(stringResource(R.string.conflicts_keep_local)) }
                                OutlinedButton(
                                    onClick = { viewModel.resolveRemote(conflict.id) },
                                    modifier = Modifier.weight(1f),
                                ) { Text(stringResource(R.string.conflicts_keep_remote)) }
                                OutlinedButton(
                                    onClick = { merging = conflict },
                                    modifier = Modifier.weight(1f),
                                ) { Text(stringResource(R.string.conflicts_merge)) }
                            }
                        }
                    }
                }
            }
        }
    }

    merging?.let { conflict ->
        MergeDialog(
            fields = viewModel.differingFields(conflict),
            onConfirm = { keepLocal ->
                viewModel.resolveMerged(conflict, keepLocal)
                merging = null
            },
            onDismiss = { merging = null },
        )
    }
}

/** Per differing field: pick the local or remote value; remote is the default. */
@Composable
private fun MergeDialog(
    fields: List<ConflictField>,
    onConfirm: (keepLocalKeys: Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var keepLocal by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.conflicts_merge)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                fields.forEach { field ->
                    Column {
                        Text(field.key, style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = field.key in keepLocal,
                                onClick = { keepLocal = keepLocal + field.key },
                                label = {
                                    Text(
                                        stringResource(R.string.conflicts_local_value, field.localValue),
                                        maxLines = 1,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChip(
                                selected = field.key !in keepLocal,
                                onClick = { keepLocal = keepLocal - field.key },
                                label = {
                                    Text(
                                        stringResource(R.string.conflicts_remote_value, field.remoteValue),
                                        maxLines = 1,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(keepLocal) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
