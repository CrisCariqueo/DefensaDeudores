package com.cristobalcariqueo.defensadedeudores.ui.screens.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import org.koin.androidx.compose.koinViewModel

/** Track list + People/Sources/Config entry points + FAB to create a track (SCOPE.md screen 1). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenTrack: (trackId: String) -> Unit,
    onOpenPeople: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenConfig: () -> Unit,
    viewModel: MainViewModel = koinViewModel(),
) {
    val tracks by viewModel.tracks.collectAsState()
    val people by viewModel.people.collectAsState()
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_main_title)) },
                actions = {
                    IconButton(onClick = onOpenPeople) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = stringResource(R.string.screen_people_title),
                        )
                    }
                    IconButton(onClick = onOpenSources) {
                        Icon(
                            Icons.Outlined.AccountBalance,
                            contentDescription = stringResource(R.string.screen_sources_title),
                        )
                    }
                    IconButton(onClick = onOpenConfig) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.screen_config_title),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.track_new_title))
            }
        },
    ) { padding ->
        if (tracks.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.main_empty), style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                items(tracks, key = { it.id }) { track ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { onOpenTrack(track.id) },
                    ) {
                        Text(
                            track.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        CreateTrackDialog(
            people = people,
            onGoToPeople = {
                creating = false
                onOpenPeople()
            },
            onCreate = { name, personIds ->
                viewModel.createTrack(name, personIds) { track ->
                    creating = false
                    onOpenTrack(track.id)
                }
            },
            onDismiss = { creating = false },
        )
    }
}

/**
 * New-track dialog: name + >= 1 debtor shortcut (SCOPE.md: a track must always
 * have at least one). With no people to pick, shows the empty-state warning +
 * a button to the People screen instead of the form.
 */
@Composable
private fun CreateTrackDialog(
    people: List<Person>,
    onGoToPeople: () -> Unit,
    onCreate: (name: String, personIds: List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.track_new_title)) },
        text = {
            if (people.isEmpty()) {
                Text(stringResource(R.string.track_no_people_warning))
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.label_name)) },
                        singleLine = true,
                    )
                    Text(
                        stringResource(R.string.track_people_hint),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                    people.forEach { person ->
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (person.id in selected) selected - person.id
                                    else selected + person.id
                                },
                        ) {
                            Checkbox(
                                checked = person.id in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + person.id else selected - person.id
                                },
                            )
                            Text(person.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (people.isEmpty()) {
                TextButton(onClick = onGoToPeople) {
                    Text(stringResource(R.string.track_go_to_people))
                }
            } else {
                TextButton(
                    onClick = { onCreate(name, selected.toList()) },
                    enabled = name.isNotBlank() && selected.isNotEmpty(),
                ) { Text(stringResource(R.string.action_save)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
