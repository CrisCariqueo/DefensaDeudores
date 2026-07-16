package com.cristobalcariqueo.defensadedeudores.ui.screens.track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.cristobalcariqueo.defensadedeudores.R
import com.cristobalcariqueo.defensadedeudores.domain.model.MatchSuggestion
import com.cristobalcariqueo.defensadedeudores.domain.model.Person
import com.cristobalcariqueo.defensadedeudores.domain.model.Registry
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryFilter
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryType
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryWithNames
import com.cristobalcariqueo.defensadedeudores.domain.model.Source
import com.cristobalcariqueo.defensadedeudores.ui.format.formatClp
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

private const val NOTE_MAX_LENGTH = 140
private const val MILLIS_PER_DAY = 86_400_000L

/** Quick-create modal: amount (CLP, integer) + optional note, tied to the selected debtor. */
@Composable
fun QuickCreateDialog(
    debtorName: String,
    sourceName: String,
    onConfirm: (amount: Int, note: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val amount = amountText.toIntOrNull()
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_create_title, debtorName, sourceName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { new -> amountText = new.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.focusRequester(focusRequester),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= NOTE_MAX_LENGTH) note = it },
                    label = { Text(stringResource(R.string.note_label)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount?.let { onConfirm(it, note.ifBlank { null }) } },
                enabled = amount != null && amount > 0,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/** AND-combinable filters (SCOPE.md): person(s), source(s), type, date range, checked state. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    current: RegistryFilter,
    people: List<Person>,
    allSources: List<Source>,
    onApply: (RegistryFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(current) }
    var pickingFrom by remember { mutableStateOf(false) }
    var pickingTo by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.track_filters),
                    style = MaterialTheme.typography.titleMedium,
                )

            Text(stringResource(R.string.filter_people), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                people.forEach { person ->
                    FilterChip(
                        selected = person.id in draft.personIds,
                        onClick = {
                            draft = draft.copy(
                                personIds = draft.personIds.toggle(person.id),
                            )
                        },
                        label = { Text(person.name) },
                    )
                }
            }

            Text(stringResource(R.string.filter_sources), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                allSources.forEach { source ->
                    FilterChip(
                        selected = source.id in draft.sourceIds,
                        onClick = {
                            draft = draft.copy(
                                sourceIds = draft.sourceIds.toggle(source.id),
                            )
                        },
                        label = { Text(source.name) },
                    )
                }
            }

            Text(stringResource(R.string.filter_type), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RegistryType.entries.forEach { type ->
                    FilterChip(
                        selected = draft.type == type,
                        onClick = {
                            draft = draft.copy(type = if (draft.type == type) null else type)
                        },
                        label = { Text(typeLabel(type)) },
                    )
                }
            }

            Text(stringResource(R.string.filter_checked), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = draft.checked == null,
                    onClick = { draft = draft.copy(checked = null) },
                    label = { Text(stringResource(R.string.checked_all)) },
                )
                FilterChip(
                    selected = draft.checked == false,
                    onClick = { draft = draft.copy(checked = false) },
                    label = { Text(stringResource(R.string.checked_pending)) },
                )
                FilterChip(
                    selected = draft.checked == true,
                    onClick = { draft = draft.copy(checked = true) },
                    label = { Text(stringResource(R.string.checked_settled)) },
                )
            }

            Text(stringResource(R.string.filter_dates), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pickingFrom = true }, modifier = Modifier.weight(1f)) {
                    Text(
                        draft.dateFrom?.toString()
                            ?: stringResource(R.string.filter_date_from),
                    )
                }
                OutlinedButton(onClick = { pickingTo = true }, modifier = Modifier.weight(1f)) {
                    Text(
                        draft.dateTo?.toString()
                            ?: stringResource(R.string.filter_date_to),
                    )
                }
            }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.amountMin?.toString().orEmpty(),
                        onValueChange = { new ->
                            draft = draft.copy(amountMin = new.filter(Char::isDigit).toIntOrNull())
                        },
                        label = { Text(stringResource(R.string.filter_amount_min)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = draft.amountMax?.toString().orEmpty(),
                        onValueChange = { new ->
                            draft = draft.copy(amountMax = new.filter(Char::isDigit).toIntOrNull())
                        },
                        label = { Text(stringResource(R.string.filter_amount_max)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedButton(
                    onClick = { draft = RegistryFilter(query = draft.query) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.filter_clear)) }
            }

            // Actions stay visible while the filter list scrolls.
            HorizontalDivider()
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(onClick = { onApply(draft) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.filter_apply))
                }
            }
        }
    }

    if (pickingFrom) {
        FilterDatePicker(
            initial = draft.dateFrom,
            onPicked = { draft = draft.copy(dateFrom = it) },
            onDismiss = { pickingFrom = false },
        )
    }
    if (pickingTo) {
        FilterDatePicker(
            initial = draft.dateTo,
            onPicked = { draft = draft.copy(dateTo = it) },
            onDismiss = { pickingTo = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDatePicker(
    initial: LocalDate?,
    onPicked: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.let { it.toEpochDays() * MILLIS_PER_DAY },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onPicked(state.selectedDateMillis?.toUtcLocalDate())
                    onDismiss()
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onPicked(null)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.filter_clear)) }
        },
    ) {
        DatePicker(state = state)
    }
}

/**
 * Return-search modal (SCOPE.md): pick the debtor (detached from the
 * quick-create selection), enter an amount, then either settle an exact-sum
 * selection of unchecked normal regs (Path A) or persist a retReg for the
 * amount instead (Path B).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReturnSearchDialog(
    state: ReturnSearchState,
    onSelectDebtor: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onSettle: () -> Unit,
    onCreateReturn: () -> Unit,
    onDismiss: () -> Unit,
) {
    val amount = state.amount
    val debtorPicked = state.debtorId != null
    val exactMatch = debtorPicked && amount != null &&
        state.selected.isNotEmpty() && state.selectedSum == amount
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.return_search_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.return_pick_debtor),
                    style = MaterialTheme.typography.labelLarge,
                )
                if (state.debtors.isEmpty()) {
                    Text(
                        stringResource(R.string.return_no_candidates),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.debtors.forEach { debtor ->
                        FilterChip(
                            selected = debtor.id == state.debtorId,
                            onClick = { onSelectDebtor(debtor.id) },
                            label = { Text(debtor.name, maxLines = 1) },
                        )
                    }
                }
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.focusRequester(focusRequester),
                )
                if (amount != null && amount > 0) {
                    Text(
                        stringResource(
                            R.string.return_selected_sum,
                            formatClp(state.selectedSum),
                            formatClp(amount),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (state.eligible.isEmpty()) {
                            Text(
                                stringResource(R.string.return_no_candidates),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        val debtorNames = state.debtors.associate { it.id to it.name }
                        state.eligible.forEach { reg ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Checkbox(
                                    checked = reg.id in state.selected,
                                    onCheckedChange = { onToggle(reg.id) },
                                )
                                Column {
                                    Text(
                                        formatClp(reg.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        listOfNotNull(
                                            // Debtor shown while searching across all of them.
                                            debtorNames[reg.personId].takeIf { !debtorPicked },
                                            reg.date.toString(),
                                            reg.note,
                                        ).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        // All actions in one wrapping row -- fixed confirm/dismiss slots overlap
        // when the localized labels get long.
        confirmButton = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                TextButton(
                    onClick = onCreateReturn,
                    enabled = debtorPicked && amount != null && amount > 0,
                ) {
                    Text(stringResource(R.string.return_create))
                }
                TextButton(onClick = onSettle, enabled = exactMatch) {
                    Text(stringResource(R.string.return_settle))
                }
            }
        },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/**
 * Edit popup (task #9): same-day regs edit in place; older ones warn that a
 * correction row will be logged instead (supersede path).
 */
@Composable
fun EditAmountDialog(
    row: RegistryWithNames,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember { mutableStateOf(row.registry.amount.toString()) }
    val amount = amountText.toIntOrNull()
    val focusRequester = remember { FocusRequester() }
    val createdToday = row.registry.createdAt
        .toLocalDateTime(TimeZone.currentSystemDefault()).date ==
        Clock.System.todayIn(TimeZone.currentSystemDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_amount_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { new -> amountText = new.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.focusRequester(focusRequester),
                )
                if (!createdToday) {
                    Text(
                        stringResource(R.string.edit_supersede_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount?.let(onConfirm) },
                enabled = amount != null && amount > 0 && amount != row.registry.amount,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/** Suggest-and-confirm match dialog -- SCOPE.md: never silent, user can dismiss. */
@Composable
fun MatchSuggestionDialog(
    suggestion: MatchSuggestion,
    returnBgColor: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val message = when (suggestion) {
        is MatchSuggestion.NewRegCovered ->
            stringResource(
                R.string.match_covered_msg,
                formatClp(suggestion.newReg.amount),
            )
        is MatchSuggestion.ExistingCovered ->
            stringResource(
                R.string.match_covered_msg,
                formatClp(suggestion.normal.amount),
            )
        is MatchSuggestion.NewRegReduced ->
            stringResource(
                R.string.match_reduced_msg,
                formatClp(
                    (suggestion.newReg.amount - suggestion.retReg.amount).toLong(),
                ),
            )
        is MatchSuggestion.ExistingReduced ->
            stringResource(
                R.string.match_reduced_msg,
                formatClp(
                    (suggestion.normal.amount - suggestion.retReg.amount).toLong(),
                ),
            )
    }
    val retReg = when (suggestion) {
        is MatchSuggestion.NewRegCovered -> suggestion.retReg
        is MatchSuggestion.NewRegReduced -> suggestion.retReg
        is MatchSuggestion.ExistingCovered -> suggestion.retReg
        is MatchSuggestion.ExistingReduced -> suggestion.retReg
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.match_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(message)
                Text(
                    stringResource(R.string.match_suggested_row),
                    style = MaterialTheme.typography.labelLarge,
                )
                RetRegPreviewRow(retReg, returnBgColor)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.match_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.match_dismiss)) }
        },
    )
}

/** The suggested retReg rendered like its table row: date + negative amount on the return background. */
@Composable
private fun RetRegPreviewRow(retReg: Registry, returnBgColor: String) {
    val bg = runCatching { Color(returnBgColor.toColorInt()) }.getOrDefault(Color(0xFFFFF3CD))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            retReg.date.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.Black,
        )
        Text(
            retReg.note.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black,
            maxLines = 1,
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        Text(
            formatClp(-retReg.amount.toLong()),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
        )
    }
}

@Composable
private fun typeLabel(type: RegistryType): String = when (type) {
    RegistryType.NORMAL -> stringResource(R.string.type_normal)
    RegistryType.RETURN -> stringResource(R.string.type_return)
    RegistryType.SUPERSEDED -> stringResource(R.string.type_superseded)
}

private fun Set<String>.toggle(id: String): Set<String> =
    if (id in this) this - id else this + id

/** DatePicker millis are UTC-midnight-based; convert in UTC to avoid off-by-one days. */
private fun Long.toUtcLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

/**
 * Detailed create (v0.2.0): any active debtor (not just track shortcuts), any
 * offered source, amount, optional note, any date. Same forward-match scan as
 * quick-create afterwards.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailedCreateDialog(
    people: List<Person>,
    sources: List<Source>,
    onConfirm: (personId: String, sourceId: String, amount: Int, note: String?, date: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var personId by remember { mutableStateOf<String?>(null) }
    var sourceId by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    var pickingDate by remember { mutableStateOf(false) }
    val amount = amountText.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detailed_create_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.filter_people), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    people.forEach { person ->
                        FilterChip(
                            selected = person.id == personId,
                            onClick = { personId = person.id },
                            label = { Text(person.name, maxLines = 1) },
                        )
                    }
                }
                Text(stringResource(R.string.filter_sources), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sources.forEach { source ->
                        FilterChip(
                            selected = source.id == sourceId,
                            onClick = { sourceId = source.id },
                            label = { Text(source.name, maxLines = 1) },
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { new -> amountText = new.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= NOTE_MAX_LENGTH) note = it },
                    label = { Text(stringResource(R.string.note_label)) },
                    singleLine = true,
                )
                Text(stringResource(R.string.label_date), style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { pickingDate = true }) { Text(date.toString()) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(personId!!, sourceId!!, amount!!, note.ifBlank { null }, date)
                },
                enabled = personId != null && sourceId != null && amount != null && amount > 0,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    if (pickingDate) {
        FilterDatePicker(
            initial = date,
            onPicked = { picked -> picked?.let { date = it } },
            onDismiss = { pickingDate = false },
        )
    }
}

/**
 * Full edit via row long-press (v0.2.0). Amount keeps the supersede/rematch
 * rules; note, source and date update in place on the surviving row. Returns
 * are not editable here (openFullEdit guards to normal regs).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditRegistryDialog(
    row: RegistryWithNames,
    sources: List<Source>,
    onConfirm: (amount: Int, note: String?, sourceId: String?, date: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val reg = row.registry
    var amountText by remember { mutableStateOf(reg.amount.toString()) }
    var note by remember { mutableStateOf(reg.note.orEmpty()) }
    var sourceId by remember { mutableStateOf(reg.sourceId) }
    var date by remember { mutableStateOf(reg.date) }
    var pickingDate by remember { mutableStateOf(false) }
    val amount = amountText.toIntOrNull()
    val createdToday = reg.createdAt
        .toLocalDateTime(TimeZone.currentSystemDefault()).date ==
        Clock.System.todayIn(TimeZone.currentSystemDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_registry_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { new -> amountText = new.filter { it.isDigit() } },
                    label = { Text(stringResource(R.string.amount_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                if (!createdToday && amount != null && amount != reg.amount) {
                    Text(
                        stringResource(R.string.edit_supersede_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= NOTE_MAX_LENGTH) note = it },
                    label = { Text(stringResource(R.string.note_label)) },
                    singleLine = true,
                )
                Text(stringResource(R.string.filter_sources), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sources.forEach { source ->
                        FilterChip(
                            selected = source.id == sourceId,
                            onClick = { sourceId = source.id },
                            label = { Text(source.name, maxLines = 1) },
                        )
                    }
                }
                Text(stringResource(R.string.label_date), style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { pickingDate = true }) { Text(date.toString()) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount!!, note.ifBlank { null }, sourceId, date) },
                enabled = amount != null && amount > 0,
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    if (pickingDate) {
        FilterDatePicker(
            initial = date,
            onPicked = { picked -> picked?.let { date = it } },
            onDismiss = { pickingDate = false },
        )
    }
}
