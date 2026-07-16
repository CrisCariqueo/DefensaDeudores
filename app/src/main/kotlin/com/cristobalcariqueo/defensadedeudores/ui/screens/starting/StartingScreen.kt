package com.cristobalcariqueo.defensadedeudores.ui.screens.starting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.cristobalcariqueo.defensadedeudores.ui.components.NameEditDialog
import com.cristobalcariqueo.defensadedeudores.ui.theme.nextSwatchKey
import org.koin.androidx.compose.koinViewModel

/**
 * Onboarding for fresh accounts -- must create >= 1 person and >= 1 source
 * before continuing (SCOPE.md screen 0). Not shown again once satisfied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartingScreen(
    onDone: () -> Unit,
    viewModel: StartingViewModel = koinViewModel(),
) {
    val people by viewModel.people.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val canContinue by viewModel.canContinue.collectAsState()
    var addingPerson by remember { mutableStateOf(false) }
    var addingSource by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.screen_starting_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.starting_intro), style = MaterialTheme.typography.bodyLarge)

            SectionHeader(
                text = stringResource(R.string.screen_people_title),
                onAdd = { addingPerson = true },
            )
            people.forEach { Text("• ${it.name}", style = MaterialTheme.typography.bodyMedium) }

            SectionHeader(
                text = stringResource(R.string.screen_sources_title),
                onAdd = { addingSource = true },
            )
            sources.forEach { Text("• ${it.name}", style = MaterialTheme.typography.bodyMedium) }

            Button(
                onClick = { viewModel.completeOnboarding(onDone) },
                enabled = canContinue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_continue))
            }
        }
    }

    if (addingPerson) {
        NameEditDialog(
            title = stringResource(R.string.people_add_title),
            initialName = "",
            initialColor = nextSwatchKey(people.map { it.color }),
            onConfirm = { name, color ->
                viewModel.addPerson(name, color)
                addingPerson = false
            },
            onDismiss = { addingPerson = false },
        )
    }
    if (addingSource) {
        NameEditDialog(
            title = stringResource(R.string.sources_add_title),
            initialName = "",
            initialColor = nextSwatchKey(sources.map { it.color }),
            onConfirm = { name, color ->
                viewModel.addSource(name, color)
                addingSource = false
            },
            onDismiss = { addingSource = false },
        )
    }
}

@Composable
private fun SectionHeader(text: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
        }
    }
}
