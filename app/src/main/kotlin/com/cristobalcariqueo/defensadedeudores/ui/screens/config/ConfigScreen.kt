package com.cristobalcariqueo.defensadedeudores.ui.screens.config

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.cristobalcariqueo.defensadedeudores.R
import com.cristobalcariqueo.defensadedeudores.data.sync.SyncStatus
import org.koin.androidx.compose.koinViewModel

private val FONT_OPTIONS = listOf("default", "serif", "sans", "mono")
private val LANGUAGE_OPTIONS = listOf("es-CL", "en-US")
private val THEME_OPTIONS = listOf("system", "dark", "light")
private val RETURN_BG_OPTIONS = listOf(
    "#FFF3CD", "#FFE0E0", "#DDEBFF", "#DFF5DF", "#F3E0FF", "#FFE2C4",
)
private val RECENT_OPTIONS = listOf(20, 30, 40, 50, 60, 70)
private val HISTORY_OPTIONS = listOf(50, 75, 100, 125, 150)

/**
 * Font, language, dark theme, return-reg bg color, table page sizes (SCOPE.md
 * screen 5) + the optional account/sync section: the app is fully usable
 * signed out; signing in starts continuous sync.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    onBack: () -> Unit,
    onOpenConflicts: () -> Unit,
    viewModel: ConfigViewModel = koinViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val current = settings ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_config_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChipSection(
                title = stringResource(R.string.config_theme),
                options = THEME_OPTIONS,
                selected = current.theme,
                label = { themeLabel(it) },
                onSelect = viewModel::setTheme,
            )

            ChipSection(
                title = stringResource(R.string.config_language),
                options = LANGUAGE_OPTIONS,
                selected = current.language,
                label = { it },
                onSelect = viewModel::setLanguage,
            )

            ChipSection(
                title = stringResource(R.string.config_font),
                options = FONT_OPTIONS,
                selected = current.font,
                label = { fontLabel(it) },
                onSelect = viewModel::setFont,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.config_return_bg),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RETURN_BG_OPTIONS.forEach { hex ->
                        val selected = hex.equals(current.returnBgColor, ignoreCase = true)
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(hex.toColorInt()), CircleShape)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                )
                                .clickable { viewModel.setReturnBgColor(hex) },
                        )
                    }
                }
            }

            ChipSection(
                title = stringResource(R.string.config_recent_size),
                options = RECENT_OPTIONS,
                selected = current.recentTableSize,
                label = { it.toString() },
                onSelect = viewModel::setRecentTableSize,
            )

            ChipSection(
                title = stringResource(R.string.config_history_size),
                options = HISTORY_OPTIONS,
                selected = current.historicalTableSize,
                label = { it.toString() },
                onSelect = viewModel::setHistoricalTableSize,
            )

            HorizontalDivider()
            AccountSection(viewModel = viewModel, onOpenConflicts = onOpenConflicts)
        }
    }
}

@Composable
private fun AccountSection(viewModel: ConfigViewModel, onOpenConflicts: () -> Unit) {
    val user by viewModel.user.collectAsState()
    val busy by viewModel.authBusy.collectAsState()
    val error by viewModel.authError.collectAsState()
    val awaitingConfirmation by viewModel.awaitingEmailConfirmation.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val conflictCount by viewModel.conflictCount.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.config_account),
            style = MaterialTheme.typography.titleMedium,
        )

        val signedIn = user
        if (signedIn == null) {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.auth_email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (awaitingConfirmation) {
                Text(
                    stringResource(R.string.auth_check_email),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.signIn(email, password) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.auth_sign_in)) }
                OutlinedButton(
                    onClick = { viewModel.signUp(email, password) },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.auth_sign_up)) }
            }
        } else {
            Text(
                stringResource(R.string.auth_signed_in_as, signedIn.email ?: signedIn.id),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                syncStatusLabel(syncStatus),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::syncNow, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.sync_now))
                }
                OutlinedButton(onClick = viewModel::signOut, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.auth_sign_out))
                }
            }
            if (conflictCount > 0) {
                Button(onClick = onOpenConflicts, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.sync_conflicts_button, conflictCount))
                }
            }
        }
    }
}

@Composable
private fun syncStatusLabel(status: SyncStatus): String = when (status) {
    SyncStatus.Idle -> stringResource(R.string.sync_status_idle)
    SyncStatus.Running -> stringResource(R.string.sync_status_running)
    is SyncStatus.Done -> stringResource(R.string.sync_status_done)
    is SyncStatus.Error -> stringResource(R.string.sync_status_error, status.message)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipSection(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        // Wrap whole chips to the next line; a chip's text never breaks mid-word.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option), maxLines = 1) },
                )
            }
        }
    }
}

@Composable
private fun fontLabel(key: String): String = when (key) {
    "serif" -> stringResource(R.string.config_font_serif)
    "sans" -> stringResource(R.string.config_font_sans)
    "mono" -> stringResource(R.string.config_font_mono)
    else -> stringResource(R.string.config_font_default)
}

@Composable
private fun themeLabel(key: String): String = when (key) {
    "dark" -> stringResource(R.string.theme_dark)
    "light" -> stringResource(R.string.theme_light)
    else -> stringResource(R.string.theme_system)
}
