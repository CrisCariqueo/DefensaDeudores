package com.cristobalcariqueo.defensadedeudores.ui.screens.sources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.cristobalcariqueo.defensadedeudores.R
import com.cristobalcariqueo.defensadedeudores.ui.components.NameCrudScreen
import com.cristobalcariqueo.defensadedeudores.ui.components.NamedItem
import org.koin.androidx.compose.koinViewModel

/** Sources (tag/category) CRUD -- SCOPE.md screen 4. */
@Composable
fun SourcesScreen(
    onBack: () -> Unit,
    viewModel: SourcesViewModel = koinViewModel(),
) {
    val sources by viewModel.sources.collectAsState()

    NameCrudScreen(
        title = stringResource(R.string.screen_sources_title),
        addDialogTitle = stringResource(R.string.sources_add_title),
        items = sources.map { NamedItem(it.id, it.name) },
        emptyMessage = stringResource(R.string.sources_empty),
        onAdd = viewModel::add,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
        onBack = onBack,
    )
}
