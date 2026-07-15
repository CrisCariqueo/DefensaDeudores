package com.cristobalcariqueo.defensadedeudores.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cristobalcariqueo.defensadedeudores.R
import com.cristobalcariqueo.defensadedeudores.ui.theme.swatchColor

/** One list row: stable id + display name + swatch key. */
data class NamedItem(val id: String, val name: String, val color: String? = null)

/**
 * Shared list-with-CRUD screen for Debtors and Sources -- both are plain
 * id+name+color entities with identical flows (SCOPE.md screens 3 and 4).
 * [suggestedColorKey] preselects the least-used swatch when creating.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameCrudScreen(
    title: String,
    addDialogTitle: String,
    items: List<NamedItem>,
    emptyMessage: String,
    suggestedColorKey: String,
    onAdd: (name: String, color: String?) -> Unit,
    onRename: (id: String, name: String, color: String?) -> Unit,
    onDelete: (id: String) -> Unit,
    onBack: (() -> Unit)?,
) {
    var editing by remember { mutableStateOf<NamedItem?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<NamedItem?>(null) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { adding = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_add))
            }
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(emptyMessage, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                items(items, key = { it.id }) { item ->
                    ListItem(
                        leadingContent = {
                            ColorDot(
                                color = swatchColor(item.color, dark, fallbackSeed = item.id),
                                size = 16,
                            )
                        },
                        headlineContent = { Text(item.name, maxLines = 1) },
                        trailingContent = {
                            IconButton(onClick = { deleting = item }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.action_delete),
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editing = item },
                    )
                }
            }
        }
    }

    if (adding) {
        NameEditDialog(
            title = addDialogTitle,
            initialName = "",
            initialColor = suggestedColorKey,
            onConfirm = { name, color ->
                onAdd(name, color)
                adding = false
            },
            onDismiss = { adding = false },
        )
    }

    editing?.let { item ->
        NameEditDialog(
            title = stringResource(R.string.dialog_edit_title),
            initialName = item.name,
            initialColor = item.color,
            onConfirm = { name, color ->
                onRename(item.id, name, color)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message, item.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(item.id)
                        deleting = null
                    },
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/**
 * Add/rename dialog shared by [NameCrudScreen] and the Starting screen. The
 * name field autofocuses so the keyboard opens with the dialog. Pass
 * [initialColor] to show the swatch picker (null hides it).
 */
@Composable
fun NameEditDialog(
    title: String,
    initialName: String,
    onConfirm: (name: String, color: String?) -> Unit,
    onDismiss: () -> Unit,
    initialColor: String? = null,
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    singleLine = true,
                    modifier = Modifier.focusRequester(focusRequester),
                )
                if (initialColor != null) {
                    SwatchRow(selectedKey = color, onSelect = { color = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, color) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
