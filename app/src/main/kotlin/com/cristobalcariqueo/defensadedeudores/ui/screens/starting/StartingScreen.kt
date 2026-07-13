package com.cristobalcariqueo.defensadedeudores.ui.screens.starting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Shown to new accounts only. Must create >=1 Person and >=1 Source before
 * [onDone] can fire -- gating logic + the create forms land with task #5.
 */
@Composable
fun StartingScreen(onDone: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Starting screen -- TODO(#5)")
    }
}
