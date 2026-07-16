package com.cristobalcariqueo.defensadedeudores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.cristobalcariqueo.defensadedeudores.ui.theme.EntitySwatches

/** Fixed-palette color picker: one tap per swatch, check mark on the selection. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwatchRow(
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EntitySwatches.forEach { swatch ->
            val color = if (dark) swatch.dark else swatch.light
            val selected = swatch.key == selectedKey
            SwatchDot(color = color, selected = selected, onClick = { onSelect(swatch.key) })
        }
    }
}

@Composable
private fun SwatchDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    val ring = MaterialTheme.colorScheme.onSurface
    Icon(
        Icons.Default.Check,
        contentDescription = null,
        tint = if (selected) Color.White else Color.Transparent,
        modifier = Modifier
            .size(32.dp)
            .background(color, CircleShape)
            .then(if (selected) Modifier.border(2.dp, ring, CircleShape) else Modifier)
            .selectable(selected = selected, onClick = onClick),
    )
}

/** Small identity dot used beside entity names in lists, chips and dialogs. */
@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier, size: Int = 10) {
    androidx.compose.foundation.layout.Box(
        modifier.size(size.dp).background(color, CircleShape),
    )
}
