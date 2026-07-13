package com.cristobalcariqueo.defensadedeudores.ui.screens.people

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.cristobalcariqueo.defensadedeudores.R
import com.cristobalcariqueo.defensadedeudores.ui.components.NameCrudScreen
import com.cristobalcariqueo.defensadedeudores.ui.components.NamedItem
import org.koin.androidx.compose.koinViewModel

/** People (debtor) CRUD -- SCOPE.md screen 3. */
@Composable
fun PeopleScreen(
    onBack: () -> Unit,
    viewModel: PeopleViewModel = koinViewModel(),
) {
    val people by viewModel.people.collectAsState()

    NameCrudScreen(
        title = stringResource(R.string.screen_people_title),
        addDialogTitle = stringResource(R.string.people_add_title),
        items = people.map { NamedItem(it.id, it.name) },
        emptyMessage = stringResource(R.string.people_empty),
        onAdd = viewModel::add,
        onRename = viewModel::rename,
        onDelete = viewModel::delete,
        onBack = onBack,
    )
}
