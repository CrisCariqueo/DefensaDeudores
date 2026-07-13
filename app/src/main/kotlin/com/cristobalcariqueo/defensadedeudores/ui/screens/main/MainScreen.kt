package com.cristobalcariqueo.defensadedeudores.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Track list + People/Sources/Config entry points + FAB to create a track. TODO(#6). */
@Composable
fun MainScreen(
    onOpenTrack: (trackId: String) -> Unit,
    onOpenPeople: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenConfig: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Main screen -- TODO(#6)")
    }
}
