package com.cristobalcariqueo.defensadedeudores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.cristobalcariqueo.defensadedeudores.R
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryType
import com.cristobalcariqueo.defensadedeudores.domain.model.RegistryWithNames
import com.cristobalcariqueo.defensadedeudores.ui.format.formatClp

/**
 * One registry row. Rendering rules from SCOPE.md: superseded regs strike
 * through; return regs show negative amounts on the configurable background;
 * checked regs get a check mark. Tapping the amount opens the edit flow (#9).
 */
@Composable
fun RegistryRow(
    row: RegistryWithNames,
    returnBgColor: String,
    onAmountClick: (RegistryWithNames) -> Unit,
) {
    val reg = row.registry
    val isReturn = reg.type == RegistryType.RETURN
    val isSuperseded = reg.type == RegistryType.SUPERSEDED
    val background = if (isReturn) parseHexColor(returnBgColor) else Color.Transparent
    val decoration = if (isSuperseded) TextDecoration.LineThrough else TextDecoration.None
    val textColor =
        if (isSuperseded) MaterialTheme.colorScheme.onSurfaceVariant
        else if (isReturn) Color.Black // configurable light bg -- keep readable in dark theme too
        else MaterialTheme.colorScheme.onSurface

    Column(
        Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                reg.date.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isReturn) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = decoration,
            )
            Text(
                row.personName,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textDecoration = decoration,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            if (reg.checked) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.reg_checked),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            Text(
                formatClp(if (isReturn) -reg.amount.toLong() else reg.amount.toLong()),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                textDecoration = decoration,
                modifier = Modifier.clickable { onAmountClick(row) },
            )
        }
        val detail = listOfNotNull(row.sourceName, reg.note?.takeIf { it.isNotBlank() })
            .joinToString(" · ")
        if (detail.isNotEmpty()) {
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (isReturn) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = decoration,
                maxLines = 1,
            )
        }
    }
    HorizontalDivider(thickness = 0.5.dp)
}

private const val DEFAULT_RETURN_BG = 0xFFFFF3CD

private fun parseHexColor(hex: String): Color = runCatching {
    Color(hex.toColorInt())
}.getOrDefault(Color(DEFAULT_RETURN_BG))
