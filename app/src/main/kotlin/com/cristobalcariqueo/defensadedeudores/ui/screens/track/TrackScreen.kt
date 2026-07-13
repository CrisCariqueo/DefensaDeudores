package com.cristobalcariqueo.defensadedeudores.ui.screens.track

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Graphs, quick-create, tables, search/filter, return-search + retReg
 * matching, edit/supersede flow. TODO(#7, #8, #9) -- see SCOPE.md.
 */
@Composable
fun TrackScreen(trackId: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Track screen ($trackId) -- TODO(#7)")
    }
}
