package com.cristobalcariqueo.defensadedeudores.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.cristobalcariqueo.defensadedeudores.R
import com.cristobalcariqueo.defensadedeudores.domain.model.GraphSlice
import com.cristobalcariqueo.defensadedeudores.ui.format.formatClp

// Validated categorical palettes (dataviz skill, reference instance): fixed
// slot order is the CVD-safety mechanism -- assign by list position, never cycle.
private val LightPalette = listOf(
    Color(0xFF2A78D6), Color(0xFF1BAF7A), Color(0xFFEDA100), Color(0xFF008300),
    Color(0xFF4A3AA7), Color(0xFFE34948), Color(0xFFE87BA4), Color(0xFFEB6834),
)
private val DarkPalette = listOf(
    Color(0xFF3987E5), Color(0xFF199E70), Color(0xFFC98500), Color(0xFF008300),
    Color(0xFF9085E9), Color(0xFFE66767), Color(0xFFD55181), Color(0xFFD95926),
)
private const val MAX_SLICES = 8
private const val GAP_DEGREES = 2f

/**
 * Donut of outstanding debt shares + legend (name and amount in text tokens --
 * identity never rides on color alone). Slices beyond [MAX_SLICES] - 1 fold
 * into "Other".
 */
@Composable
fun DonutChart(
    title: String,
    slices: List<GraphSlice>,
    modifier: Modifier = Modifier,
) {
    val palette = if (isSystemInDarkTheme()) DarkPalette else LightPalette
    val otherColor = MaterialTheme.colorScheme.outline
    val otherLabel = stringResource(R.string.track_graph_other)

    val display = if (slices.size > MAX_SLICES) {
        slices.take(MAX_SLICES - 1) + GraphSlice(
            id = "other",
            label = otherLabel,
            total = slices.drop(MAX_SLICES - 1).sumOf { it.total },
        )
    } else {
        slices
    }
    val total = display.sumOf { it.total }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleSmall)

        if (total <= 0L) {
            Text(
                stringResource(R.string.track_graph_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
            return@Column
        }

        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
            Canvas(Modifier.size(140.dp)) {
                val stroke = Stroke(width = 22.dp.toPx())
                val inset = stroke.width / 2
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                val topLeft = Offset(inset, inset)
                // Gaps between slices (surface shows through); single slice needs none.
                val gap = if (display.size > 1) GAP_DEGREES else 0f
                val available = 360f - gap * display.size
                var startAngle = -90f + gap / 2
                display.forEachIndexed { index, slice ->
                    val sweep = (slice.total.toFloat() / total) * available
                    val color = if (slice.id == "other") otherColor else palette[index % palette.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = stroke,
                    )
                    startAngle += sweep + gap
                }
            }
            Text(formatClp(total), style = MaterialTheme.typography.titleSmall)
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
            display.forEachIndexed { index, slice ->
                val color = if (slice.id == "other") otherColor else palette[index % palette.size]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(color, CircleShape))
                    Text(
                        slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 6.dp).weight(1f),
                        maxLines = 1,
                    )
                    Text(
                        formatClp(slice.total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
